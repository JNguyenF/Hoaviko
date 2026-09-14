package mg.hoaviko.app

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val VertHoaviko = Color(0xFF146B50)

// Point d'entrée de l'application.
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = VertHoaviko,
                    background = Color(0xFFF5F8F6),
                    surface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationHoaviko()
                }
            }
        }
    }
}

// Navigation entre les écrans.
@Composable
fun NavigationHoaviko() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "bienvenue"
    ) {
        composable("bienvenue") {
            EcranBienvenue(
                onConnexion = {
                    navController.navigate("connexion") {
                        launchSingleTop = true
                    }
                },
                onInscription = {
                    navController.navigate("inscription") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("connexion") {
            EcranConnexion(
                onRetour = {
                    navController.popBackStack()
                }
            )
        }

        composable("inscription") {
            EcranInscription(
                onRetour = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// Exemple : 120000 devient "120 000".
fun formatMontant(valeur: Long): String {
    return valeur.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()
}

// Accueil public : fonctionne sans Internet.
@Composable
fun EcranBienvenue(
    onConnexion: () -> Unit,
    onInscription: () -> Unit
) {
    var montantMensuel by rememberSaveable {
        mutableStateOf("10000")
    }

    var dureeAnnees by rememberSaveable {
        mutableStateOf("1")
    }

    val montant = montantMensuel.toLongOrNull() ?: 0L
    val annees = dureeAnnees.toLongOrNull() ?: 0L
    val saisieValide = montant > 0L && annees > 0L

    // Versement mensuel × 12 mois × nombre d'années.
    val total = montant * 12L * annees

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
            text = "Hoaviko",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Préparez votre avenir à votre rythme",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Hoaviko accompagne les travailleurs indépendants " +
                    "dans la préparation de leur retraite : " +
                    "définissez un objectif d’épargne et suivez " +
                    "vos cotisations au fil des mois.",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Simulez votre épargne",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Disponible sans Internet et sans compte."
                )

                OutlinedTextField(
                    value = montantMensuel,
                    onValueChange = { saisie ->
                        montantMensuel = saisie
                            .filter { it in '0'..'9' }
                            .take(9)
                    },
                    label = {
                        Text("Versement mensuel (Ar)")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dureeAnnees,
                    onValueChange = { saisie ->
                        dureeAnnees = saisie
                            .filter { it in '0'..'9' }
                            .take(2)
                    },
                    label = {
                        Text("Durée en années")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (saisieValide) {
                    Text("Total des versements prévus")

                    Text(
                        text = "${formatMontant(total)} Ar",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val uniteAnnee = if (annees == 1L) "an" else "ans"

                    Text(
                        text = "${formatMontant(montant)} Ar par mois " +
                                "pendant $annees $uniteAnnee"
                    )
                } else {
                    Text(
                        text = "Saisissez un montant et une durée " +
                                "supérieurs à zéro.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Estimation sans intérêts ni frais, " +
                            "avec un versement identique chaque mois. " +
                            "Aucun argent n’est versé depuis ce simulateur.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            text = "Accédez à votre espace pour suivre vos cotisations.",
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = onConnexion,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Se connecter")
        }

        OutlinedButton(
            onClick = onInscription,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Créer un compte")
        }

        Text(
            text = "Prototype pédagogique • Paiements simulés",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Champ de mot de passe réutilisable.
@Composable
fun ChampMotDePasse(
    valeur: String,
    onValeurChange: (String) -> Unit,
    libelle: String,
    enabled: Boolean = true
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valeur,
        onValueChange = onValeurChange,
        enabled = enabled,
        label = {
            Text(libelle)
        },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(
                enabled = enabled,
                onClick = {
                    visible = !visible
                }
            ) {
                Text(
                    if (visible) "Masquer" else "Afficher"
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// Formulaire de connexion : branchement Firebase à venir.
@Composable
fun EcranConnexion(onRetour: () -> Unit) {
    var email by rememberSaveable {
        mutableStateOf("")
    }

    var motDePasse by remember {
        mutableStateOf("")
    }

    var erreur by remember {
        mutableStateOf<String?>(null)
    }

    var afficherInformation by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onRetour) {
            Text("Retour")
        }

        Text(
            text = "Se connecter",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Retrouvez votre espace Hoaviko.",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                erreur = null
            },
            label = {
                Text("Adresse e-mail")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ChampMotDePasse(
            valeur = motDePasse,
            onValeurChange = {
                motDePasse = it
                erreur = null
            },
            libelle = "Mot de passe"
        )

        erreur?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                erreur = when {
                    !Patterns.EMAIL_ADDRESS
                        .matcher(email.trim()).matches() ->
                        "Renseigne une adresse e-mail valide."

                    motDePasse.isBlank() ->
                        "Renseigne ton mot de passe."

                    else -> null
                }

                afficherInformation = erreur == null
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Se connecter")
        }
    }

    if (afficherInformation) {
        BoiteInformation(
            titre = "Connexion à brancher",
            message = "Le formulaire est correctement rempli. " +
                    "Nous relierons cet écran à Firebase " +
                    "à la prochaine étape.",
            onFermer = {
                afficherInformation = false
            }
        )
    }
}

// Inscription réelle avec Firebase Authentication.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranInscription(
    onRetour: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val etatAuth by authViewModel.uiState.collectAsState()

    var nom by rememberSaveable {
        mutableStateOf("")
    }

    var prenom by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var dateNaissance by rememberSaveable {
        mutableStateOf("")
    }

    var cin by rememberSaveable {
        mutableStateOf("")
    }

    // Les mots de passe ne sont pas sauvegardés sur le téléphone.
    var motDePasse by remember {
        mutableStateOf("")
    }

    var confirmation by remember {
        mutableStateOf("")
    }

    var erreur by remember {
        mutableStateOf<String?>(null)
    }

    var afficherCalendrier by remember {
        mutableStateOf(false)
    }

    val formulaireActif =
        !etatAuth.chargement && !etatAuth.compteCree

    val calendrier = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long
            ): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= Calendar.getInstance()
                    .get(Calendar.YEAR)
            }
        }
    )

    // Efface les mots de passe dès que le compte est créé.
    LaunchedEffect(etatAuth.compteCree) {
        if (etatAuth.compteCree) {
            motDePasse = ""
            confirmation = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = onRetour,
            enabled = !etatAuth.chargement
        ) {
            Text("Retour")
        }

        Text(
            text = "Créer un compte",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Bienvenue sur Hoaviko",
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            value = nom,
            onValueChange = {
                nom = it
                erreur = null
            },
            enabled = formulaireActif,
            label = {
                Text("Nom")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = prenom,
            onValueChange = {
                prenom = it
                erreur = null
            },
            enabled = formulaireActif,
            label = {
                Text("Prénom")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                erreur = null
            },
            enabled = formulaireActif,
            label = {
                Text("Adresse e-mail")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dateNaissance,
                onValueChange = {},
                enabled = formulaireActif,
                label = {
                    Text("Date de naissance")
                },
                placeholder = {
                    Text("JJ/MM/AAAA")
                },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        enabled = formulaireActif,
                        onClickLabel = "Choisir la date de naissance"
                    ) {
                        afficherCalendrier = true
                    }
            )
        }

        OutlinedTextField(
            value = cin,
            onValueChange = { saisie ->
                cin = saisie.filter { it in '0'..'9' }
                erreur = null
            },
            enabled = formulaireActif,
            label = {
                Text("Numéro de CIN")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ChampMotDePasse(
            valeur = motDePasse,
            onValeurChange = {
                motDePasse = it
                erreur = null
            },
            libelle = "Mot de passe",
            enabled = formulaireActif
        )

        Text(
            text = "Au moins 8 caractères",
            style = MaterialTheme.typography.bodySmall
        )

        ChampMotDePasse(
            valeur = confirmation,
            onValeurChange = {
                confirmation = it
                erreur = null
            },
            libelle = "Confirmer le mot de passe",
            enabled = formulaireActif
        )

        erreur?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        etatAuth.erreur?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = formulaireActif,
            onClick = {
                erreur = when {
                    nom.isBlank() || prenom.isBlank() ->
                        "Renseigne ton nom et ton prénom."

                    !Patterns.EMAIL_ADDRESS
                        .matcher(email.trim()).matches() ->
                        "Renseigne une adresse e-mail valide."

                    !dateNaissanceValide(dateNaissance) ->
                        "Choisis une date de naissance valide."

                    cin.isBlank() ->
                        "Renseigne ton numéro de CIN."

                    motDePasse.length < 8 ->
                        "Le mot de passe doit contenir " +
                                "au moins 8 caractères."

                    motDePasse != confirmation ->
                        "Les mots de passe ne correspondent pas."

                    else -> null
                }

                if (erreur == null) {
                    authViewModel.inscrire(
                        email = email,
                        motDePasse = motDePasse
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text(
                if (etatAuth.chargement) {
                    "Création en cours..."
                } else {
                    "Créer mon compte"
                }
            )
        }

        Text(
            text = "Pour les essais, utilise des informations " +
                    "d’identité fictives et une adresse e-mail que tu contrôles.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (afficherCalendrier) {
        DatePickerDialog(
            onDismissRequest = {
                afficherCalendrier = false
            },
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
                            erreur = null
                        }

                        afficherCalendrier = false
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        afficherCalendrier = false
                    }
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

    if (etatAuth.compteCree) {
        BoiteInformation(
            titre = "Compte créé",
            message = "Ton compte e-mail/mot de passe a été créé. " +
                    "L’enregistrement du nom, de la date de naissance " +
                    "et du CIN sera ajouté à la prochaine étape.",
            onFermer = onRetour
        )
    }
}

// Vérifie le format et la validité de la date.
fun dateNaissanceValide(valeur: String): Boolean {
    if (!valeur.matches(Regex("""\d{2}/\d{2}/\d{4}"""))) {
        return false
    }

    val format = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.FRANCE
    ).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val position = ParsePosition(0)
    val date = format.parse(valeur, position) ?: return false

    return position.index == valeur.length &&
            !date.after(Date())
}

// Boîte de dialogue réutilisable.
@Composable
fun BoiteInformation(
    titre: String,
    message: String,
    onFermer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = {
            Text(titre)
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onFermer) {
                Text("Compris")
            }
        }
    )
}