package com.example.data.repository

import android.util.Log
import com.example.data.SessionManager
import com.example.data.local.*
import com.example.data.models.*
import com.example.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class InventoryRepository(
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val salleDao = db.salleDao()
    private val equipementDao = db.equipementDao()
    private val controleDao = db.controleDao()
    private val anomalieDao = db.anomalieDao()
    private val anomalieDraftDao = db.anomalieDraftDao()

    val allSalles: Flow<List<Salle>> = salleDao.getAllSalles()
    val allEquipements: Flow<List<Equipement>> = equipementDao.getAllEquipements()
    val allControles: Flow<List<Controle>> = controleDao.getAllControles()
    val allAnomalies: Flow<List<Anomalie>> = anomalieDao.getAllAnomalies()

    suspend fun getAnomalieDraft(equipmentId: Int): AnomalieDraft? {
        return withContext(Dispatchers.IO) {
            anomalieDraftDao.getDraftByEquipmentId(equipmentId)
        }
    }

    suspend fun saveAnomalieDraft(equipmentId: Int, description: String, photoPath: String?, isCritical: Boolean) {
        withContext(Dispatchers.IO) {
            anomalieDraftDao.insertDraft(
                AnomalieDraft(equipmentId, description, photoPath, isCritical)
            )
        }
    }

    suspend fun deleteAnomalieDraft(equipmentId: Int) {
        withContext(Dispatchers.IO) {
            anomalieDraftDao.deleteDraftByEquipmentId(equipmentId)
        }
    }

    suspend fun initSeedDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val salles = allSalles.first()
            if (salles.isEmpty()) {
                Log.d("InventoryRepo", "Database represents empty. Seeding realistic equipment inventory data.")
                
                val seedSalles = listOf(
                    Salle(1, "Salle Serveurs Alpha", 1, "Centre principal d'hébergement informatique, armoires réseaux, pare-feu."),
                    Salle(2, "Salle Cryogénie & Energie", 1, "Onduleurs, climatisation industrielle hacheur électrique."),
                    Salle(3, "Bureau Supervision Admin", 2, "Consoles centrales de monitoring réseau, contrôle de sécurité.")
                )
                salleDao.insertSalles(seedSalles)

                val seedEquipements = listOf(
                    Equipement(1, "EQ-ALPHA-01", "EQ-ALPHA-01", "Serveur Web", 1, "Salle Serveurs Alpha", "PowerEdge R740", "Dell", "opérationnel", 0, "Serveur principal hébergeant le site extranet de l'institut.", "2026-06-10 14:00"),
                    Equipement(2, "EQ-ALPHA-02", "EQ-ALPHA-02", "Routeur Core", 1, "Salle Serveurs Alpha", "Catalyst 9300", "Cisco", "à contrôler", 1, "Commutateur central liant les baies réseaux de supervision.", "2026-05-30 09:15"),
                    Equipement(3, "EQ-ALPHA-03", "EQ-ALPHA-03", "Baie Stockage", 1, "Salle Serveurs Alpha", "Unity XT", "Dell", "opérationnel", 0, "Datastore principal 100 TB SAS en RAID 10.", null),
                    Equipement(4, "EQ-BETA-01", "EQ-BETA-01", "Unité Climatisation", 2, "Salle Cryogénie & Energie", "InRow RC", "APC", "en maintenance", 0, "Système d'aspiration d'air chaud inter-rangées.", "2026-06-12 11:30"),
                    Equipement(5, "EQ-BETA-02", "EQ-BETA-02", "Onduleur Central", 2, "Salle Cryogénie & Energie", "Smart-UPS", "APC", "opérationnel", 0, "Batterie de secours 10kVA assurant l'autonomie critique.", null),
                    Equipement(6, "EQ-GAMMA-01", "EQ-GAMMA-01", "Poste Supervision", 3, "Bureau Supervision Admin", "ThinkCentre", "Lenovo", "en panne", 2, "PC de monitoring dédié au mur d'images de sécurité.", "2026-06-08 16:45")
                )
                equipementDao.insertEquipements(seedEquipements)
                
                val seedAnomalies = listOf(
                    Anomalie(UUID.randomUUID().toString(), 2, "Filtre à poussière obstrué sur le ventilateur d'extraction.", "ouverte", "2026-06-12 15:40", null, true),
                    Anomalie(UUID.randomUUID().toString(), 6, "Écran noir au démarrage, carte d'alimentation de l'écran endommagée.", "ouverte", "2026-06-14 08:30", null, true),
                    Anomalie(UUID.randomUUID().toString(), 6, "Ventilateur interne du CPU fait un bruit anormal de frottement.", "en cours", "2026-06-14 09:12", null, true)
                )
                for (anom in seedAnomalies) {
                    anomalieDao.insertAnomalie(anom)
                }
            }
        }
    }

    fun getEquipementsBySalleId(salleId: Int): Flow<List<Equipement>> {
        return equipementDao.getEquipementsBySalleId(salleId)
    }

    suspend fun getEquipementByCode(code: String): Equipement? {
        return withContext(Dispatchers.IO) {
            // First check local database cache
            val local = equipementDao.getEquipementByCode(code)
            if (local != null) return@withContext local

            // Fallback try api if config coordinates are provided
            try {
                val service = ApiClient.getApiService(sessionManager)
                val response = service.getEquipementByCode(code = code)
                if (response.isSuccessful && response.body()?.success == true) {
                    val eq = response.body()?.equipement
                    if (eq != null) {
                        equipementDao.insertEquipements(listOf(eq))
                        return@withContext eq
                    }
                }
            } catch (e: Exception) {
                Log.e("InventoryRepo", "Network resolution of code failed: ${e.message}")
            }
            return@withContext null
        }
    }

    suspend fun fetchSallesAndEquipementsFromApi(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val service = ApiClient.getApiService(sessionManager)
                
                // Fetch equipments (includes salle names)
                val response = service.getEquipements(action = "list")
                if (response.isSuccessful && response.body()?.success == true) {
                    val list = response.body()?.equipements ?: emptyList()
                    if (list.isNotEmpty()) {
                        equipementDao.insertEquipements(list)
                        
                        // Extract unique Salles from equipments list
                        val sallesList = list.map { eq ->
                            Salle(eq.salle_id, eq.nom_salle, 1, "Salle importée depuis le serveur.")
                        }.distinctBy { s -> s.id }
                        
                        if (sallesList.isNotEmpty()) {
                            salleDao.insertSalles(sallesList)
                        }
                        return@withContext true
                    }
                }
                return@withContext false
            } catch (e: Exception) {
                Log.e("InventoryRepo", "Failed to sync inventory from API: ${e.message}")
                return@withContext false
            }
        }
    }

    suspend fun saveControle(
        equipementId: Int,
        statut: String,
        notes: String,
        signatureData: String,
        inspecteurNom: String,
        inspecteurId: Int
    ): Controle {
        return withContext(Dispatchers.IO) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.FRANCE).format(java.util.Date())
            val ctrl = Controle(
                id = UUID.randomUUID().toString(),
                equipement_id = equipementId,
                date_controle = dateStr,
                inspecteur_id = inspecteurId,
                inspecteur_nom = inspecteurNom,
                statut = statut,
                signature_path = signatureData,
                notes = notes,
                is_synced = false
            )
            // Save state in DB
            controleDao.insertControle(ctrl)

            // Update equipment cache values
            val eq = equipementDao.getEquipementById(equipementId)
            if (eq != null) {
                val currentAnoms = eq.nb_anomalies
                equipementDao.updateEquipementStatus(
                    id = equipementId,
                    statut = if (statut == "conforme") "opérationnel" else "à contrôler",
                    date = dateStr,
                    nbAnomalies = currentAnoms
                )
            }

            // Sync immediately if possible
            trySyncControle(ctrl)
            return@withContext ctrl
        }
    }

    suspend fun saveAnomalie(
        equipementId: Int,
        description: String,
        photoData: String? = null
    ): Anomalie {
        return withContext(Dispatchers.IO) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.FRANCE).format(java.util.Date())
            val anom = Anomalie(
                id = UUID.randomUUID().toString(),
                equipement_id = equipementId,
                description = description,
                statut = "ouverte",
                date_creation = dateStr,
                photo_path = photoData,
                is_synced = false
            )
            anomalieDao.insertAnomalie(anom)

            // Increment equipment anomaly count and change state to "à contrôler"
            val eq = equipementDao.getEquipementById(equipementId)
            if (eq != null) {
                val newCount = eq.nb_anomalies + 1
                equipementDao.updateEquipementStatus(
                    id = equipementId,
                    statut = "à contrôler",
                    date = eq.date_dernier_controle ?: dateStr,
                    nbAnomalies = newCount
                )
            }

            trySyncAnomalie(anom)
            return@withContext anom
        }
    }

    private suspend fun trySyncControle(c: Controle): Boolean {
        return try {
            val service = ApiClient.getApiService(sessionManager)
            val params = mapOf(
                "action" to "submit_controle",
                "id" to c.id,
                "equipement_id" to c.equipement_id.toString(),
                "date_controle" to c.date_controle,
                "inspecteur_id" to c.inspecteur_id.toString(),
                "inspecteur_nom" to c.inspecteur_nom,
                "statut" to c.statut,
                "notes" to c.notes,
                "signature" to c.signature_path
            )
            val response = service.submitControle(params)
            if (response.isSuccessful && response.body()?.get("success") == true) {
                controleDao.markSynced(c.id)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Controle sync post failed immediately (Expected if PHP endpoint not configured or offline): ${e.message}")
            false
        }
    }

    private suspend fun trySyncAnomalie(a: Anomalie): Boolean {
        return try {
            val service = ApiClient.getApiService(sessionManager)
            val params = mapOf(
                "action" to "submit_anomalie",
                "id" to a.id,
                "equipement_id" to a.equipement_id.toString(),
                "description" to a.description,
                "statut" to a.statut,
                "date_creation" to a.date_creation,
                "photo" to (a.photo_path ?: "")
            )
            val response = service.submitAnomalie(params)
            if (response.isSuccessful && response.body()?.get("success") == true) {
                anomalieDao.markSynced(a.id)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("InventoryRepo", "Anomalie sync post failed immediately: ${e.message}")
            false
        }
    }

    suspend fun syncAllOfflineData(): SyncResult {
        return withContext(Dispatchers.IO) {
            var controlsSynced = 0
            var anomaliesSynced = 0
            var syncSucceeded = true

            // Fetch unsynced components
            val unsyncedCtrls = controleDao.getUnsyncedControles()
            for (c in unsyncedCtrls) {
                val ok = trySyncControle(c)
                if (ok) controlsSynced++ else syncSucceeded = false
            }

            val unsyncedAnoms = anomalieDao.getUnsyncedAnomalies()
            for (a in unsyncedAnoms) {
                val ok = trySyncAnomalie(a)
                if (ok) anomaliesSynced++ else syncSucceeded = false
            }

            // Also reload equipment definitions
            val reloadOk = fetchSallesAndEquipementsFromApi()
            if (!reloadOk) {
                syncSucceeded = false
            }

            SyncResult(controlsSynced, anomaliesSynced, syncSucceeded)
        }
    }
}

data class SyncResult(
    val controlsCount: Int,
    val anomaliesCount: Int,
    val success: Boolean
)
