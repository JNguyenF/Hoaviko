package mg.hoaviko.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Cotisation::class, Retrait::class],
    version = 3,
    exportSchema = true
)
abstract class HoavikoDatabase : RoomDatabase() {

    abstract fun cotisationDao(): CotisationDao
    abstract fun retraitDao(): RetraitDao

    companion object {
        @Volatile
        private var INSTANCE: HoavikoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE cotisations
                    ADD COLUMN modeVersement TEXT NOT NULL
                    DEFAULT 'Non renseigné'
                    """.trimIndent()
                )
            }
        }

        // Ajoute la table des retraits (fonctionnalité de retrait d'argent).
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `retraits` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `utilisateurId` TEXT NOT NULL,
                    `montantBrutAr` INTEGER NOT NULL,
                    `montantNetAr` INTEGER NOT NULL,
                    `tauxPenalite` INTEGER NOT NULL,
                    `modeVersement` TEXT NOT NULL,
                    `dateRetrait` INTEGER NOT NULL,
                    `simulee` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    `index_retraits_utilisateurId`
                    ON `retraits` (`utilisateurId`)
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): HoavikoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HoavikoDatabase::class.java,
                    "hoaviko.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}