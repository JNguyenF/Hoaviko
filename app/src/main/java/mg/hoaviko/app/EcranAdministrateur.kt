package mg.hoaviko.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EcranOrientationCompte(
    email: String,
    onDeconnexion: () -> Unit,
    roleViewModel: RoleViewModel = viewModel()
) {
    val etat by roleViewModel.uiState.collectAsState()

    var espacePersonnel by rememberSaveable {
        mutableStateOf(false)
    }

    when {
        espacePersonnel -> {
            EcranCompte(
                email = email,
                onDeconnexion = onDeconnexion
            )
        }

        etat.chargement || etat.erreur != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (etat.chargement) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Vérification de votre accès...")
                } else {
                    Text(
                        text = etat.erreur.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            roleViewModel.verifierRole()
                        }
                    ) {
                        Text("Réessayer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Permet l'accès aux données personnelles déjà
                // disponibles hors ligne, sans accorder de rôle admin.
                OutlinedButton(
                    onClick = {
                        espacePersonnel = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ouvrir mon espace personnel")
                }

                Text(
                    text = "Sans Internet, votre profil doit avoir " +
                            "déjà été chargé sur ce téléphone.",
                    style = MaterialTheme.typography.bodySmall
                )

                TextButton(onClick = onDeconnexion) {
                    Text("Se déconnecter")
                }
            }
        }

        etat.administrateur -> {
            EcranAdministrateur(
                email = email,
                onEspacePersonnel = {
                    espacePersonnel = true
                },
                onDeconnexion = onDeconnexion
            )
        }

        else -> {
            EcranCompte(
                email = email,
                onDeconnexion = onDeconnexion
            )
        }
    }
}

@Composable
fun EcranAdministrateur
            (

    email: String,
    onEspacePersonnel: () -> Unit,
    onDeconnexion: () -> Unit
)
{var afficherDemandes by rememberSaveable {
    mutableStateOf(false)
}

    if (afficherDemandes) {
        EcranDemandesRetraite(
            administrateur = true,
            onRetour = {
                afficherDemandes = false
            }
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
            text = "Espace administrateur",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Compte administrateur",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(email)

                Text(
                    text = "Votre rôle a été vérifié auprès de Firebase."
                )
            }
        }

        Text(
            text = "Retraite anticipée",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            onClick = {
                afficherDemandes = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Consulter les demandes")
        }

        OutlinedButton(
            onClick = onEspacePersonnel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mon espace personnel")
        }

        OutlinedButton(
            onClick = onDeconnexion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Se déconnecter")
        }
    }
}

