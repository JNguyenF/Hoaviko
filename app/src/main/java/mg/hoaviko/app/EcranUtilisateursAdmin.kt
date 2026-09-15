package mg.hoaviko.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EcranUtilisateursAdmin(
    onRetour: () -> Unit,
    utilisateursViewModel: UtilisateursAdminViewModel = viewModel()
) {
    val etat by utilisateursViewModel.uiState.collectAsState()

    var recherche by rememberSaveable {
        mutableStateOf("")
    }

    var utilisateurSelectionneId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val utilisateurSelectionne = etat.utilisateurs.firstOrNull {
        it.uid == utilisateurSelectionneId
    }

    val revenir: () -> Unit = {
        if (utilisateurSelectionneId != null) {
            utilisateurSelectionneId = null
        } else {
            onRetour()
        }
    }

    BackHandler(onBack = revenir)

    val utilisateursFiltres = etat.utilisateurs.filter {
        val texte = recherche.trim()

        "${it.nom} ${it.prenom}".contains(
            texte,
            ignoreCase = true
        ) || it.uid.contains(texte, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(onClick = revenir) {
                Text("Retour")
            }

            Text(
                text = if (utilisateurSelectionne != null) {
                    "Détail du profil"
                } else {
                    "Utilisateurs"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when {
            etat.chargement -> {
                item {
                    CircularProgressIndicator()
                }
            }

            etat.erreur != null -> {
                item {
                    Text(
                        text = etat.erreur.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        onClick = {
                            utilisateursViewModel.chargerUtilisateurs()
                        }
                    ) {
                        Text("Réessayer")
                    }
                }
            }

            utilisateurSelectionne != null -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            InformationProfil(
                                titre = "Nom",
                                valeur = utilisateurSelectionne.nom
                            )

                            InformationProfil(
                                titre = "Prénom",
                                valeur = utilisateurSelectionne.prenom
                            )

                            InformationProfil(
                                titre = "Date de naissance",
                                valeur = utilisateurSelectionne.dateNaissance
                            )

                            InformationProfil(
                                titre = "Numéro de CIN",
                                valeur = utilisateurSelectionne.cin
                            )

                            InformationProfil(
                                titre = "Identifiant Firebase",
                                valeur = utilisateurSelectionne.uid
                            )
                        }
                    }
                }
            }

            else -> {
                item {
                    OutlinedTextField(
                        value = recherche,
                        onValueChange = { recherche = it },
                        label = {
                            Text("Rechercher un nom ou un identifiant")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "${utilisateursFiltres.size} profil(s)"
                    )

                    OutlinedButton(
                        onClick = {
                            utilisateursViewModel.chargerUtilisateurs()
                        }
                    ) {
                        Text("Actualiser")
                    }
                }

                if (utilisateursFiltres.isEmpty()) {
                    item {
                        Text("Aucun profil trouvé.")
                    }
                }

                items(
                    items = utilisateursFiltres,
                    key = { it.uid }
                ) { utilisateur ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${utilisateur.nom} ${utilisateur.prenom}"
                                    .trim()
                                    .ifBlank { "Profil sans nom" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "ID : ${utilisateur.uid}",
                                style = MaterialTheme.typography.bodySmall
                            )

                            TextButton(
                                onClick = {
                                    utilisateurSelectionneId = utilisateur.uid
                                }
                            ) {
                                Text("Voir le profil")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InformationProfil(
    titre: String,
    valeur: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = titre,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = valeur.ifBlank { "Non renseigné" }
        )
    }
}

