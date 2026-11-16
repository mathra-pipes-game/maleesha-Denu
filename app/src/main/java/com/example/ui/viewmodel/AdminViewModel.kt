package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MathraRepository
import com.example.domain.model.AuditLog
import com.example.domain.model.Complaint
import com.example.domain.model.DriverProfile
import com.example.domain.model.FareSetting
import com.example.domain.model.Trip
import com.example.domain.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: MathraRepository
) : ViewModel() {

    private val _adminToast = MutableStateFlow<String?>(null)
    val adminToast: StateFlow<String?> = _adminToast.asStateFlow()

    val allUsers: StateFlow<List<UserProfile>> = repository.observeAllUsersAdmin()
        .catch { e -> handleErr("users", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allDrivers: StateFlow<List<DriverProfile>> = repository.observeAllDriversAdmin()
        .catch { e -> handleErr("drivers", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTrips: StateFlow<List<Trip>> = repository.observeAllTripsAdmin()
        .catch { e -> handleErr("trips", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allComplaints: StateFlow<List<Complaint>> = repository.observeAllComplaintsAdmin()
        .catch { e -> handleErr("complaints", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.observeAuditLogsAdmin()
        .catch { e -> handleErr("audit_logs", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun clearToast() {
        _adminToast.value = null
    }

    fun approveDriver(driverId: String, notes: String = "Documents & vehicle verified") {
        viewModelScope.launch {
            repository.adminVerifyDriver(driverId, "APPROVED", notes)
                .onSuccess { _adminToast.value = "Driver approved." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Approval failed" }
        }
    }

    fun rejectDriver(driverId: String, notes: String = "Incomplete KYC documents") {
        viewModelScope.launch {
            repository.adminVerifyDriver(driverId, "REJECTED", notes)
                .onSuccess { _adminToast.value = "Driver rejected." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Rejection failed" }
        }
    }

    fun toggleUserSuspension(uid: String, suspend: Boolean) {
        viewModelScope.launch {
            repository.adminSuspendUser(uid, suspend)
                .onSuccess { _adminToast.value = if (suspend) "Account suspended." else "Account restored." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Update failed" }
        }
    }

    fun saveFareSetting(setting: FareSetting) {
        viewModelScope.launch {
            repository.adminSaveFareSetting(setting)
                .onSuccess { _adminToast.value = "${setting.displayNameEn} tariff saved." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Save failed" }
        }
    }

    fun seedAllDefaultTariffs() {
        viewModelScope.launch {
            FareSetting.defaults().forEach { repository.adminSaveFareSetting(it) }
            _adminToast.value = "All 7 Sri Lanka vehicle tariffs initialized in Firestore."
        }
    }

    fun resolveComplaint(complaintId: String, status: String, response: String) {
        viewModelScope.launch {
            repository.adminRespondComplaint(complaintId, status, response)
                .onSuccess { _adminToast.value = "Ticket updated ($status)." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Failed" }
        }
    }

    fun createPromotion(code: String, titleEn: String, titleSi: String, percent: Long, maxLkr: Double) {
        viewModelScope.launch {
            repository.adminCreatePromotion(code, titleEn, titleSi, percent, maxLkr)
                .onSuccess { _adminToast.value = "Promotion $code published." }
                .onFailure { _adminToast.value = it.localizedMessage ?: "Promo creation failed" }
        }
    }

    private fun handleErr(label: String, error: Throwable) {
        val fsErr = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsErr == null && error is CancellationException) throw error
        Log.w("AdminViewModel", "Admin stream [$label]: ${(fsErr ?: error).message}")
    }
}
