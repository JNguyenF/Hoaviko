package mg.hoaviko.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RetraitDao {

    @Insert
    suspend fun ajouter(retrait: Retrait)

    // Historique du propriétaire, du plus récent au plus ancien.
    @Query(
        """
        SELECT * FROM retraits
        WHERE utilisateurId = :utilisateurId
        ORDER BY dateRetrait DESC, id DESC
        """
    )
    fun observerRetraits(
        utilisateurId: String
    ): Flow<List<Retrait>>

    // Renvoie zéro si aucun retrait n'est enregistré.
    @Query(
        """
        SELECT COALESCE(SUM(montantBrutAr), 0)
        FROM retraits
        WHERE utilisateurId = :utilisateurId
        """
    )
    fun observerTotal(
        utilisateurId: String
    ): Flow<Long>
}


