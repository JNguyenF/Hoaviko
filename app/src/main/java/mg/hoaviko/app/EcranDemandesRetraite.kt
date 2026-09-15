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
fun EcranDemandesRetraite(
    administrateur: Boolean,
    onRetour: () -> Unit,
    demandeViewModel: DemandeRetraiteViewModel = viewModel(
        key = if (administrateur) {
            "demandes-administrateur"
        } else {
            "ma-demande-retraite"
        }
    )
) {
    val etat by demandeViewModel.uiState.collectAsState()

    var motif by rememberSaveable { mutableStateOf("") }
    var justificatif by rememberSaveable { mutableStateOf("") }
    var nouvelleDemande by rememberSaveable {
        mutableStateOf(false)
    }

    val demandeActuelle = etat.demandes.firstOrNull()

    LaunchedEffect(demandeActuelle?.statut) {
        if (demandeActuelle?.statut != "REFUSEE") {
            nouvelleDemande = false
        }
    }

    BackHandler {
        onRetour()
    }

    LaunchedEffect(administrateur) {
        demandeViewModel.charger(administrateur)
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
            TextButton(onClick = onRetour) {
                Text("Retour")
            }

            Text(
                text = if (administrateur) {
                    "Demandes de retraite anticipée"
                } else {
                    "Ma demande de retraite anticipée"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            OutlinedButton(
                onClick = {
                    demandeViewModel.charger(administrateur)
                },
                enabled = !etat.chargement && !etat.traitement
            ) {
                Text("Actualiser")
            }
        }

        if (etat.chargement || etat.traitement) {
            item {
                CircularProgressIndicator()
            }
        }

        etat.erreur?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (
            !administrateur &&
            !etat.chargement &&
            demandeActuelle?.statut == "REFUSEE" &&
            !nouvelleDemande
        ) {
            item {
                Button(
                    onClick = {
                        motif = ""
                        justificatif = ""
                        nouvelleDemande = true
                    },
                    enabled = !etat.traitement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Déposer une nouvelle demande")
                }
            }
        }

        if (
            !etat.chargement &&
            !administrateur &&
            (
                    etat.demandes.isEmpty() ||
                            (
                                    demandeActuelle?.statut == "REFUSEE" &&
                                            nouvelleDemande
                                    )
                    )
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Expliquez votre demande. " +
                                "Un administrateur examinera les informations."
                    )

                    OutlinedTextField(
                        value = motif,
                        onValueChange = { motif = it.take(1000) },
                        label = { Text("Motif de la demande") },
                        minLines = 3,
                        enabled = !etat.traitement,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = justificatif,
                        onValueChange = {
                            justificatif = it.take(200)
                        },
                        label = {
                            Text("Référence du justificatif fictif")
                        },
                        placeholder = {
                            Text("Exemple : ATTESTATION-TEST-001")
                        },
                        enabled = !etat.traitement,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Version pédagogique : aucun document " +
                                "n’est téléversé à cette étape.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Button(
                        onClick = {
                            demandeViewModel.envoyer(
                                motif = motif,
                                justificatif = justificatif
                            )
                        },
                        enabled = !etat.traitement,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Envoyer ma demande")
                    }
                }
            }
        }

        if (
            administrateur &&
            !etat.chargement &&
            etat.erreur == null &&
            etat.demandes.isEmpty()
        ) {
            item {
                Text("Aucune demande reçue.")
            }
        }

        items(
            items = etat.demandes,
            key = { it.id }
        ) { demande ->
            CarteDemandeRetraite(
                demande = demande,
                administrateur = administrateur,
                traitement = etat.traitement,
                onDecision = { accepter, commentaire ->
                    demandeViewModel.decider(
                        demandeId = demande.id,
                        accepter = accepter,
                        commentaire = commentaire
                    )
                }
            )
        }
    }
}

@Composable
fun CarteDemandeRetraite(
    demande: DemandeRetraite,
    administrateur: Boolean,
    traitement: Boolean,
    onDecision: (Boolean, String) -> Unit
) {
    var commentaire by rememberSaveable(demande.id) {
        mutableStateOf("")
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val libelleStatut = when (demande.statut) {
                "EN_ATTENTE" -> "En attente"
                "ACCEPTEE" -> "Acceptée"
                "REFUSEE" -> "Refusée"
                else -> "Statut inconnu"
            }

            Text(
                text = libelleStatut,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (administrateur) {
                Text(
                    text = "Identifiant du demandeur : ${demande.id}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "Motif",
                fontWeight = FontWeight.Bold
            )
            Text(demande.motif)

            Text(
                text = "Justificatif fictif : ${demande.justificatif}"
            )

            if (demande.commentaire.isNotBlank()) {
                Text(
                    text = "Décision : ${demande.commentaire}"
                )
            }

            if (
                administrateur &&
                demande.statut == "EN_ATTENTE"
            ) {
                OutlinedTextField(
                    value = commentaire,
                    onValueChange = {
                        commentaire = it.take(1000)
                    },
                    label = { Text("Commentaire de décision") },
                    enabled = !traitement,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onDecision(true, commentaire)
                    },
                    enabled = !traitement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Accepter la demande")
                }

                OutlinedButton(
                    onClick = {
                        onDecision(false, commentaire)
                    },
                    enabled = !traitement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Refuser la demande")
                }
            }
        }
    }
}

