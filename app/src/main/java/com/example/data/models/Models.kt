package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "salles")
data class Salle(
    @PrimaryKey val id: Int,
    val nom_salle: String,
    val etage: Int,
    val description: String
)

@Entity(tableName = "equipements")
data class Equipement(
    @PrimaryKey val id: Int,
    val code_inventaire: String,
    val qr_code: String,
    val type: String,
    val salle_id: Int,
    val nom_salle: String,
    val const_modele: String,
    val const_fabricant: String,
    val statut: String, // "opérationnel", "en panne", "en maintenance", "à contrôler"
    val nb_anomalies: Int = 0,
    val description: String = "",
    val date_dernier_controle: String? = null
)

@Entity(tableName = "controles")
data class Controle(
    @PrimaryKey val id: String, // UUID string
    val equipement_id: Int,
    val date_controle: String,
    val inspecteur_id: Int,
    val inspecteur_nom: String,
    val statut: String, // "conforme", "non conforme"
    val signature_path: String, // Drawing path string data e.g. "x1,y1;x2,y2..."
    val notes: String = "",
    val is_synced: Boolean = false
)

@Entity(tableName = "anomalies")
data class Anomalie(
    @PrimaryKey val id: String, // UUID string
    val equipement_id: Int,
    val description: String,
    val statut: String, // "ouverte", "en cours", "résolue"
    val date_creation: String,
    val photo_path: String? = null,
    val is_synced: Boolean = false
)

@Entity(tableName = "anomalie_drafts")
data class AnomalieDraft(
    @PrimaryKey val equipment_id: Int,
    val description: String,
    val photo_path: String? = null,
    val is_critical: Boolean = false
)

data class User(
    val id: Int,
    val nom: String,
    val prenom: String,
    val email: String,
    val role: String
)

data class LoginResponse(
    val success: Boolean,
    val session_id: String? = null,
    val user: User? = null,
    val message: String? = null
)

data class EquipementsResponse(
    val success: Boolean,
    val equipements: List<Equipement>? = null,
    val message: String? = null
)

data class EquipementDetailResponse(
    val success: Boolean,
    val equipement: Equipement? = null,
    val message: String? = null
)
