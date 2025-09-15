package com.civicconnect.android.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.civicconnect.android.models.Issue
import com.civicconnect.android.network.NetworkManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class IssueViewModel : ViewModel() {

    private val apiService = NetworkManager.apiService

    private val _issues = MutableLiveData<List<Issue>>()
    val issues: LiveData<List<Issue>> = _issues

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Added LiveData for create result to notify UI on issue creation
    private val _createResult = MutableLiveData<Result<Issue>>()
    val createResult: LiveData<Result<Issue>> get() = _createResult

    fun loadIssues() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getIssues()
                if (response.isSuccessful) {
                    _issues.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Failed to load issues: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createIssue(
        title: String,
        category: String,
        description: String,
        location: String,
        priority: String,
        coordinates: List<Double>?,
        photoFile: File?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val locationBody = location.toRequestBody("text/plain".toMediaTypeOrNull())
                val priorityBody = priority.toRequestBody("text/plain".toMediaTypeOrNull())
                val coordsJson = coordinates?.let { "[${it[0]},${it[1]}]" } ?: "null"
                val coordinatesBody = coordsJson.toRequestBody("application/json".toMediaTypeOrNull())

                val photoPart = photoFile?.let { file ->
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("photo", file.name, requestFile)
                }

                val response = apiService.createIssue(
                    titleBody, categoryBody, descriptionBody,
                    locationBody, priorityBody, coordinatesBody, photoPart
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        _createResult.value = Result.success(it)
                    } ?: run {
                        _createResult.value = Result.failure(Exception("Empty response body"))
                    }
                    // Refresh the issues list after successful creation
                    loadIssues()
                } else {
                    _createResult.value = Result.failure(Exception("Failed to create issue: ${response.message()}"))
                }
            } catch (e: Exception) {
                _createResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
