package mg.hoaviko.app

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val VertHoaviko = Color(0xFF146B50)

// Démarrage de l'application.
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

// Un seul AuthViewModel est partagé entre les écrans.
@Composable
fun NavigationHoaviko(
    authViewModel: AuthViewModel = viewModel()
) {
    val session by authViewModel.session.collectAsState()

    // Recrée la navigation quand la session change.
    key(session?.uid) {
        val navController = rememberNavController()
        val utilisateur = session

        NavHost(
            navController = navController,
            startDestination = if (utilisateur != null) {
                "espace"
            } else {
                "bienvenue"
            }
        ) {
            if (utilisateur != null) {
                composable("espace") {
                    EcranCompte(
                        email = utilisateur.email,
                        onDeconnexion = {
                            authViewModel.deconnecter()
                        }
                    )
                }
            } else {
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
                        },
                        authViewModel = authViewModel
                    )
                }

                composable("inscription") {
                    EcranInscription(
                        onRetour = {
                            navController.popBackStack()
                        },
                        authViewModel = authViewModel
                    )
                }
            }
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

// Accueil public et simulateur sans Internet.
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

                Text("Disponible sans Internet et sans compte.")

                OutlinedTextField(
                    value = montantMensuel,
                    onValueChange = { saisie ->
                        montantMensuel = saisie
                            .filter { it in '0'..'9' }
                            .take(9)
                    },
                    label = { Text("Versement mensuel (Ar)") },
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
                    label = { Text("Durée en années") },
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

                    val unite = if (annees == 1L) "an" else "ans"

                    Text(
                        text = "${formatMontant(montant)} Ar par mois " +
                                "pendant $annees $unite"
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
            text = "Accédez à votre espace pour suivre vos cotisations."
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

// Champ de mot de passe commun aux deux formulaires.
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
        label = { Text(libelle) },
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(
                enabled = enabled,
                onClick = { visible = !visible }
            ) {
                Text(if (visible) "Masquer" else "Afficher")
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// Connexion réelle.
// NavigationHoaviko ouvre l'espace quand la session change.
@Composable
fun EcranConnexion(
    onRetour: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val etat by authViewModel.connexionState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var erreurLocale by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(
            onClick = onRetour,
            enabled = !etat.chargement
        ) {
            Text("Retour")
        }

        Text(
            text = "Se connecter",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text("Retrouvez votre espace Hoaviko.")

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                erreurLocale = null
                authViewModel.effacerErreurConnexion()
            },
            enabled = !etat.chargement,
            label = { Text("Adresse e-mail") },
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
                erreurLocale = null
                authViewModel.effacerErreurConnexion()
            },
            libelle = "Mot de passe",
            enabled = !etat.chargement
        )

        val messageErreur = erreurLocale ?: etat.erreur

        messageErreur?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = !etat.chargement,
            onClick = {
                erreurLocale = when {
                    !Patterns.EMAIL_ADDRESS
                        .matcher(email.trim()).matches() ->
                        "Renseigne une adresse e-mail valide."

                    motDePasse.isBlank() ->
                        "Renseigne ton mot de passe."

                    else -> null
                }

                if (erreurLocale == null) {
                    authViewModel.connecter(
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
                if (etat.chargement) {
                    "Connexion en cours..."
                } else {
                    "Se connecter"
                }
            )
        }
    }
}

// L'identité sera renseignée dans EcranCompleterProfil.
@Composable
fun EcranInscription(
    onRetour: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val etat by authViewModel.uiState.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var erreurLocale by remember { mutableStateOf<String?>(null) }

    val formulaireActif = !etat.chargement && !etat.compteCree

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(
            onClick = onRetour,
            enabled = !etat.chargement
        ) {
            Text("Retour")
        }

        Text(
            text = "Créer un compte",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Créez votre compte, puis complétez votre profil."
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                erreurLocale = null
            },
            enabled = formulaireActif,
            label = { Text("Adresse e-mail") },
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
                erreurLocale = null
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
                erreurLocale = null
            },
            libelle = "Confirmer le mot de passe",
            enabled = formulaireActif
        )

        val messageErreur = erreurLocale ?: etat.erreur

        messageErreur?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = formulaireActif,
            onClick = {
                erreurLocale = when {
                    !Patterns.EMAIL_ADDRESS
                        .matcher(email.trim()).matches() ->
                        "Renseigne une adresse e-mail valide."

                    motDePasse.length < 8 ->
                        "Le mot de passe doit contenir au moins 8 caractères."

                    motDePasse != confirmation ->
                        "Les mots de passe ne correspondent pas."

                    else -> null
                }

                if (erreurLocale == null) {
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
                if (etat.chargement) {
                    "Création en cours..."
                } else {
                    "Créer mon compte"
                }
            )
        }
    }
}

// Première version de l'espace personnel.
@Composable
fun EcranEspacePersonnel(
    email: String,
    onDeconnexion: () -> Unit,
    prenom: String = ""
) {
    EcranCotisations(
        email = email,
        prenom = prenom,
        onDeconnexion = onDeconnexion
    )
}

// Utilisée par ProfilViewModel pour vérifier la date.
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