package com.example.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.SessionManager
import com.example.data.ControleDraft
import com.example.data.local.AppDatabase
import com.example.data.models.*
import com.example.data.remote.ApiClient
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

data class CriticalAlert(
    val equipmentId: Int,
    val equipmentCode: String,
    val description: String
)

sealed class AppScreen {
    object Login : AppScreen()
    object Dashboard : AppScreen()
    object Scan : AppScreen()
    data class Detail(val equipmentId: Int) : AppScreen()
    data class Controle(val equipmentId: Int) : AppScreen()
    data class Anomalie(val equipmentId: Int) : AppScreen()
    object Historique : AppScreen()
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "institut_inventaire.db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: InventoryRepository by lazy {
        InventoryRepository(database, sessionManager)
    }

    // Screens routing
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Login)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<AppScreen>()

    // Global lists
    val salles: StateFlow<List<Salle>> = repository.allSalles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipements: StateFlow<List<Equipement>> = repository.allEquipements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val controles: StateFlow<List<Controle>> = repository.allControles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val anomalies: StateFlow<List<Anomalie>> = repository.allAnomalies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active logged in user
    private val _currentUser = MutableStateFlow<User?>(sessionManager.getUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // API settings state
    private val _apiBaseUrl = MutableStateFlow(sessionManager.getApiBaseUrl())
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    // UI state states
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _activeCriticalAlert = MutableStateFlow<CriticalAlert?>(null)
    val activeCriticalAlert: StateFlow<CriticalAlert?> = _activeCriticalAlert.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun triggerCriticalAlert(equipmentId: Int, equipmentCode: String, description: String) {
        _activeCriticalAlert.value = CriticalAlert(equipmentId, equipmentCode, description)
    }

    fun clearCriticalAlert() {
        _activeCriticalAlert.value = null
    }

    init {
        // Init cache database with default items if it has nothing
        viewModelScope.launch {
            repository.initSeedDataIfEmpty()
            if (sessionManager.isLoggedIn()) {
                _currentScreen.value = AppScreen.Dashboard
            }
        }

        // Register network callback for auto-sync when connection is restored
        try {
            val connectivityManager = application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                private var wasOffline = false

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d("InventoryVM", "Internet connection restored! Triggering auto-sync.")
                    _isOnline.value = true
                    if (wasOffline) {
                        viewModelScope.launch {
                            performSync()
                        }
                    }
                    wasOffline = false
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.d("InventoryVM", "Internet connection lost!")
                    _isOnline.value = false
                    wasOffline = true
                }
            })
        } catch (e: Exception) {
            Log.e("InventoryVM", "Failed to register network monitoring: ${e.message}")
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
        } else {
            // Default fallback
            _currentScreen.value = if (sessionManager.isLoggedIn()) AppScreen.Dashboard else AppScreen.Login
        }
    }

    fun setApiUrl(url: String) {
        sessionManager.setApiBaseUrl(url)
        _apiBaseUrl.value = sessionManager.getApiBaseUrl()
    }

    fun performLogin(email: String, motDePasse: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _loginError.value = null
            try {
                val service = ApiClient.getApiService(sessionManager)
                val credentials = mapOf("email" to email, "mot_de_passe" to motDePasse)
                
                val response = service.login(credentials)
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val user = body.user!!
                    sessionManager.loginUser(user, body.session_id)
                    _currentUser.value = user
                    navigateTo(AppScreen.Dashboard)
                } else {
                    val msg = response.body()?.message ?: "Identifiants ou mot de passe incorrects."
                    // Fallback to local demo log-in if offline / not matching
                    attemptLocalDemoLogin(email, motDePasse, msg)
                }
            } catch (e: Exception) {
                Log.e("InventoryVM", "API login offline or failed: ${e.message}")
                attemptLocalDemoLogin(email, motDePasse, "Erreur réseau: ${e.localizedMessage}. Tentative de connexion hors-ligne...")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun attemptLocalDemoLogin(email: String, motDePasse: String, networkError: String) {
        // Support offline credentials for demo & inspection convenience
        if (email.contains("admin") || email.contains("inspecteur") || (email.isNotEmpty() && motDePasse.length >= 4)) {
            val role = if (email.contains("admin")) "administrateur" else "inspecteur"
            val mockUser = User(
                id = 42,
                nom = "Dubois",
                prenom = "Alexandre",
                email = email,
                role = role
            )
            sessionManager.loginUser(mockUser, "mock_php_session_id_42")
            _currentUser.value = mockUser
            _loginError.value = null
            navigateTo(AppScreen.Dashboard)
        } else {
            _loginError.value = "$networkError Saisissez un email et mot de passe (min 4 caratères) pour la connexion de démo."
        }
    }

    fun performLogout() {
        sessionManager.logoutUser()
        _currentUser.value = null
        screenStack.clear()
        _currentScreen.value = AppScreen.Login
    }

    fun performSync() {
        viewModelScope.launch {
            _isProcessing.value = true
            _syncMessage.value = "Synchronisation en cours avec le serveur..."
            try {
                val result = repository.syncAllOfflineData()
                if (result.success) {
                    _syncMessage.value = "Synchronisation réussie! Éléments contrôles synchronisés: ${result.controlsCount}, anomalies: ${result.anomaliesCount}."
                } else {
                    _syncMessage.value = "Synchro partielle. Données sauvegardées localement. (Serveur distant injoignable)"
                }
            } catch (e: Exception) {
                _syncMessage.value = "Échec de synchronisation: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Resolves equipment scanning or manual look up
    fun handleScannedCode(code: String, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            val eq = repository.getEquipementByCode(code.trim())
            _isProcessing.value = false
            if (eq != null) {
                navigateTo(AppScreen.Detail(eq.id))
                onFinished(true)
            } else {
                onFinished(false)
            }
        }
    }

    fun handleScannedCodeContinuous(code: String, onResult: (Equipement?) -> Unit) {
        viewModelScope.launch {
            val eq = repository.getEquipementByCode(code.trim())
            if (eq != null) {
                val user = _currentUser.value ?: User(1, "Inconnu", "Inspecteur", "test@test.com", "inspecteur")
                repository.saveControle(
                    equipementId = eq.id,
                    statut = "conforme",
                    notes = "Inventorié via scan en continu",
                    signatureData = "100,100",
                    inspecteurNom = "${user.prenom} ${user.nom}",
                    inspecteurId = user.id
                )
                onResult(eq)
            } else {
                onResult(null)
            }
        }
    }

    fun saveControle(equipmentId: Int, statut: String, notes: String, signatureData: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: User(1, "Inconnu", "Inspecteur", "test@test.com", "inspecteur")
            _isProcessing.value = true
            repository.saveControle(
                equipementId = equipmentId,
                statut = statut,
                notes = notes,
                signatureData = signatureData,
                inspecteurNom = "${user.prenom} ${user.nom}",
                inspecteurId = user.id
            )
            // Clear draft when successfully saved
            sessionManager.clearControleDraft(equipmentId)
            _isProcessing.value = false
            // Navigate back to details
            navigateBack()
        }
    }

    fun saveControleDraft(equipmentId: Int, step1Ok: Boolean, step2Ok: Boolean, step3Ok: Boolean, isConforme: Boolean, notes: String, signaturePath: String) {
        sessionManager.saveControleDraft(equipmentId, step1Ok, step2Ok, step3Ok, isConforme, notes, signaturePath)
    }

    fun getControleDraft(equipmentId: Int): ControleDraft? {
        return sessionManager.getControleDraft(equipmentId)
    }

    fun clearControleDraft(equipmentId: Int) {
        sessionManager.clearControleDraft(equipmentId)
    }

    fun saveAnomalieDraft(equipmentId: Int, description: String, photoPath: String?, isCritical: Boolean) {
        viewModelScope.launch {
            repository.saveAnomalieDraft(equipmentId, description, photoPath, isCritical)
        }
    }

    suspend fun getAnomalieDraft(equipmentId: Int): AnomalieDraft? {
        return repository.getAnomalieDraft(equipmentId)
    }

    fun clearAnomalieDraft(equipmentId: Int) {
        viewModelScope.launch {
            repository.deleteAnomalieDraft(equipmentId)
        }
    }

    fun saveAnomalie(equipmentId: Int, description: String, photoData: String?, isCritical: Boolean = false) {
        viewModelScope.launch {
            _isProcessing.value = true
            val finalDesc = if (isCritical) "[CRITIQUE] $description" else description
            repository.saveAnomalie(
                equipementId = equipmentId,
                description = finalDesc,
                photoData = photoData
            )
            // Clear the draft on success
            repository.deleteAnomalieDraft(equipmentId)
            _isProcessing.value = false
            
            if (isCritical) {
                val eq = equipements.value.find { it.id == equipmentId }
                val eqCode = eq?.code_inventaire ?: "EQ-$equipmentId"
                triggerCriticalAlert(equipmentId, eqCode, description)
            }
            
            // Return back
            navigateBack()
        }
    }
}
