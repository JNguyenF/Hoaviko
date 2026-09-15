package mg.hoaviko.app

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UtilisateurAdmin(
    val uid: String,
    val nom: String,
    val prenom: String,
    val dateNaissance: String,
    val cin: String
)

data class EtatUtilisateursAdmin(
    val chargement: Boolean = true,
    val utilisateurs: List<UtilisateurAdmin> = emptyList(),
    val erreur: String? = null
)

class UtilisateursAdminViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(EtatUtilisateursAdmin())
    val uiState = _uiState.asStateFlow()

    init {
        chargerUtilisateurs()
    }

    fun chargerUtilisateurs() {
        val uidConnecte = auth.currentUser?.uid

        if (uidConnecte == null) {
            _uiState.value = EtatUtilisateursAdmin(
                chargement = false,
                erreur = "Tu dois être connecté."
            )
            return
        }

        _uiState.value = EtatUtilisateursAdmin()

        // Les règles Firestore réservent cette lecture globale
        // aux administrateurs.
        db.collection("utilisateurs")
            .get(Source.SERVER)
            .addOnSuccessListener { resultat ->
                if (auth.currentUser?.uid != uidConnecte) {
                    return@addOnSuccessListener
                }

                val utilisateurs = resultat.documents.map { document ->
                    UtilisateurAdmin(
                        uid = document.id,
                        nom = document.getString("nom").orEmpty(),
                        prenom = document.getString("prenom").orEmpty(),
                        dateNaissance = document
                            .getString("dateNaissance").orEmpty(),
                        cin = document.getString("cin").orEmpty()
                    )
                }.sortedWith(
                    compareBy<UtilisateurAdmin> { it.nom.lowercase() }
                        .thenBy { it.prenom.lowercase() }
                )

                _uiState.value = EtatUtilisateursAdmin(
                    chargement = false,
                    utilisateurs = utilisateurs
                )
            }
            .addOnFailureListener {
                if (auth.currentUser?.uid == uidConnecte) {
                    _uiState.value = EtatUtilisateursAdmin(
                        chargement = false,
                        erreur = "Impossible de charger les profils. " +
                                "Vérifie Internet et tes droits administrateur."
                    )
                }
            }
    }
}