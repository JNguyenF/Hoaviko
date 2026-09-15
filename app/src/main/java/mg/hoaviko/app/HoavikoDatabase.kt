package mg.hoaviko.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Cotisation::class],
    version = 2,
    exportSchema = true
)
abstract class HoavikoDatabase : RoomDatabase() {

    abstract fun cotisationDao(): CotisationDao

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

        fun getInstance(context: Context): HoavikoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HoavikoDatabase::class.java,
                    "hoaviko.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

