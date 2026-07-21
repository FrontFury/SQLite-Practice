package com.example.student_directory

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
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
        val tvTotalStudents = findViewById<TextView>(R.id.tvTotalStudents)
        val etSearch = findViewById<TextInputEditText>(R.id.etSearch)
        val btnSort = findViewById<ImageButton>(R.id.btnSort)

        val adapter = StudentAdapter(
            onItemClick = { student -> showDetailsDialog(student) },
            onEditClick = { student -> showAddEditDialog(student) },
            onDeleteClick = { student -> viewModel.delete(student) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.allStudents.collect { students ->
                adapter.submitList(students)
                tvTotalStudents.text = "Total Students: ${students.size}"
            }
        }

        etSearch.addTextChangedListener { text ->
            viewModel.setSearchQuery(text.toString())
        }

        btnSort.setOnClickListener {
            showSortDialog()
        }

        fabAdd.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun showDetailsDialog(student: Student) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_details, null)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDetailName)
        val tvGender = dialogView.findViewById<TextView>(R.id.tvDetailGender)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvDetailEmail)
        val tvContact = dialogView.findViewById<TextView>(R.id.tvDetailContact)
        val tvBirthplace = dialogView.findViewById<TextView>(R.id.tvDetailBirthplace)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        tvName.text = student.name
        tvGender.text = student.gender
        tvEmail.text = student.email
        tvContact.text = student.contact
        tvBirthplace.text = student.birthplace

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Make background transparent to see rounded corners of card
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showSortDialog() {
        val options = arrayOf("A-Z", "Z-A", "Newest", "Oldest")
        
        AlertDialog.Builder(this)
            .setTitle("Sort Students")
            .setItems(options) { _, which ->
                val order = when (which) {
                    0 -> SortOrder.A_Z
                    1 -> SortOrder.Z_A
                    2 -> SortOrder.NEWEST
                    3 -> SortOrder.OLDEST
                    else -> SortOrder.NEWEST
                }
                viewModel.setSortOrder(order)
            }
            .show()
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
