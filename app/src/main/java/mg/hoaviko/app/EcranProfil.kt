package mg.hoaviko.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Choisit l'écran selon la présence du profil dans Firestore.
@Composable
fun EcranCompte(
    email: String,
    onDeconnexion: () -> Unit,
    profilViewModel: ProfilViewModel = viewModel()
) {
    val etat by profilViewModel.uiState.collectAsState()
    val profil = etat.profil

    when {
        etat.chargement -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        !etat.lectureReussie -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = etat.erreur ?: "Le profil est indisponible.",
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { profilViewModel.chargerProfil() }
                ) {
                    Text("Réessayer")
                }

                TextButton(onClick = onDeconnexion) {
                    Text("Se déconnecter")
                }
            }
        }

        profil == null -> {
            EcranCompleterProfil(
                etat = etat,
                onEnregistrer = { nom, prenom, date, cin ->
                    profilViewModel.enregistrerProfil(
                        nom = nom,
                        prenom = prenom,
                        dateNaissance = date,
                        cin = cin
                    )
                },
                onDeconnexion = onDeconnexion
            )
        }

        else -> {
            EcranEspacePersonnel(
                email = email,
                prenom = profil.prenom,
                onDeconnexion = onDeconnexion
            )
        }
    }
}

// Formulaire de profil avec sélection de la date par calendrier.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranCompleterProfil(
    etat: EtatProfil,
    onEnregistrer: (String, String, String, String) -> Unit,
    onDeconnexion: () -> Unit
) {
    var nom by rememberSaveable { mutableStateOf("") }
    var prenom by rememberSaveable { mutableStateOf("") }
    var dateNaissance by rememberSaveable { mutableStateOf("") }
    var cin by rememberSaveable { mutableStateOf("") }

    var calendrierVisible by remember { mutableStateOf(false) }

    val calendrier = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long
            ): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= Calendar.getInstance().get(Calendar.YEAR)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Compléter mon profil",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Renseignez vos informations pour préparer " +
                    "votre espace Hoaviko."
        )

        OutlinedTextField(
            value = nom,
            onValueChange = { nom = it },
            label = { Text("Nom") },
            enabled = !etat.enregistrement,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = prenom,
            onValueChange = { prenom = it },
            label = { Text("Prénom") },
            enabled = !etat.enregistrement,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Date de naissance")

        OutlinedButton(
            onClick = { calendrierVisible = true },
            enabled = !etat.enregistrement,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (dateNaissance.isBlank()) {
                    "Choisir une date"
                } else {
                    dateNaissance
                }
            )
        }

        OutlinedTextField(
            value = cin,
            onValueChange = { saisie ->
                cin = saisie.filter { it in '0'..'9' }
            },
            label = { Text("Numéro de CIN") },
            enabled = !etat.enregistrement,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        etat.erreur?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                onEnregistrer(nom, prenom, dateNaissance, cin)
            },
            enabled = !etat.enregistrement,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text(
                if (etat.enregistrement) {
                    "Enregistrement..."
                } else {
                    "Enregistrer mon profil"
                }
            )
        }

        TextButton(
            onClick = onDeconnexion,
            enabled = !etat.enregistrement
        ) {
            Text("Se déconnecter")
        }

        Text(
            text = "Projet pédagogique : utilisez une identité " +
                    "et un CIN fictifs.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (calendrierVisible) {
        DatePickerDialog(
            onDismissRequest = { calendrierVisible = false },
            confirmButton = {
                TextButton(
                    enabled = calendrier.selectedDateMillis != null,
                    onClick = {
                        calendrier.selectedDateMillis?.let { millis ->
                            val format = SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.FRANCE
                            ).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }

                            dateNaissance = format.format(Date(millis))
                        }

                        calendrierVisible = false
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { calendrierVisible = false }
                ) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(
                state = calendrier,
                showModeToggle = false
            )
        }
    }
}

