package mg.hoaviko.app

import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EtatInscription(
    val chargement: Boolean = false,
    val compteCree: Boolean = false,
    val erreur: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(EtatInscription())
    val uiState: StateFlow<EtatInscription> = _uiState.asStateFlow()

    fun inscrire(email: String, motDePasse: String) {
        if (_uiState.value.chargement || _uiState.value.compteCree) {
            return
        }

        if (email.isBlank() || motDePasse.length < 8) {
            _uiState.value = EtatInscription(
                erreur = "Vérifie l’e-mail et le mot de passe."
            )
            return
        }

        _uiState.value = EtatInscription(chargement = true)

        auth.createUserWithEmailAndPassword(
            email.trim(),
            motDePasse
        ).addOnCompleteListener { resultat ->

            if (resultat.isSuccessful) {
                _uiState.value = EtatInscription(compteCree = true)
            } else {
                val message = when (resultat.exception) {
                    is FirebaseNetworkException ->
                        "Connexion impossible. Vérifie Internet."

                    is FirebaseAuthUserCollisionException ->
                        "Impossible de créer un compte avec cette adresse. " +
                                "Un compte existe peut-être déjà."

                    is FirebaseAuthWeakPasswordException ->
                        "Le mot de passe ne respecte pas les règles de sécurité."

                    else ->
                        "Impossible de créer le compte. Réessaie plus tard."
                }

                _uiState.value = EtatInscription(erreur = message)
            }
        }
    }
}

