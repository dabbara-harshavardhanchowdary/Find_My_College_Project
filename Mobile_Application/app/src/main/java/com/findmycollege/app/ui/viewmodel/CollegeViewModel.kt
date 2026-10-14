package com.findmycollege.app.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.findmycollege.app.data.model.College
import com.findmycollege.app.data.repository.CollegeRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CollegeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: CollegeRepository = CollegeRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs = application.getSharedPreferences("find_my_college_prefs", Context.MODE_PRIVATE)

    private val _collegesList = MutableStateFlow<List<College>>(emptyList())
    val collegesList: StateFlow<List<College>> = _collegesList.asStateFlow()

    private val _filteredList = MutableStateFlow<List<College>>(emptyList())
    val filteredList: StateFlow<List<College>> = _filteredList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedState = MutableStateFlow("All States")
    val selectedState: StateFlow<String> = _selectedState.asStateFlow()

    private val _selectedStream = MutableStateFlow("All")
    val selectedStream: StateFlow<String> = _selectedStream.asStateFlow()

    private val _statesList = MutableStateFlow<List<String>>(listOf("All States"))
    val statesList: StateFlow<List<String>> = _statesList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Bookmarked College IDs set
    private val _bookmarkedCollegeIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedCollegeIds: StateFlow<Set<String>> = _bookmarkedCollegeIds.asStateFlow()

    // Bookmarked Colleges list
    private val _bookmarkedColleges = MutableStateFlow<List<College>>(emptyList())
    val bookmarkedColleges: StateFlow<List<College>> = _bookmarkedColleges.asStateFlow()

    // Dynamic comparison slots (starts with 2 blank null items, max 5)
    private val _comparisonSlots = MutableStateFlow<List<College?>>(listOf(null, null))
    val comparisonSlots: StateFlow<List<College?>> = _comparisonSlots.asStateFlow()

    // Currently selected college for Detail Dialog
    private val _selectedCollegeForDetails = MutableStateFlow<College?>(null)
    val selectedCollegeForDetails: StateFlow<College?> = _selectedCollegeForDetails.asStateFlow()

    private var firestoreBookmarksJob: Job? = null

    init {
        loadBookmarksFromPrefs()
        fetchColleges()
        initAuthAndFirestoreListener()
    }

    private fun loadBookmarksFromPrefs() {
        val saved = prefs.getStringSet("bookmarked_ids", emptySet()) ?: emptySet()
        _bookmarkedCollegeIds.value = saved
    }

    private fun saveBookmarksToPrefs(set: Set<String>) {
        prefs.edit().putStringSet("bookmarked_ids", set).apply()
    }

    private fun initAuthAndFirestoreListener() {
        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            firestoreBookmarksJob?.cancel()

            if (currentUser != null) {
                firestoreBookmarksJob = viewModelScope.launch {
                    repository.getUserBookmarksFlow(currentUser.uid).collect { firestoreSet ->
                        val localSet = _bookmarkedCollegeIds.value
                        val merged = (firestoreSet + localSet).toSet()

                        _bookmarkedCollegeIds.value = merged
                        saveBookmarksToPrefs(merged)
                        updateBookmarkedCollegesList(_collegesList.value, merged)
                    }
                }
            } else {
                loadBookmarksFromPrefs()
                updateBookmarkedCollegesList(_collegesList.value, _bookmarkedCollegeIds.value)
            }
        }
    }

    private fun extractStatesList(colleges: List<College>): List<String> {
        val uniqueStates = colleges.mapNotNull { college ->
            val stateName = college.getStateDisplay().ifBlank { college.state.trim() }
            if (stateName.isNotBlank() && stateName.toIntOrNull() == null) {
                stateName
            } else {
                null
            }
        }
        .filter { it.isNotBlank() && it.toIntOrNull() == null }
        .distinct()
        .sorted()

        return listOf("All States") + uniqueStates
    }

    private var collegesJob: Job? = null

    fun fetchColleges(stream: String = _selectedStream.value) {
        collegesJob?.cancel()
        collegesJob = viewModelScope.launch {
            _isLoading.value = true
            repository.getCollegesFlow(stream)
                .catch {
                    _isLoading.value = false
                }
                .collect { list ->
                    _collegesList.value = list
                    _statesList.value = extractStatesList(list)
                    applyFilters(_searchQuery.value, _selectedState.value, stream, list)
                    updateBookmarkedCollegesList(list, _bookmarkedCollegeIds.value)
                    _isLoading.value = false
                }
        }
    }

    suspend fun getCollegesForStreamDirect(stream: String): List<College> {
        return repository.getColleges(stream)
    }

    fun getLockedComparisonStream(): String? {
        val college1 = _comparisonSlots.value.filterNotNull().firstOrNull() ?: return null
        return if (college1.stream.isNotBlank()) college1.stream else college1.getEffectiveStream()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilters(query, _selectedState.value, _selectedStream.value, _collegesList.value)
    }

    fun onStateFilterChange(state: String) {
        _selectedState.value = state
        applyFilters(_searchQuery.value, state, _selectedStream.value, _collegesList.value)
    }

    fun onStreamFilterChange(stream: String) {
        _selectedStream.value = stream
        fetchColleges(stream)
    }

    fun selectCollegeForDetails(college: College?) {
        _selectedCollegeForDetails.value = college
    }

    private fun applyFilters(
        query: String,
        stateFilter: String,
        streamFilter: String = _selectedStream.value,
        list: List<College>
    ) {
        var result = list

        if (streamFilter.isNotBlank() && !streamFilter.equals("All", ignoreCase = true)) {
            val streamNorm = streamFilter.trim()
            result = result.filter { col ->
                col.stream.equals(streamNorm, ignoreCase = true)
            }
        }

        if (stateFilter.isNotBlank() && stateFilter != "All States") {
            val filterNormalized = stateFilter.trim()
            result = result.filter { col ->
                col.getStateDisplay().equals(filterNormalized, ignoreCase = true) ||
                col.state.trim().equals(filterNormalized, ignoreCase = true)
            }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { col ->
                col.name.lowercase().contains(q) ||
                col.city.lowercase().contains(q) ||
                col.getStateDisplay().lowercase().contains(q) ||
                col.state.lowercase().contains(q) ||
                col.getInstituteIdDisplay().lowercase().contains(q) ||
                col.getRankDisplay().lowercase().contains(q)
            }
        }

        _filteredList.value = result
    }

    // Bookmark Actions
    fun toggleBookmark(college: College): Boolean {
        val id = college.getInstituteIdDisplay()
        val current = _bookmarkedCollegeIds.value.toMutableSet()
        val isCurrentlyBookmarked = current.contains(id)

        if (isCurrentlyBookmarked) {
            current.remove(id)
        } else {
            current.add(id)
        }

        _bookmarkedCollegeIds.value = current
        saveBookmarksToPrefs(current)
        updateBookmarkedCollegesList(_collegesList.value, current)

        // Sync to Firestore if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
            repository.updateUserBookmarkInFirestore(
                userId = currentUser.uid,
                collegeId = id,
                isCurrentlyBookmarked = isCurrentlyBookmarked
            )
        }

        return !isCurrentlyBookmarked
    }

    fun isBookmarked(college: College): Boolean {
        val id = college.getInstituteIdDisplay()
        return _bookmarkedCollegeIds.value.contains(id)
    }

    private fun updateBookmarkedCollegesList(all: List<College>, ids: Set<String>) {
        _bookmarkedColleges.value = all.filter { col ->
            val id = col.getInstituteIdDisplay()
            ids.contains(id)
        }
    }

    // Comparison Actions
    private val _comparisonError = MutableStateFlow<String?>(null)
    val comparisonError: StateFlow<String?> = _comparisonError.asStateFlow()

    fun clearComparisonError() {
        _comparisonError.value = null
    }

    fun setCollegeForSlot(slotIndex: Int, college: College?): Boolean {
        if (college != null) {
            val otherSelectedColleges = _comparisonSlots.value.filterIndexed { i, c -> i != slotIndex && c != null }.mapNotNull { it }
            val college1 = otherSelectedColleges.firstOrNull()
            val college2 = college
            if (college1 != null) {
                val stream1 = college1.stream.ifBlank { college1.getEffectiveStream() }
                val stream2 = college2.stream.ifBlank { college2.getEffectiveStream() }
                if (!stream2.equals(stream1, ignoreCase = true)) {
                    val streamDisplay = if (college1.stream.isNotBlank()) college1.stream else stream1
                    val errorMsg = "Please select a college from the same stream ($streamDisplay) as your previously selected college."
                    _comparisonError.value = errorMsg
                    android.widget.Toast.makeText(getApplication(), errorMsg, android.widget.Toast.LENGTH_LONG).show()
                    return false
                }
            }
        }
        val current = _comparisonSlots.value.toMutableList()
        if (slotIndex in current.indices) {
            current[slotIndex] = college
            _comparisonSlots.value = current
            _comparisonError.value = null
            return true
        }
        return false
    }

    fun clearSlot(slotIndex: Int) {
        setCollegeForSlot(slotIndex, null)
    }

    fun addComparisonSlot() {
        val current = _comparisonSlots.value.toMutableList()
        if (current.size < 5) {
            current.add(null)
            _comparisonSlots.value = current
        }
    }

    fun removeComparisonSlot(slotIndex: Int) {
        val current = _comparisonSlots.value.toMutableList()
        if (current.size > 2 && slotIndex in current.indices) {
            current.removeAt(slotIndex)
            _comparisonSlots.value = current
        }
    }

    fun isInComparison(college: College): Boolean {
        val id = college.getInstituteIdDisplay()
        return _comparisonSlots.value.filterNotNull().any {
            it.getInstituteIdDisplay() == id
        }
    }

    fun toggleComparison(college: College): Boolean {
        val id = college.getInstituteIdDisplay()
        val currentSlots = _comparisonSlots.value.toMutableList()
        val existingIndex = currentSlots.indexOfFirst {
            it != null && (it.getInstituteIdDisplay() == id)
        }

        if (existingIndex >= 0) {
            currentSlots[existingIndex] = null
            _comparisonSlots.value = currentSlots
            _comparisonError.value = null
            return false
        } else {
            val college1 = currentSlots.filterNotNull().firstOrNull()
            val college2 = college
            if (college1 != null) {
                val stream1 = college1.stream.ifBlank { college1.getEffectiveStream() }
                val stream2 = college2.stream.ifBlank { college2.getEffectiveStream() }
                if (!stream2.equals(stream1, ignoreCase = true)) {
                    val streamDisplay = if (college1.stream.isNotBlank()) college1.stream else stream1
                    val errorMsg = "Please select a college from the same stream ($streamDisplay) as your previously selected college."
                    _comparisonError.value = errorMsg
                    android.widget.Toast.makeText(getApplication(), errorMsg, android.widget.Toast.LENGTH_LONG).show()
                    return false
                }
            }

            val nullIndex = currentSlots.indexOfFirst { it == null }
            if (nullIndex >= 0) {
                currentSlots[nullIndex] = college
                _comparisonSlots.value = currentSlots
                _comparisonError.value = null
                return true
            } else if (currentSlots.size < 5) {
                currentSlots.add(college)
                _comparisonSlots.value = currentSlots
                _comparisonError.value = null
                return true
            } else {
                val errorMsg = "Maximum 5 colleges can be compared at once. Please remove a college first."
                _comparisonError.value = errorMsg
                android.widget.Toast.makeText(getApplication(), errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                return false
            }
        }
    }
}
