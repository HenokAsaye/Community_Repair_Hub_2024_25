package com.example.community_repair_hub.ViewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.community_repair_hub.Utills.TokenManager
import com.example.community_repair_hub.data.network.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "SignupViewModel"

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val selectedRole: String = "Citizen",
    val selectedRegion: String = "",
    val selectedCity: String = "",
    val isRegionDropdownExpanded: Boolean = false,
    val isCityDropdownExpanded: Boolean = false,
    val regions: List<String> = emptyList(),
    val cities: List<String> = emptyList(),
    val regionCityMap: Map<String, List<String>> = emptyMap(),
    val isLoadingRegions: Boolean = false,
    val signupInProgress: Boolean = false,
    val signupError: String? = null,
    val imageUri: Uri? = null,
    val signupSuccess: Boolean = false
)

class SignupViewModel(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    init {
        loadRegionsAndCities()
    }

    fun loadRegionsAndCities() {
        val regionCityMap = mapOf(
            "Addis Ababa" to listOf("Addis Ababa"),
            "Oromia" to listOf("Adama", "Dire Dawa", "Jimma", "Shashemene"),
            "Amhara" to listOf("Bahir Dar", "Gondar", "Dessie", "Debre Markos"),
            "Tigray" to listOf("Mekelle", "Shire", "Axum", "Adigrat"),
            "Sidama" to listOf("Hawassa"),
            "Somali" to listOf("Jigjiga", "Degehabur", "Gode"),
            "Benishangul-Gumuz" to listOf("Assosa", "Metekel", "Kamashi"),
            "Gambella" to listOf("Gambella", "Abobo", "Itang"),
            "Afar" to listOf("Semera", "Dubti", "Logiya"),
            "Southern Nations, Nationalities, and Peoples' Region (SNNPR)" to listOf("Arba Minch", "Jinka", "Wolayta Sodo")
        )

        _uiState.update {
            it.copy(
                regions = regionCityMap.keys.toList(),
                regionCityMap = regionCityMap,
                isLoadingRegions = false
            )
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update {
            it.copy(
                name = newName,
                signupError = null
            )
        }
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, signupError = null) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, signupError = null) }
    }

    fun onRoleSelected(role: String) {
        _uiState.update { it.copy(selectedRole = role, signupError = null) }
    }

    fun onRegionSelected(region: String) {
        _uiState.update {
            it.copy(
                selectedRegion = region,
                selectedCity = "",
                cities = it.regionCityMap[region] ?: emptyList(),
                isRegionDropdownExpanded = false,
                signupError = null
            )
        }
    }

    fun onCitySelected(city: String) {
        _uiState.update {
            it.copy(
                selectedCity = city,
                isCityDropdownExpanded = false,
                signupError = null
            )
        }
    }

    fun toggleRegionDropdown(expanded: Boolean? = null) {
        val newState = expanded ?: !_uiState.value.isRegionDropdownExpanded
        _uiState.update { it.copy(isRegionDropdownExpanded = newState) }
    }

    fun toggleCityDropdown(expanded: Boolean? = null) {
        val newState = expanded ?: !_uiState.value.isCityDropdownExpanded
        _uiState.update { it.copy(isCityDropdownExpanded = newState) }
    }

    fun onImagePicked(uri: Uri) {
        _uiState.update { it.copy(imageUri = uri, signupError = null) }
    }

    fun signup(context: Context) {
        val currentState = _uiState.value
        Log.d(TAG, "Starting signup process for email: ${currentState.email}")

        if (currentState.name.isBlank() ||
            currentState.email.isBlank() ||
            currentState.password.isBlank() ||
            currentState.selectedRegion.isBlank() ||
            currentState.selectedCity.isBlank()
        ) {
            Log.d(TAG, "Validation failed: Empty fields")
            _uiState.update { it.copy(signupError = "Please fill in all fields") }
            return
        }

        if (!currentState.email.contains("@")) {
            Log.d(TAG, "Validation failed: Invalid email format")
            _uiState.update { it.copy(signupError = "Invalid email format") }
            return
        }

        if (currentState.password.length < 6) {
            Log.d(TAG, "Validation failed: Password too short")
            _uiState.update { it.copy(signupError = "Password must be at least 6 characters") }
            return
        }

        val validCities = currentState.regionCityMap[currentState.selectedRegion] ?: emptyList()
        if (!validCities.contains(currentState.selectedCity)) {
            _uiState.update { it.copy(signupError = "Invalid city for selected region") }
            return
        }

        _uiState.update {
            it.copy(
                signupInProgress = true,
                signupError = null,
                signupSuccess = false
            )
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Calling authRepository.signup()")
                when (val result = authRepository.signup(
                    name = currentState.name,
                    email = currentState.email,
                    password = currentState.password,
                    role = currentState.selectedRole,
                    region = currentState.selectedRegion,
                    city = currentState.selectedCity,
                    imageUri = currentState.imageUri,
                    context = context
                )) {
                    is AuthRepository.AuthResult.Success -> {
                        Log.d(TAG, "Signup successful")
                        result.data.token?.let { token ->
                            Log.d(TAG, "Saving token")
                            tokenManager.saveToken(token)
                        }
                        _uiState.update {
                            it.copy(
                                signupInProgress = false,
                                signupSuccess = true
                            )
                        }
                    }

                    is AuthRepository.AuthResult.Error -> {
                        Log.e(TAG, "Signup Failed: ${result.exception.message}", result.exception)
                        _uiState.update {
                            it.copy(
                                signupInProgress = false,
                                signupError = result.exception.message ?: "Signup failed"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during signup", e)
                _uiState.update {
                    it.copy(
                        signupInProgress = false,
                        signupError = "An unexpected error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetSignupStatus() {
        _uiState.update {
            it.copy(
                signupSuccess = false,
                signupError = null
            )
        }
    }

    fun resetForm() {
        _uiState.value = SignupUiState()
    }
}