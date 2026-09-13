package mg.hoaviko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private val VertHoaviko = Color(0xFF146B50)

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

@Composable
fun EcranBienvenue(
    onConnexion: () -> Unit,
    onInscription: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Hoaviko",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Votre avenir se prépare aujourd’hui",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Fixez votre objectif, suivez vos cotisations " +
                    "et préparez votre retraite à votre rythme.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onInscription,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Créer un compte")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onConnexion,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Se connecter")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(16.dp))
    }
}
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
                    navController.navigate("connexion")
                },
                onInscription = {
                    navController.navigate("inscription")
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

@Composable
fun EcranConnexion(onRetour: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connexion",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Accédez à votre espace Hoaviko.")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = onRetour) {
            Text("Retour")
        }
    }
}

@Composable
fun EcranInscription(onRetour: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Créer un compte",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Commencez votre parcours avec Hoaviko.")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = onRetour) {
            Text("Retour")
        }
    }
}