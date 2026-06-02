package com.raymi.app.presentation.settings

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.raymi.app.core.workspace.WorkspaceManager
import com.raymi.app.data.remote.StorageDataSource
import com.raymi.app.domain.model.Resource
import com.raymi.app.domain.model.Workspace
import com.raymi.app.domain.usecase.workspace.UpdateWorkspaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.raymi.app.core.utils.ConnectivityObserver

@HiltViewModel
class BusinessSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workspaceManager: WorkspaceManager,
    private val updateWorkspaceUseCase: UpdateWorkspaceUseCase,
    private val storageDataSource: StorageDataSource,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSettingsUiState())
    val uiState: StateFlow<BusinessSettingsUiState> = _uiState.asStateFlow()

    // ✅ FIX PROBLEM 7: Evitar fugas de memoria del GPS
    private var nativeLocationManager: LocationManager? = null
    private var nativeLocationListener: LocationListener? = null

    init {
        // ... (rest of init)
        // Observar conexión a internet
        connectivityObserver.isConnected
            .onEach { connected ->
                if (connected) dismissNoInternetAlert()
            }
            .launchIn(viewModelScope)

        workspaceManager.currentWorkspace
            .filterNotNull()
            .onEach { workspace ->
                _uiState.update { it.copy(
                    nombre = workspace.nombre,
                    nombreComercial = workspace.nombreComercial,
                    ruc = workspace.ruc,
                    direccion = workspace.direccion,
                    telefono = workspace.telefono,
                    descripcion = workspace.descripcion,
                    logoUrl = workspace.logoUrl,
                    moneda = workspace.moneda,
                    terminosCondiciones = workspace.terminosCondiciones,
                    politicaPenalidades = workspace.politicaPenalidades,
                    googleMapsUrl = workspace.googleMapsUrl
                ) }
            }
            .launchIn(viewModelScope)
    }

    fun onNombreChange(v: String) = _uiState.update { it.copy(nombre = v) }
    fun onNombreComercialChange(v: String) = _uiState.update { it.copy(nombreComercial = v) }
    fun onRucChange(v: String) = _uiState.update { it.copy(ruc = v) }
    fun onDireccionChange(v: String) = _uiState.update { it.copy(direccion = v) }
    fun onGoogleMapsUrlChange(v: String) = _uiState.update { it.copy(googleMapsUrl = v) }
    
    fun onTelefonoChange(v: String) {
        if (v.length <= 9 && v.all { it.isDigit() }) {
            _uiState.update { it.copy(telefono = v) }
        }
    }
    
    fun onDescripcionChange(v: String) = _uiState.update { it.copy(descripcion = v) }
    fun onMonedaChange(v: String) = _uiState.update { it.copy(moneda = v) }
    
    // ✅ FEATURE 3 FIX: Handlers for legal policies
    fun onTerminosChange(v: String) = _uiState.update { it.copy(terminosCondiciones = v) }
    fun onPoliticaChange(v: String) = _uiState.update { it.copy(politicaPenalidades = v) }

    /**
     * Captura la ubicación actual con estrategia de respaldo (Fallback).
     */
    fun captureLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        // Si el hardware está apagado, mostramos el diálogo de alerta
        if (!isGpsEnabled && !isNetworkEnabled) {
            _uiState.update { it.copy(showGpsDisabledAlert = true, isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, showGpsDisabledAlert = false) }

        // Estrategia 1: Google Fused Location
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        applyLocation(location)
                    } else {
                        // Estrategia 2: Fallback a Native GPS (Si Fused falla o devuelve null)
                        captureNativeLocation(locationManager)
                    }
                }
                .addOnFailureListener {
                    captureNativeLocation(locationManager)
                }
        } catch (_: SecurityException) {
            _uiState.update { it.copy(error = "Sin permisos de ubicación", isLoading = false) }
        } catch (e: Exception) {
            captureNativeLocation(locationManager)
        }
    }

    private fun captureNativeLocation(manager: LocationManager) {
        try {
            val provider = if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
                LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            
            nativeLocationManager = manager
            nativeLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    applyLocation(location)
                    removeGpsUpdates()
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }

            manager.requestLocationUpdates(provider, 0L, 0f, nativeLocationListener!!)
            
            viewModelScope.launch {
                delay(12000)
                if (_uiState.value.isLoading) {
                    removeGpsUpdates()
                    _uiState.update { it.copy(isLoading = false, error = "El GPS está tardando demasiado. Asegúrate de estar en un lugar despejado.") }
                }
            }
        } catch (_: SecurityException) {
            _uiState.update { it.copy(isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Error técnico al acceder al GPS nativo.") }
        }
    }

    private fun removeGpsUpdates() {
        nativeLocationListener?.let { nativeLocationManager?.removeUpdates(it) }
        nativeLocationListener = null
    }

    override fun onCleared() {
        super.onCleared()
        removeGpsUpdates()
    }

    private fun applyLocation(location: Location) {
        val url = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
        onGoogleMapsUrlChange(url)
        _uiState.update { it.copy(isLoading = false, error = null) }
    }

    fun dismissGpsAlert() {
        _uiState.update { it.copy(showGpsDisabledAlert = false) }
    }

    fun dismissNoInternetAlert() {
        _uiState.update { it.copy(showNoInternetAlert = false) }
    }

    fun subirLogo(uri: Uri) {
        if (!connectivityObserver.isConnected.value) {
            _uiState.update { it.copy(showNoInternetAlert = true) }
            return
        }
        val workspaceId = workspaceManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                _uiState.value.logoUrl?.let { oldUrl ->
                    storageDataSource.getPathFromUrl(oldUrl)?.let { path ->
                        storageDataSource.deleteFile(path)
                    }
                }
                val url = storageDataSource.uploadFile("negocios/$workspaceId/logo.webp", uri)
                _uiState.update { it.copy(logoUrl = url, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al subir logo: ${e.message}", isLoading = false) }
            }
        }
    }

    fun guardarCambios() {
        if (!connectivityObserver.isConnected.value) {
            _uiState.update { it.copy(showNoInternetAlert = true) }
            return
        }
        val current = workspaceManager.currentWorkspace.value ?: return
        
        if (_uiState.value.telefono.length != 9) {
            _uiState.update { it.copy(error = "El teléfono debe tener exactamente 9 dígitos") }
            return
        }

        val updated = current.copy(
            nombre = _uiState.value.nombre,
            nombreComercial = _uiState.value.nombreComercial,
            ruc = _uiState.value.ruc,
            direccion = _uiState.value.direccion,
            telefono = _uiState.value.telefono,
            descripcion = _uiState.value.descripcion,
            logoUrl = _uiState.value.logoUrl,
            moneda = _uiState.value.moneda,
            googleMapsUrl = _uiState.value.googleMapsUrl,
            // ✅ FEATURE 3 FIX: Sincronizar políticas legales al guardar
            terminosCondiciones = _uiState.value.terminosCondiciones,
            politicaPenalidades = _uiState.value.politicaPenalidades
        )

        viewModelScope.launch {
            updateWorkspaceUseCase(updated).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        workspaceManager.setWorkspace(updated)
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, isSuccess = false) }
    }
}

data class BusinessSettingsUiState(
    val nombre: String = "",
    val nombreComercial: String = "",
    val ruc: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val descripcion: String = "",
    val logoUrl: String? = null,
    val moneda: String = "PEN",
    val terminosCondiciones: String = "",
    val politicaPenalidades: String = "",
    val googleMapsUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val showGpsDisabledAlert: Boolean = false,
    val showNoInternetAlert: Boolean = false
)
