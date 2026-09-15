package mg.hoaviko.app

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EtatRole(
    val chargement: Boolean = true,
    val administrateur: Boolean = false,
    val erreur: String? = null
)

class RoleViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(EtatRole())
    val uiState = _uiState.asStateFlow()

    init {
        verifierRole()
    }

    fun verifierRole() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            _uiState.value = EtatRole(
                chargement = false,
                erreur = "Tu dois être connecté."
            )
            return
        }

        _uiState.value = EtatRole(chargement = true)

        // Vérifie le rôle sur le serveur, pas dans le cache local.
        db.collection("administrateurs")
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (auth.currentUser?.uid != uid) {
                    return@addOnSuccessListener
                }

                _uiState.value = EtatRole(
                    chargement = false,
                    administrateur = document.exists() &&
                            document.getBoolean("actif") == true
                )
            }
            .addOnFailureListener {
                if (auth.currentUser?.uid != uid) {
                    return@addOnFailureListener
                }

                _uiState.value = EtatRole(
                    chargement = false,
                    erreur = "Impossible de vérifier le rôle. " +
                            "Vérifie Internet et les règles Firestore."
                )
            }
    }
}

