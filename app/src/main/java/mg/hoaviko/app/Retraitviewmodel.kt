package mg.hoaviko.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class EtatFormulaireRetrait(
    val montant: String = "",
    val modeVersement: String = "Orange Money",
    val enregistrement: Boolean = false,
    val erreur: String? = null,
    val message: String? = null
)

// Résumé affiché avant la validation définitive du retrait.
data class ConfirmationRetrait(
    val montantBrutAr: Long,
    val montantNetAr: Long,
    val penaliteAr: Long,
    val tauxPenalite: Int,
    val modeVersement: String
)

data class EtatRetraits(
    val chargement: Boolean = true,
    val retraits: List<Retrait> = emptyList(),
    val soldeDisponibleAr: Long = 0L,
    val formulaire: EtatFormulaireRetrait = EtatFormulaireRetrait(),
    val erreurLecture: String? = null,
    val demandeConfirmation: ConfirmationRetrait? = null
)

class RetraitViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val utilisateurId = requireNotNull(
        FirebaseAuth.getInstance().currentUser?.uid
    ) {
        "Une connexion est nécessaire pour accéder aux retraits."
    }

    private val repository = RetraitRepository(
        HoavikoDatabase.getInstance(application).retraitDao()
    )

    private val formulaire = MutableStateFlow(EtatFormulaireRetrait())
    private val confirmation =
        MutableStateFlow<ConfirmationRetrait?>(null)

    // Contexte fourni par l'écran : total cotisé (CotisationViewModel),
    // date de naissance (profil) et statut de la demande de retraite
    // anticipée (DemandeRetraiteViewModel). "ACCEPTEE" = validée.
    private val contexte = MutableStateFlow(
        Triple(0L, "", false)
    )

    val uiState: StateFlow<EtatRetraits> = combine(
        repository.observerRetraits(utilisateurId),
        repository.observerTotalRetire(utilisateurId),
        formulaire,
        confirmation,
        contexte
    ) { retraits, totalRetireAr, saisie, confirmationActuelle, ctx ->
        val (totalCotiseAr, _, _) = ctx

        EtatRetraits(
            chargement = false,
            retraits = retraits,
            soldeDisponibleAr =
                (totalCotiseAr - totalRetireAr).coerceAtLeast(0),
            formulaire = saisie,
            demandeConfirmation = confirmationActuelle
        )
    }.catch {
        emit(
            EtatRetraits(
                chargement = false,
                erreurLecture = "Impossible de lire les retraits. " +
                        "Quitte cet écran puis réessaie."
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EtatRetraits()
    )

    // À appeler depuis l'écran dès que le total cotisé, la date de
    // naissance ou le statut de retraite anticipée sont connus.
    fun mettreAJourContexte(
        totalCotiseAr: Long,
        dateNaissance: String,
        retraiteAnticipeeValidee: Boolean
    ) {
        contexte.value =
            Triple(totalCotiseAr, dateNaissance, retraiteAnticipeeValidee)
    }

    fun modifierMontant(saisie: String) {
        if (formulaire.value.enregistrement) return

        formulaire.update {
            it.copy(
                montant = saisie
                    .filter { caractere -> caractere in '0'..'9' }
                    .take(9),
                erreur = null,
                message = null
            )
        }
    }

    fun modifierModeVersement(mode: String) {
        if (formulaire.value.enregistrement) return
        if (mode !in MODES_VERSEMENT) return

        formulaire.update {
            it.copy(
                modeVersement = mode,
                erreur = null,
                message = null
            )
        }
    }

    // Âge en années révolues à partir d'une date au format dd/MM/yyyy.
    private fun calculerAge(dateNaissance: String): Int? {
        if (dateNaissance.isBlank()) return null

        val format = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        format.isLenient = false

        val naissance = try {
            format.parse(dateNaissance) ?: return null
        } catch (e: Exception) {
            return null
        }

        val calNaissance = Calendar.getInstance().apply { time = naissance }
        val calAujourdhui = Calendar.getInstance()

        var age = calAujourdhui.get(Calendar.YEAR) -
                calNaissance.get(Calendar.YEAR)

        if (
            calAujourdhui.get(Calendar.DAY_OF_YEAR) <
            calNaissance.get(Calendar.DAY_OF_YEAR)
        ) {
            age--
        }

        return age
    }

    // Règles Hoaviko :
    // - 60 ans ou plus            -> 0 % (dans tous les cas)
    // - moins de 60 ans + validée -> 0 %
    // - moins de 60 ans, sinon    -> 10 %
    // Une demande en attente ou refusée compte comme "non validée".
    private fun calculerTauxPenalite(
        dateNaissance: String,
        retraiteAnticipeeValidee: Boolean
    ): Int {
        val age = calculerAge(dateNaissance)

        if (age != null && age >= AGE_RETRAITE) {
            return 0
        }

        return if (retraiteAnticipeeValidee) {
            0
        } else {
            TAUX_PENALITE_ANTICIPEE
        }
    }

    // Étape 1 : calcule et affiche la confirmation (montant net, pénalité).
    fun demanderRetrait() {
        if (formulaire.value.enregistrement) return

        val (totalCotiseAr, dateNaissance, retraiteAnticipeeValidee) =
            contexte.value

        val soldeDisponibleAr = (
                totalCotiseAr - uiState.value.retraits.sumOf {
                    it.montantBrutAr
                }
                ).coerceAtLeast(0)

        val montantBrutAr = formulaire.value.montant.toLongOrNull()

        if (montantBrutAr == null || montantBrutAr <= 0L) {
            formulaire.update {
                it.copy(erreur = "Indique un montant valide.")
            }
            return
        }

        if (montantBrutAr > soldeDisponibleAr) {
            formulaire.update {
                it.copy(
                    erreur = "Le montant dépasse le solde disponible " +
                            "(${formatMontant(soldeDisponibleAr)} Ar)."
                )
            }
            return
        }

        val tauxPenalite = calculerTauxPenalite(
            dateNaissance,
            retraiteAnticipeeValidee
        )
        val penaliteAr = (montantBrutAr * tauxPenalite) / 100

        confirmation.value = ConfirmationRetrait(
            montantBrutAr = montantBrutAr,
            montantNetAr = montantBrutAr - penaliteAr,
            penaliteAr = penaliteAr,
            tauxPenalite = tauxPenalite,
            modeVersement = formulaire.value.modeVersement
        )
    }

    fun annulerConfirmation() {
        confirmation.value = null
    }

    // Étape 2 : enregistrement définitif après confirmation du montant net.
    fun confirmerRetrait() {
        if (formulaire.value.enregistrement) return

        val confirmationActuelle = confirmation.value ?: return

        // Vérifie que la session correspond toujours à ce ViewModel.
        if (FirebaseAuth.getInstance().currentUser?.uid != utilisateurId) {
            formulaire.update {
                it.copy(erreur = "La session a changé. Reconnecte-toi.")
            }
            confirmation.value = null
            return
        }

        formulaire.update { it.copy(enregistrement = true) }

        viewModelScope.launch {
            try {
                repository.retirer(
                    utilisateurId = utilisateurId,
                    montantBrutAr = confirmationActuelle.montantBrutAr,
                    tauxPenalite = confirmationActuelle.tauxPenalite,
                    modeVersement = confirmationActuelle.modeVersement
                )

                val message = if (confirmationActuelle.tauxPenalite > 0) {
                    "Retrait simulé : " +
                            "${formatMontant(confirmationActuelle.montantNetAr)} " +
                            "Ar reçus (pénalité de " +
                            "${confirmationActuelle.tauxPenalite} %)."
                } else {
                    "Retrait simulé : " +
                            "${formatMontant(confirmationActuelle.montantNetAr)} " +
                            "Ar reçus, sans pénalité."
                }

                confirmation.value = null
                formulaire.value = EtatFormulaireRetrait(message = message)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                confirmation.value = null
                formulaire.update {
                    it.copy(
                        enregistrement = false,
                        erreur = "Enregistrement impossible. Réessaie."
                    )
                }
            }
        }
    }
}


