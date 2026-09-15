package mg.hoaviko.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EtatFormulaireCotisation(
    val montant: String = "",
    val modeVersement: String = "Orange Money",
    val enregistrement: Boolean = false,
    val erreur: String? = null,
    val message: String? = null
)

data class EtatCotisations(
    val chargement: Boolean = true,
    val cotisations: List<Cotisation> = emptyList(),
    val totalAr: Long = 0L,
    val formulaire: EtatFormulaireCotisation = EtatFormulaireCotisation(),
    val erreurLecture: String? = null
)

class CotisationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val utilisateurId = requireNotNull(
        FirebaseAuth.getInstance().currentUser?.uid
    ) {
        "Une connexion est nécessaire pour accéder aux cotisations."
    }

    private val repository = CotisationRepository(
        HoavikoDatabase.getInstance(application).cotisationDao()
    )

    private val formulaire = MutableStateFlow(
        EtatFormulaireCotisation()
    )

    val uiState: StateFlow<EtatCotisations> = combine(
        repository.observerCotisations(utilisateurId),
        repository.observerTotal(utilisateurId),
        formulaire
    ) { cotisations, total, saisie ->
        EtatCotisations(
            chargement = false,
            cotisations = cotisations,
            totalAr = total,
            formulaire = saisie
        )
    }.catch {
        emit(
            EtatCotisations(
                chargement = false,
                erreurLecture = "Impossible de lire les cotisations. " +
                        "Quitte cet écran puis réessaie."
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EtatCotisations()
    )

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

    fun ajouterCotisation() {
        if (formulaire.value.enregistrement) return

        // Vérifie que la session correspond toujours à ce ViewModel.
        if (FirebaseAuth.getInstance().currentUser?.uid != utilisateurId) {
            formulaire.update {
                it.copy(erreur = "La session a changé. Reconnecte-toi.")
            }
            return
        }

        val montant = formulaire.value.montant.toLongOrNull()

        if (montant == null || montant <= 0L) {
            formulaire.update {
                it.copy(
                    erreur = "Saisis un montant supérieur à zéro.",
                    message = null
                )
            }
            return
        }

        formulaire.update {
            it.copy(
                enregistrement = true,
                erreur = null,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                repository.ajouter(
                    utilisateurId = utilisateurId,
                    montantAr = montant,
                    modeVersement = formulaire.value.modeVersement
                )

                formulaire.value = EtatFormulaireCotisation(
                    message = "Cotisation fictive enregistrée."
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
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

