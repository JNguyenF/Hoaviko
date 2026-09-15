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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// dateNaissance attendue au format dd/MM/yyyy (voir EcranProfil.kt),
// à transmettre depuis le profil de l'utilisateur connecté.
@Composable
fun EcranRetrait(
    dateNaissance: String,
    onRetour: () -> Unit,
    cotisationViewModel: CotisationViewModel = viewModel(),
    demandeViewModel: DemandeRetraiteViewModel = viewModel(
        key = "ma-demande-retraite"
    ),
    retraitViewModel: RetraitViewModel = viewModel()
) {
    val etatCotisation by cotisationViewModel.uiState.collectAsState()
    val etatDemande by demandeViewModel.uiState.collectAsState()
    val etat by retraitViewModel.uiState.collectAsState()

    val demandeActuelle = etatDemande.demandes.firstOrNull()
    val retraiteAnticipeeValidee = demandeActuelle?.statut == "ACCEPTEE"

    LaunchedEffect(Unit) {
        demandeViewModel.charger(false)
    }

    LaunchedEffect(
        etatCotisation.totalAr,
        dateNaissance,
        retraiteAnticipeeValidee
    ) {
        retraitViewModel.mettreAJourContexte(
            totalCotiseAr = etatCotisation.totalAr,
            dateNaissance = dateNaissance,
            retraiteAnticipeeValidee = retraiteAnticipeeValidee
        )
    }

    val formatDate = remember {
        SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE)
    }

    val formulaire = etat.formulaire

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
                text = "Retrait de cotisations",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Solde disponible")

                    Text(
                        text = "${formatMontant(etat.soldeDisponibleAr)} Ar",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (retraiteAnticipeeValidee) {
                            "Retraite anticipée validée : " +
                                    "retraits sans pénalité."
                        } else {
                            "Retrait avant 60 ans soumis à une " +
                                    "pénalité de 10 %, sauf retraite " +
                                    "anticipée validée."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Demander un retrait",
                        style = MaterialTheme.typography.titleLarge
                    )

                    OutlinedTextField(
                        value = formulaire.montant,
                        onValueChange = {
                            retraitViewModel.modifierMontant(it)
                        },
                        label = { Text("Montant en ariary") },
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
                        val selectionne = formulaire.modeVersement == mode

                        OutlinedButton(
                            onClick = {
                                retraitViewModel.modifierModeVersement(mode)
                            },
                            enabled = !formulaire.enregistrement,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectionne) {
                                    MaterialTheme.colorScheme
                                        .primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (selectionne) {
                                    MaterialTheme.colorScheme
                                        .onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        ) {
                            Text(
                                text = if (selectionne) "✓ $mode" else mode
                            )
                        }
                    }

                    formulaire.erreur?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    formulaire.message?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { retraitViewModel.demanderRetrait() },
                        enabled = !formulaire.enregistrement,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) {
                        Text(
                            text = if (formulaire.enregistrement) {
                                "Enregistrement..."
                            } else {
                                "Demander le retrait"
                            }
                        )
                    }

                    Text(
                        text = "Aucun paiement réel n'est effectué. " +
                                "Le retrait est simulé pour ce projet " +
                                "pédagogique.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text(
                text = "Historique des retraits",
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (etat.retraits.isEmpty()) {
            item {
                Text("Aucun retrait pour le moment.")
            }
        }

        items(
            items = etat.retraits,
            key = { retrait -> retrait.id }
        ) { retrait ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${formatMontant(retrait.montantNetAr)} " +
                                "Ar reçus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Montant demandé : " +
                                "${formatMontant(retrait.montantBrutAr)} Ar"
                    )

                    if (retrait.tauxPenalite > 0) {
                        Text(
                            text = "Pénalité appliquée : " +
                                    "${retrait.tauxPenalite} %",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("Sans pénalité")
                    }

                    Text(text = formatDate.format(Date(retrait.dateRetrait)))

                    Text(text = "Moyen : ${retrait.modeVersement}")
                }
            }
        }
    }

    // Boîte de confirmation avec le montant net avant validation finale.
    etat.demandeConfirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = { retraitViewModel.annulerConfirmation() },
            title = { Text("Confirmer le retrait") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Montant demandé : " +
                                "${formatMontant(confirmation.montantBrutAr)} Ar"
                    )

                    if (confirmation.tauxPenalite > 0) {
                        Text(
                            "Pénalité (${confirmation.tauxPenalite} %) : " +
                                    "-${formatMontant(confirmation.penaliteAr)} Ar"
                        )
                    } else {
                        Text("Aucune pénalité applicable.")
                    }

                    Text(
                        text = "Montant net reçu : " +
                                "${formatMontant(confirmation.montantNetAr)} Ar",
                        fontWeight = FontWeight.Bold
                    )

                    Text("Moyen de versement : ${confirmation.modeVersement}")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { retraitViewModel.confirmerRetrait() }
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { retraitViewModel.annulerConfirmation() }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

