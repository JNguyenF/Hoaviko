package mg.hoaviko.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CotisationDao {

    @Insert
    suspend fun ajouter(cotisation: Cotisation)

    // Historique du propriétaire, du plus récent au plus ancien.
    @Query(
        """
        SELECT * FROM cotisations
        WHERE utilisateurId = :utilisateurId
        ORDER BY dateVersement DESC, id DESC
        """
    )
    fun observerCotisations(
        utilisateurId: String
    ): Flow<List<Cotisation>>

    // Renvoie zéro si aucune cotisation n'est enregistrée.
    @Query(
        """
        SELECT COALESCE(SUM(montantAr), 0)
        FROM cotisations
        WHERE utilisateurId = :utilisateurId
        """
    )
    fun observerTotal(
        utilisateurId: String
    ): Flow<Long>
}
