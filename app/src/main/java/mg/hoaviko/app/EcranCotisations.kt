package mg.hoaviko.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun EcranCotisations(
    email: String,
    prenom: String,
    dateNaissance: String,
    onDeconnexion: () -> Unit,
    cotisationViewModel: CotisationViewModel = viewModel()
) {
    var afficherDemande by rememberSaveable {
        mutableStateOf(false)
    }

    var afficherRetrait by rememberSaveable {
        mutableStateOf(false)
    }

    if (afficherDemande) {
        EcranDemandesRetraite(
            administrateur = false,
            onRetour = {
                afficherDemande = false
            }
        )
        return
    }

    if (afficherRetrait) {
        EcranRetrait(
            dateNaissance = dateNaissance,
            onRetour = {
                afficherRetrait = false
            }
        )
        return
    }
    val etat by cotisationViewModel.uiState.collectAsState()
    val formulaire = etat.formulaire

    val formatDate = remember {
        SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nom de l'application.
        item {
            Text(
                text = "Hoaviko",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Informations du compte connecté.
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (prenom.isBlank()) {
                        "Bienvenue !"
                    } else {
                        "Bienvenue, $prenom !"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (etat.chargement) {
            item {
                CircularProgressIndicator()
            }
        } else if (etat.erreurLecture != null) {
            item {
                Text(
                    text = etat.erreurLecture.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Total enregistré dans Room.
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Total des cotisations fictives"
                        )

                        Text(
                            text = "${formatMontant(etat.totalAr)} Ar",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "${etat.cotisations.size} " +
                                    "cotisation(s) enregistrée(s)"
                        )
                    }
                }
            }

            // Formulaire d'ajout.
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Ajouter une cotisation",
                            style = MaterialTheme.typography.titleLarge
                        )

                        OutlinedTextField(
                            value = formulaire.montant,
                            onValueChange = {
                                cotisationViewModel.modifierMontant(it)
                            },
                            label = {
                                Text("Montant en ariary")
                            },
                            enabled = !formulaire.enregistrement,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Moyen de versement",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Orange Money, Airtel Money et MVola.
                        MODES_VERSEMENT.forEach { mode ->
                            val selectionne =
                                formulaire.modeVersement == mode

                            OutlinedButton(
                                onClick = {
                                    cotisationViewModel
                                        .modifierModeVersement(mode)
                                },
                                enabled = !formulaire.enregistrement,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectionne) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = if (selectionne) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            ) {
                                Text(
                                    text = if (selectionne) {
                                        "✓ $mode"
                                    } else {
                                        mode
                                    }
                                )
                            }
                        }

                        // Erreur de saisie ou d'enregistrement.
                        formulaire.erreur?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Confirmation d'enregistrement.
                        formulaire.message?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                cotisationViewModel.ajouterCotisation()
                            },
                            enabled = !formulaire.enregistrement,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Text(
                                text = if (formulaire.enregistrement) {
                                    "Enregistrement..."
                                } else {
                                    "Ajouter — simulation"
                                }
                            )
                        }

                        Text(
                            text = "Aucun paiement réel n’est effectué. " +
                                    "Le moyen choisi est enregistré " +
                                    "uniquement pour la simulation.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Historique.
            item {
                Text(
                    text = "Historique",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (etat.cotisations.isEmpty()) {
                item {
                    Text("Aucune cotisation pour le moment.")
                }
            }

            items(
                items = etat.cotisations,
                key = { cotisation -> cotisation.id }
            ) { cotisation ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${formatMontant(cotisation.montantAr)} Ar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = formatDate.format(
                                Date(cotisation.dateVersement)
                            )
                        )

                        Text(
                            text = "Moyen : ${cotisation.modeVersement}"
                        )

                        Text(
                            text = "Cotisation simulée",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    afficherDemande = true
                },
                enabled = !formulaire.enregistrement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ma demande de retraite anticipée")
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    afficherRetrait = true
                },
                enabled = !formulaire.enregistrement,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retirer de l'argent")
            }
        }
        // Déconnexion.
        item {
            OutlinedButton(
                onClick = onDeconnexion,
                enabled = !formulaire.enregistrement,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text("Se déconnecter")
            }
        }
    }
}