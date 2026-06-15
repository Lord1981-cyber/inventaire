package com.example.data.local

import androidx.room.*
import com.example.data.models.Salle
import com.example.data.models.Equipement
import com.example.data.models.Controle
import com.example.data.models.Anomalie
import com.example.data.models.AnomalieDraft
import kotlinx.coroutines.flow.Flow

@Dao
interface SalleDao {
    @Query("SELECT * FROM salles ORDER BY nom_salle ASC")
    fun getAllSalles(): Flow<List<Salle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalles(salles: List<Salle>)

    @Query("DELETE FROM salles")
    suspend fun clearAll()
}

@Dao
interface EquipementDao {
    @Query("SELECT * FROM equipements ORDER BY nom_salle ASC, type ASC")
    fun getAllEquipements(): Flow<List<Equipement>>

    @Query("SELECT * FROM equipements")
    suspend fun getAllEquipementsOnce(): List<Equipement>

    @Query("SELECT * FROM equipements WHERE id = :id")
    suspend fun getEquipementById(id: Int): Equipement?

    @Query("SELECT * FROM equipements WHERE salle_id = :salleId ORDER BY type ASC, code_inventaire ASC")
    fun getEquipementsBySalleId(salleId: Int): Flow<List<Equipement>>

    @Query("SELECT * FROM equipements WHERE code_inventaire = :code OR qr_code = :code")
    suspend fun getEquipementByCode(code: String): Equipement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipements(equipements: List<Equipement>)

    @Query("UPDATE equipements SET statut = :statut, date_dernier_controle = :date, nb_anomalies = :nbAnomalies WHERE id = :id")
    suspend fun updateEquipementStatus(id: Int, statut: String, date: String, nbAnomalies: Int)

    @Query("DELETE FROM equipements")
    suspend fun clearAll()
}

@Dao
interface ControleDao {
    @Query("SELECT * FROM controles ORDER BY date_controle DESC")
    fun getAllControles(): Flow<List<Controle>>

    @Query("SELECT * FROM controles WHERE equipement_id = :equipementId ORDER BY date_controle DESC")
    fun getControlesByEquipementId(equipementId: Int): Flow<List<Controle>>

    @Query("SELECT * FROM controles WHERE is_synced = 0")
    suspend fun getUnsyncedControles(): List<Controle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertControle(controle: Controle)

    @Query("UPDATE controles SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface AnomalieDao {
    @Query("SELECT * FROM anomalies ORDER BY date_creation DESC")
    fun getAllAnomalies(): Flow<List<Anomalie>>

    @Query("SELECT * FROM anomalies WHERE equipement_id = :equipementId ORDER BY date_creation DESC")
    fun getAnomaliesByEquipementId(equipementId: Int): Flow<List<Anomalie>>

    @Query("SELECT * FROM anomalies WHERE is_synced = 0")
    suspend fun getUnsyncedAnomalies(): List<Anomalie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomalie(anomalie: Anomalie)

    @Query("UPDATE anomalies SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface AnomalieDraftDao {
    @Query("SELECT * FROM anomalie_drafts WHERE equipment_id = :equipmentId")
    suspend fun getDraftByEquipmentId(equipmentId: Int): AnomalieDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: AnomalieDraft)

    @Query("DELETE FROM anomalie_drafts WHERE equipment_id = :equipmentId")
    suspend fun deleteDraftByEquipmentId(equipmentId: Int)
}

@Database(entities = [Salle::class, Equipement::class, Controle::class, Anomalie::class, AnomalieDraft::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun salleDao(): SalleDao
    abstract fun equipementDao(): EquipementDao
    abstract fun controleDao(): ControleDao
    abstract fun anomalieDao(): AnomalieDao
    abstract fun anomalieDraftDao(): AnomalieDraftDao
}
