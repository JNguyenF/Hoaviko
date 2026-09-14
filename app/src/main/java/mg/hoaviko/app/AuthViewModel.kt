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

data class EtatConnexion(
    val chargement: Boolean = false,
    val emailConnecte: String? = null,
    val erreur: String? = null
)

data class SessionUtilisateur(
    val uid: String,
    val email: String
)

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(EtatInscription())
    val uiState: StateFlow<EtatInscription> = _uiState.asStateFlow()

    private val _connexionState = MutableStateFlow(
        EtatConnexion(emailConnecte = auth.currentUser?.email)
    )
    val connexionState: StateFlow<EtatConnexion> =
        _connexionState.asStateFlow()

    // Récupère la session existante dès le démarrage.
    private val _session = MutableStateFlow(
        auth.currentUser?.let { utilisateur ->
            SessionUtilisateur(
                uid = utilisateur.uid,
                email = utilisateur.email.orEmpty()
            )
        }
    )

    val session: StateFlow<SessionUtilisateur?> =
        _session.asStateFlow()

    // Observe les connexions et les déconnexions.
    private val observateurSession = FirebaseAuth.AuthStateListener {
            firebaseAuth ->

        val utilisateur = firebaseAuth.currentUser

        _session.value = utilisateur?.let {
            SessionUtilisateur(
                uid = it.uid,
                email = it.email.orEmpty()
            )
        }

        _connexionState.value = _connexionState.value.copy(
            emailConnecte = utilisateur?.email
        )
    }

    init {
        auth.addAuthStateListener(observateurSession)
    }

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

    fun connecter(email: String, motDePasse: String) {
        if (_connexionState.value.chargement) {
            return
        }

        if (email.isBlank() || motDePasse.isBlank()) {
            _connexionState.value = EtatConnexion(
                erreur = "Renseigne ton e-mail et ton mot de passe."
            )
            return
        }

        _connexionState.value = EtatConnexion(chargement = true)

        auth.signInWithEmailAndPassword(
            email.trim(),
            motDePasse
        ).addOnCompleteListener { resultat ->
            if (resultat.isSuccessful) {
                _connexionState.value = EtatConnexion(
                    emailConnecte = auth.currentUser?.email
                )
            } else {
                val message = when (resultat.exception) {
                    is FirebaseNetworkException ->
                        "Connexion impossible. Vérifie Internet."

                    else ->
                        "Connexion impossible. Vérifie tes identifiants " +
                                "ou réessaie plus tard."
                }

                _connexionState.value = EtatConnexion(
                    erreur = message
                )
            }
        }
    }

    fun deconnecter() {
        auth.signOut()
        _uiState.value = EtatInscription()
        _connexionState.value = EtatConnexion()
    }

    fun effacerErreurConnexion() {
        _connexionState.value = _connexionState.value.copy(
            erreur = null
        )
    }

    override fun onCleared() {
        auth.removeAuthStateListener(observateurSession)
        super.onCleared()
    }
}