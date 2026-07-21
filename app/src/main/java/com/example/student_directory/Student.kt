package com.example.student_directory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_table")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    val contact: String,
    val gender: String,
    val birthplace: String,
    val imageUri: String? = null
)
