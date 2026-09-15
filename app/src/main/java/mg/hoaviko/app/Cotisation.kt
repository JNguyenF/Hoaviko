package mg.hoaviko.app

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cotisations",
    indices = [Index(value = ["utilisateurId"])]
)
data class Cotisation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val utilisateurId: String,

    val montantAr: Long,

    val dateVersement: Long = System.currentTimeMillis(),

    val simulee: Boolean = true,

    @ColumnInfo(defaultValue = "'Non renseigné'")
    val modeVersement: String = "Non renseigné"
)
