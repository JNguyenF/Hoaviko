package mg.hoaviko.app

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// montantBrutAr est débité du solde (cotisations - retraits).
// montantNetAr est le montant "reçu" une fois la pénalité déduite.
@Entity(
    tableName = "retraits",
    indices = [Index(value = ["utilisateurId"])]
)
data class Retrait(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val utilisateurId: String,

    val montantBrutAr: Long,

    val montantNetAr: Long,

    val tauxPenalite: Int,

    val modeVersement: String,

    val dateRetrait: Long = System.currentTimeMillis(),

    val simulee: Boolean = true
)


