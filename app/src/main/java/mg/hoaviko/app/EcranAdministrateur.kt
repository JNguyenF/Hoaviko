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

// Oriente le compte selon le rôle vérifié dans Firebase.
@Composable
fun EcranOrientationCompte(
    email: String,
    onDeconnexion: () -> Unit,
    roleViewModel: RoleViewModel = viewModel()
) {
    val etat by roleViewModel.uiState.collectAsState()

    when {
        etat.chargement -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(16.dp))

                Text("Vérification de votre accès...")

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDeconnexion) {
                    Text("Se déconnecter")
                }
            }
        }

        etat.erreur != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Vérification impossible",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = etat.erreur.orEmpty()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        roleViewModel.verifierRole()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Réessayer")
                }

                TextButton(
                    onClick = onDeconnexion,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Se déconnecter")
                }
            }
        }

        etat.administrateur -> {
            EcranAdministrateur(
                email = email,
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

// Espace réservé à l'administration.
@Composable
fun EcranAdministrateur(
    email: String,
    onDeconnexion: () -> Unit
) {
    var afficherUtilisateurs by rememberSaveable {
        mutableStateOf(false)
    }

    if (afficherUtilisateurs) {
        EcranUtilisateursAdmin(
            onRetour = {
                afficherUtilisateurs = false
            }
        )
        return
    }
    var afficherDemandes by rememberSaveable {
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

                Text(text = email)

                Text(
                    text = "Gérez les demandes de retraite anticipée."
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Retraite anticipée",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Consultez les dossiers et acceptez " +
                            "ou refusez les demandes avec un commentaire."
                )

                Button(
                    onClick = {
                        afficherDemandes = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Text("Consulter les demandes")
                }
            }
        }
        Button(
            onClick = {
                afficherUtilisateurs = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Consulter les utilisateurs")
        }

        OutlinedButton(
            onClick = onDeconnexion,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            Text("Se déconnecter")
        }
    }
}