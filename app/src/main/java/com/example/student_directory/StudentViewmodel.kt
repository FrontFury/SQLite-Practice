package com.example.student_directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SortOrder {
    A_Z, Z_A, NEWEST, OLDEST
}

class StudentViewModel(private val repository: StudentRepository) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _searchQuery = MutableStateFlow("")

    val allStudents = combine(repository.allStudents, _sortOrder, _searchQuery) { students, sortOrder, query ->
        val filtered = if (query.isEmpty()) {
            students
        } else {
            students.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true) 
            }
        }

        when (sortOrder) {
            SortOrder.A_Z -> filtered.sortedBy { it.name.lowercase() }
            SortOrder.Z_A -> filtered.sortedByDescending { it.name.lowercase() }
            SortOrder.NEWEST -> filtered.sortedByDescending { it.id }
            SortOrder.OLDEST -> filtered.sortedBy { it.id }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun insert(student: Student) {
        viewModelScope.launch {
            repository.insert(student)
        }
    }

    fun update(student: Student) {
        viewModelScope.launch {
            repository.update(student)
        }
    }

    fun delete(student: Student) {
        viewModelScope.launch {
            repository.delete(student)
        }
    }
}
