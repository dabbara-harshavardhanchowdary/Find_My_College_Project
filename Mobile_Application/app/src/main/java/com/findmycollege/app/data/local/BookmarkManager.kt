package com.findmycollege.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BookmarkManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("find_my_college_prefs", Context.MODE_PRIVATE)
    private val key = "bookmarked_college_ids"
    private val _bookmarksFlow = MutableStateFlow<Set<String>>(getSavedBookmarks())
    val bookmarksFlow: StateFlow<Set<String>> = _bookmarksFlow.asStateFlow()

    private fun getSavedBookmarks(): Set<String> {
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    fun isBookmarked(collegeId: String): Boolean {
        return _bookmarksFlow.value.contains(collegeId)
    }

    fun toggleBookmark(collegeId: String): Boolean {
        val current = _bookmarksFlow.value.toMutableSet()
        val willBeBookmarked = if (current.contains(collegeId)) {
            current.remove(collegeId)
            false
        } else {
            current.add(collegeId)
            true
        }
        prefs.edit().putStringSet(key, current).apply()
        _bookmarksFlow.value = current
        return willBeBookmarked
    }
}
