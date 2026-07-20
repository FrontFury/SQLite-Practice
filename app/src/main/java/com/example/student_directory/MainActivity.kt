package com.example.student_directory

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: StudentViewModel by viewModels {
        val database = StudentDatabase.getDatabase(this)
        val repository = StudentRepository(database.studentDao())
        StudentViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        val adapter = StudentAdapter(
            onEditClick = { student -> showAddEditDialog(student) },
            onDeleteClick = { student -> viewModel.delete(student) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.allStudents.collect { students ->
                adapter.submitList(students)
            }
        }

        fabAdd.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun showAddEditDialog(student: Student?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_add_edit, null)
        val txtTitle = dialogView.findViewById<TextView>(R.id.txtTitle)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.etEmail)
        val etContact = dialogView.findViewById<TextInputEditText>(R.id.etContact)
        val spinnerGender = dialogView.findViewById<Spinner>(R.id.spinnerGender)
        val spinnerBirthplace = dialogView.findViewById<Spinner>(R.id.spinnerBirthplace)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Setup Spinners
        val genderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.gender_array,
            android.R.layout.simple_spinner_item
        )
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        val countryAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.countries_array,
            android.R.layout.simple_spinner_item
        )
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBirthplace.adapter = countryAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        if (student != null) {
            txtTitle.text = "Edit Student"
            etName.setText(student.name)
            etEmail.setText(student.email)
            etContact.setText(student.contact)
            
            // Set spinner selections
            val genderPos = genderAdapter.getPosition(student.gender)
            if (genderPos >= 0) spinnerGender.setSelection(genderPos)
            
            val countryPos = countryAdapter.getPosition(student.birthplace)
            if (countryPos >= 0) spinnerBirthplace.setSelection(countryPos)
        } else {
            txtTitle.text = "Add Student"
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val contact = etContact.text.toString().trim()
            val gender = spinnerGender.selectedItem.toString()
            val birthplace = spinnerBirthplace.selectedItem.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && contact.isNotEmpty()) {
                if (student != null) {
                    viewModel.update(
                        student.copy(
                            name = name,
                            email = email,
                            contact = contact,
                            gender = gender,
                            birthplace = birthplace
                        )
                    )
                } else {
                    viewModel.insert(
                        Student(
                            name = name,
                            email = email,
                            contact = contact,
                            gender = gender,
                            birthplace = birthplace
                        )
                    )
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
