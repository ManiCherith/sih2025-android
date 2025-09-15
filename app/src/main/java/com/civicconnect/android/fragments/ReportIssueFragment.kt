package com.civicconnect.android.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.civicconnect.android.databinding.FragmentReportIssueBinding
import com.civicconnect.android.models.IssueCategory
import com.civicconnect.android.models.IssuePriority
import com.civicconnect.android.viewmodels.IssueViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File

class ReportIssueFragment : Fragment() {

    private var _binding: FragmentReportIssueBinding? = null
    private val binding get() = _binding!!

    private lateinit var issueViewModel: IssueViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedCategoryValue: String? = null
    private var selectedPriorityValue: String? = null
    private var photoFile: File? = null

    private val IMAGE_PICK_CODE = 1001
    private val LOCATION_PERMISSION_REQUEST_CODE = 101

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportIssueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        issueViewModel = ViewModelProvider(requireActivity())[IssueViewModel::class.java]

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupSpinners()
        setupAddPhotoButton()
        setupUseMyLocationButton()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupUseMyLocationButton() {
        binding.btnUseCurrentLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            } else {
                fetchLocation()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val lat = it.latitude.toString()
                    val lng = it.longitude.toString()
                    binding.etLatitude.setText(lat)
                    binding.etLongitude.setText(lng)
                    Toast.makeText(requireContext(), "Location obtained", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to fetch location: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getRealPathFromUri(uri: Uri): String {
        var path = ""
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireActivity().contentResolver.query(uri, projection, null, null, null)
        cursor?.let {
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = cursor.getString(columnIndex)
            }
            cursor.close()
        }
        return path
    }

    private fun setupSpinners() {
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            IssueCategory.getAllDisplayNames()
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryValue = IssueCategory.values()[position].value
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryValue = null
            }
        }

        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            IssuePriority.getAllDisplayNames()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPriority.adapter = priorityAdapter
        binding.spinnerPriority.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPriorityValue = IssuePriority.values()[position].value
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPriorityValue = null
            }
        }
    }

    private fun setupAddPhotoButton() {
        binding.btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            if (imageUri != null) {
                binding.ivPhotoPreview.visibility = View.VISIBLE
                binding.ivPhotoPreview.setImageURI(imageUri)
                val path = getRealPathFromUri(imageUri)
                photoFile = if (path.isNotEmpty()) File(path) else null
            }
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitIssue.setOnClickListener {
            submitIssue()
        }
    }

    private fun submitIssue() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val category = selectedCategoryValue
        val priority = selectedPriorityValue

        val latStr = binding.etLatitude.text.toString().trim()
        val lngStr = binding.etLongitude.text.toString().trim()

        val coordinates = if (latStr.isNotEmpty() && lngStr.isNotEmpty()) {
            val lat = latStr.toDoubleOrNull()
            val lng = lngStr.toDoubleOrNull()
            if (lat != null && lng != null) listOf(lat, lng) else null
        } else {
            null
        }

        when {
            title.isEmpty() -> {
                binding.etTitle.error = "Title is required"
                binding.etTitle.requestFocus()
                return
            }
            description.isEmpty() -> {
                binding.etDescription.error = "Description is required"
                binding.etDescription.requestFocus()
                return
            }
            location.isEmpty() -> {
                binding.etLocation.error = "Location is required"
                binding.etLocation.requestFocus()
                return
            }
            category == null -> {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return
            }
            priority == null -> {
                Toast.makeText(requireContext(), "Please select a priority", Toast.LENGTH_SHORT).show()
                return
            }
        }

        issueViewModel.createIssue(title, category, description, location, priority, coordinates, photoFile)
    }

    private fun observeViewModel() {
        issueViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnSubmitIssue.isEnabled = !loading
        }

        issueViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        issueViewModel.createResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Issue submitted successfully!", Toast.LENGTH_LONG).show()
                    clearForm()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Failed to submit issue: ${it.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun clearForm() {
        binding.etTitle.text?.clear()
        binding.etDescription.text?.clear()
        binding.etLocation.text?.clear()
        binding.etLatitude.text?.clear()
        binding.etLongitude.text?.clear()
        binding.spinnerCategory.setSelection(0)
        binding.spinnerPriority.setSelection(0)
        binding.ivPhotoPreview.setImageURI(null)
        binding.ivPhotoPreview.visibility = View.GONE
        photoFile = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
