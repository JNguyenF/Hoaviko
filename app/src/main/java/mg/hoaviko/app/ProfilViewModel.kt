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

        val reference = db.collection("utilisateurs").document(uid)

        // Transforme le document Firestore en profil.
        fun lireProfil(
            document: com.google.firebase.firestore.DocumentSnapshot
        ): ProfilUtilisateur {
            return ProfilUtilisateur(
                nom = document.getString("nom").orEmpty(),
                prenom = document.getString("prenom").orEmpty(),
                dateNaissance = document
                    .getString("dateNaissance").orEmpty(),
                cin = document.getString("cin").orEmpty()
            )
        }

        // Actualise le profil lorsque le serveur est accessible.
        fun actualiserDepuisServeur() {
            reference.get(Source.SERVER)
                .addOnSuccessListener { document ->
                    if (auth.currentUser?.uid != uid) {
                        return@addOnSuccessListener
                    }

                    _uiState.value = EtatProfil(
                        chargement = false,
                        lectureReussie = true,
                        profil = if (document.exists()) {
                            lireProfil(document)
                        } else {
                            null
                        }
                    )
                }
                .addOnFailureListener { exception ->
                    if (auth.currentUser?.uid != uid) {
                        return@addOnFailureListener
                    }

                    val code = (
                            exception as?
                                    com.google.firebase.firestore.FirebaseFirestoreException
                            )?.code

                    val accesRefuse =
                        code == com.google.firebase.firestore
                            .FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                                code == com.google.firebase.firestore
                            .FirebaseFirestoreException.Code.UNAUTHENTICATED

                    if (accesRefuse) {
                        _uiState.value = EtatProfil(
                            chargement = false,
                            erreur = "Accès au profil refusé. " +
                                    "Vérifie ta connexion au compte " +
                                    "et les règles Firestore."
                        )
                    } else if (_uiState.value.profil == null) {
                        // Sans copie locale, on ne peut pas ouvrir le profil.
                        _uiState.value = EtatProfil(
                            chargement = false,
                            erreur = "Aucun profil disponible sur ce téléphone. " +
                                    "Connecte-toi à Internet puis appuie sur Réessayer."
                        )
                    }

                    // Si un profil local est déjà affiché,
                    // une panne réseau ne bloque pas l'application.
                }
        }

        // Commence par la copie locale : pas d'attente du réseau.
        reference.get(Source.CACHE)
            .addOnSuccessListener { document ->
                if (auth.currentUser?.uid != uid) {
                    return@addOnSuccessListener
                }

                if (document.exists()) {
                    _uiState.value = EtatProfil(
                        chargement = false,
                        lectureReussie = true,
                        profil = lireProfil(document)
                    )
                }

                // L'absence dans le cache ne prouve pas que le profil
                // est absent du serveur : il faut le vérifier.
                actualiserDepuisServeur()
            }
            .addOnFailureListener {
                if (auth.currentUser?.uid != uid) {
                    return@addOnFailureListener
                }

                actualiserDepuisServeur()
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