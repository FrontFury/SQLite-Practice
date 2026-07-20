package com.example.student_directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class StudentViewModel(private val repository: StudentRepository) : ViewModel() {

    val allStudents: Flow<List<Student>> = repository.allStudents

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
