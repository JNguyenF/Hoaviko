package mg.hoaviko.app

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DemandeRetraite(
    val id: String,
    val motif: String,
    val justificatif: String,
    val statut: String,
    val commentaire: String
)

data class EtatDemandesRetraite(
    val chargement: Boolean = true,
    val traitement: Boolean = false,
    val demandes: List<DemandeRetraite> = emptyList(),
    val erreur: String? = null
)

class DemandeRetraiteViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("demandesRetraite")

    private val _uiState = MutableStateFlow(EtatDemandesRetraite())
    val uiState = _uiState.asStateFlow()

    private fun convertir(document: DocumentSnapshot): DemandeRetraite {
        return DemandeRetraite(
            id = document.id,
            motif = document.getString("motif").orEmpty(),
            justificatif = document.getString("justificatif").orEmpty(),
            statut = document.getString("statut").orEmpty(),
            commentaire = document.getString("commentaire").orEmpty()
        )
    }

    fun charger(administrateur: Boolean) {
        if (_uiState.value.traitement) return

        val uid = auth.currentUser?.uid ?: return

        _uiState.value = EtatDemandesRetraite()

        if (administrateur) {
            collection.get(Source.SERVER)
                .addOnSuccessListener { resultat ->
                    if (auth.currentUser?.uid != uid) {
                        return@addOnSuccessListener
                    }

                    _uiState.value = EtatDemandesRetraite(
                        chargement = false,
                        demandes = resultat.documents
                            .map { convertir(it) }
                            .sortedBy { it.statut != "EN_ATTENTE" }
                    )
                }
                .addOnFailureListener {
                    if (auth.currentUser?.uid == uid) {
                        afficherErreur(
                            "Lecture impossible. Vérifie Internet " +
                                    "et tes droits administrateur."
                        )
                    }
                }
        } else {
            collection.document(uid).get(Source.SERVER)
                .addOnSuccessListener { document ->
                    if (auth.currentUser?.uid != uid) {
                        return@addOnSuccessListener
                    }

                    _uiState.value = EtatDemandesRetraite(
                        chargement = false,
                        demandes = if (document.exists()) {
                            listOf(convertir(document))
                        } else {
                            emptyList()
                        }
                    )
                }
                .addOnFailureListener {
                    if (auth.currentUser?.uid == uid) {
                        afficherErreur(
                            "Lecture impossible. Vérifie Internet " +
                                    "et les règles Firestore."
                        )
                    }
                }
        }
    }

    fun envoyer(motif: String, justificatif: String) {
        if (_uiState.value.traitement) return

        val uid = auth.currentUser?.uid ?: return
        val motifNettoye = motif.trim()
        val reference = justificatif.trim()

        if (motifNettoye.length !in 10..1000) {
            afficherErreur(
                "Le motif doit contenir entre 10 et 1 000 caractères."
            )
            return
        }

        if (reference.length !in 3..200) {
            afficherErreur(
                "Renseigne une référence de justificatif fictif."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            traitement = true,
            erreur = null
        )

        val document = collection.document(uid)

        // Identifiant unique pour l'éventuelle archive.
        val archive = document.collection("historique").document()

        db.runTransaction { transaction ->
            val ancienneDemande = transaction.get(document)

            val nouvelleDemande = mutableMapOf<String, Any>(
                "utilisateurId" to uid,
                "motif" to motifNettoye,
                "justificatif" to reference,
                "statut" to "EN_ATTENTE",
                "commentaire" to "",
                "creeLe" to FieldValue.serverTimestamp()
            )

            if (ancienneDemande.exists()) {
                check(
                    ancienneDemande.getString("statut") == "REFUSEE"
                ) {
                    "Une nouvelle demande est possible uniquement après un refus."
                }

                // Conserve le motif, la décision et les dates précédentes.
                transaction.set(
                    archive,
                    requireNotNull(ancienneDemande.data)
                )

                nouvelleDemande["precedentId"] = archive.id
            }

            transaction.set(document, nouvelleDemande)

            true
        }.addOnSuccessListener {
            if (auth.currentUser?.uid == uid) {
                _uiState.value = _uiState.value.copy(
                    traitement = false
                )
                charger(administrateur = false)
            }
        }.addOnFailureListener {
            if (auth.currentUser?.uid == uid) {
                afficherErreur(
                    "Envoi impossible. Vérifie Internet et actualise la page. " +
                            "Une demande en attente ou acceptée ne peut pas être remplacée."
                )
            }
        }
    }

    fun decider(
        demandeId: String,
        accepter: Boolean,
        commentaire: String
    ) {
        if (_uiState.value.traitement) return

        val uid = auth.currentUser?.uid ?: return
        val texte = commentaire.trim()

        if (demandeId == uid) {
            afficherErreur("Tu ne peux pas traiter ta propre demande.")
            return
        }

        if (texte.length !in 3..1000) {
            afficherErreur("Ajoute un commentaire entre 3 et 1 000 caractères.")
            return
        }

        _uiState.value = _uiState.value.copy(
            traitement = true,
            erreur = null
        )

        val document = collection.document(demandeId)

        db.runTransaction { transaction ->
            val demande = transaction.get(document)

            check(demande.getString("statut") == "EN_ATTENTE") {
                "Cette demande a déjà été traitée."
            }

            transaction.update(
                document,
                mapOf(
                    "statut" to if (accepter) "ACCEPTEE" else "REFUSEE",
                    "commentaire" to texte,
                    "traitePar" to uid,
                    "traiteLe" to FieldValue.serverTimestamp()
                )
            )
            true
        }.addOnSuccessListener {
            if (auth.currentUser?.uid == uid) {
                _uiState.value = _uiState.value.copy(traitement = false)
                charger(administrateur = true)
            }
        }.addOnFailureListener {
            if (auth.currentUser?.uid == uid) {
                afficherErreur(
                    "Décision impossible. Vérifie Internet et tes droits, " +
                            "puis actualise : la demande a peut-être déjà été traitée."
                )
            }
        }
    }

    private fun afficherErreur(message: String) {
        _uiState.value = _uiState.value.copy(
            chargement = false,
            traitement = false,
            erreur = message
        )
    }
}

