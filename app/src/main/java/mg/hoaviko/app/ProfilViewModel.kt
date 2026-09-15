package mg.hoaviko.app

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfilUtilisateur(
    val nom: String = "",
    val prenom: String = "",
    val dateNaissance: String = "",
    val cin: String = ""
)

data class EtatProfil(
    val chargement: Boolean = true,
    val lectureReussie: Boolean = false,
    val enregistrement: Boolean = false,
    val profil: ProfilUtilisateur? = null,
    val erreur: String? = null
)

class ProfilViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(EtatProfil())
    val uiState = _uiState.asStateFlow()

    init {
        chargerProfil()
    }

    fun chargerProfil() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            _uiState.value = EtatProfil(
                chargement = false,
                erreur = "Tu dois être connecté."
            )
            return
        }

        _uiState.value = EtatProfil()

        db.collection("utilisateurs")
            .document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->
                if (auth.currentUser?.uid != uid) {
                    return@addOnSuccessListener
                }

                val profil = if (document.exists()) {
                    ProfilUtilisateur(
                        nom = document.getString("nom").orEmpty(),
                        prenom = document.getString("prenom").orEmpty(),
                        dateNaissance = document
                            .getString("dateNaissance").orEmpty(),
                        cin = document.getString("cin").orEmpty()
                    )
                } else {
                    null
                }

                _uiState.value = EtatProfil(
                    chargement = false,
                    lectureReussie = true,
                    profil = profil
                )
            }
            .addOnFailureListener {
                if (auth.currentUser?.uid != uid) {
                    return@addOnFailureListener
                }

                _uiState.value = EtatProfil(
                    chargement = false,
                    erreur = "Impossible de charger le profil. " +
                            "Vérifie Internet et les règles Firestore."
                )
            }
    }

    fun enregistrerProfil(
        nom: String,
        prenom: String,
        dateNaissance: String,
        cin: String
    ) {
        if (_uiState.value.enregistrement) return

        val uid = auth.currentUser?.uid ?: return

        val erreur = when {
            nom.isBlank() || prenom.isBlank() ->
                "Renseigne ton nom et ton prénom."

            !dateNaissanceValide(dateNaissance) ->
                "Choisis une date de naissance valide."

            cin.isBlank() || cin.any { it !in '0'..'9' } ->
                "Renseigne un numéro de CIN composé de chiffres."

            else -> null
        }

        if (erreur != null) {
            _uiState.value = _uiState.value.copy(erreur = erreur)
            return
        }

        val profil = ProfilUtilisateur(
            nom = nom.trim(),
            prenom = prenom.trim(),
            dateNaissance = dateNaissance,
            cin = cin
        )

        _uiState.value = _uiState.value.copy(
            enregistrement = true,
            erreur = null
        )

        // Aucun mot de passe n'est enregistré dans Firestore.
        val donnees = mapOf(
            "nom" to profil.nom,
            "prenom" to profil.prenom,
            "dateNaissance" to profil.dateNaissance,
            "cin" to profil.cin
        )

        db.collection("utilisateurs")
            .document(uid)
            .set(donnees)
            .addOnSuccessListener {
                if (auth.currentUser?.uid != uid) {
                    return@addOnSuccessListener
                }

                _uiState.value = EtatProfil(
                    chargement = false,
                    lectureReussie = true,
                    profil = profil
                )
            }
            .addOnFailureListener {
                if (auth.currentUser?.uid != uid) {
                    return@addOnFailureListener
                }

                _uiState.value = _uiState.value.copy(
                    enregistrement = false,
                    erreur = "Enregistrement impossible. Réessaie."
                )
            }
    }
}