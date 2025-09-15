package com.civicconnect.android.repository

import com.civicconnect.android.models.Issue
import com.civicconnect.android.network.NetworkManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File

class IssueRepository {

    private val api = NetworkManager.apiService

    suspend fun getMyIssues(): Response<List<Issue>> {
        return api.getIssues()
    }

    suspend fun createIssue(
        title: String,
        category: String,
        description: String,
        location: String,
        priority: String,
        coordinates: List<Double>?,
        photoFile: File?
    ): Response<Issue> {
        val textMediaType = "text/plain".toMediaType()

        val titleBody = title.toRequestBody(textMediaType)
        val categoryBody = category.lowercase().toRequestBody(textMediaType)
        val descriptionBody = description.toRequestBody(textMediaType)
        val locationBody = location.toRequestBody(textMediaType)
        val priorityBody = priority.lowercase().toRequestBody(textMediaType)

        val coordsJson = coordinates?.let { "[${it[0]},${it[1]}]" } ?: "null"
        val coordinatesBody = coordsJson.toRequestBody("application/json".toMediaType())

        val photoPart = photoFile?.let {
            val requestFile = it.asRequestBody("image/*".toMediaType())
            MultipartBody.Part.createFormData("photo", it.name, requestFile)
        }

        return api.createIssue(
            titleBody,
            categoryBody,
            descriptionBody,
            locationBody,
            priorityBody,
            coordinatesBody,
            photoPart
        )
    }
}
