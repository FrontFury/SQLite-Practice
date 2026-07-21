package com.example.student_directory

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import coil.load
import coil.transform.CircleCropTransformation
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private var ivPreview: ImageView? = null
    private var activeVoiceEditText: TextInputEditText? = null

    private val speechRecognizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                activeVoiceEditText?.setText(results[0])
                // Move cursor to the end
                activeVoiceEditText?.setSelection(activeVoiceEditText?.text?.length ?: 0)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val internalUri = copyUriToInternalStorage(it)
            selectedImageUri = internalUri
            if (internalUri != null && internalUri.scheme == "file") {
                ivPreview?.load(File(internalUri.path!!)) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            } else {
                ivPreview?.load(internalUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                }
            }
            ivPreview?.imageTintList = null
        }
    }

    private fun copyUriToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "student_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
            onDeleteClick = { student -> showDeleteConfirmation(student) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Swipe to Delete
        val deleteIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_delete)
        val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
        val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
        val background = ColorDrawable()
        val backgroundColor = Color.parseColor("#f44336")

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val student = adapter.currentList[position]

                val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_confirm_delete, null)
                val tvMessage = dialogView.findViewById<TextView>(R.id.tvConfirmMessage)
                val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelDelete)
                val btnDelete = dialogView.findViewById<Button>(R.id.btnConfirmDelete)

                tvMessage.text = "Are you sure you want to delete ${student.name}?"

                val dialog = AlertDialog.Builder(this@MainActivity)
                    .setView(dialogView)
                    .create()
                
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                btnCancel.setOnClickListener {
                    adapter.notifyItemChanged(position)
                    dialog.dismiss()
                }

                btnDelete.setOnClickListener {
                    viewModel.delete(student)
                    Snackbar.make(recyclerView, "${student.name} deleted", Snackbar.LENGTH_LONG)
                        .setAnchorView(fabAdd)
                        .setAction("Undo") {
                            viewModel.insert(student)
                        }.show()
                    dialog.dismiss()
                }

                dialog.setOnCancelListener {
                    adapter.notifyItemChanged(position)
                }

                dialog.show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                // Draw the red delete background
                background.color = backgroundColor
                if (dX > 0) { // Swiping to the right
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                } else { // Swiping to the left
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                }
                background.draw(c)

                // Calculate position of delete icon
                val deleteIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                val deleteIconMargin = (itemHeight - intrinsicHeight) / 2
                val deleteIconLeft = if (dX > 0) itemView.left + deleteIconMargin else itemView.right - deleteIconMargin - intrinsicWidth
                val deleteIconRight = if (dX > 0) itemView.left + deleteIconMargin + intrinsicWidth else itemView.right - deleteIconMargin
                val deleteIconBottom = deleteIconTop + intrinsicHeight

                // Draw the delete icon
                if (dX != 0f) {
                    deleteIcon?.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                    deleteIcon?.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(recyclerView)

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

    private fun showDeleteConfirmation(student: Student) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_delete, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvConfirmMessage)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelDelete)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnConfirmDelete)

        tvMessage.text = "Are you sure you want to delete ${student.name}?"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            viewModel.delete(student)
            Snackbar.make(recyclerView, "${student.name} deleted", Snackbar.LENGTH_LONG)
                .setAnchorView(fabAdd)
                .setAction("Undo") {
                    viewModel.insert(student)
                }.show()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startVoiceInput(targetEditText: TextInputEditText) {
        activeVoiceEditText = targetEditText
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDetailsDialog(student: Student) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_student_details, null)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDetailName)
        val tvGender = dialogView.findViewById<TextView>(R.id.tvDetailGender)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvDetailEmail)
        val tvContact = dialogView.findViewById<TextView>(R.id.tvDetailContact)
        val tvBirthplace = dialogView.findViewById<TextView>(R.id.tvDetailBirthplace)
        val ivDetailImage = dialogView.findViewById<ImageView>(R.id.ivDetailImage)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        tvName.text = student.name
        tvGender.text = student.gender
        tvEmail.text = student.email
        tvContact.text = student.contact
        tvBirthplace.text = student.birthplace

        if (student.imageUri != null) {
            try {
                val uri = Uri.parse(student.imageUri)
                if (uri.scheme == "file") {
                    ivDetailImage.load(File(uri.path!!))
                } else {
                    ivDetailImage.load(uri)
                }
                ivDetailImage.imageTintList = null
            } catch (e: Exception) {
                ivDetailImage.setImageResource(R.drawable.ic_menu_compass)
            }
        } else {
            ivDetailImage.setImageResource(R.drawable.ic_menu_compass)
        }

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
        val tilName = dialogView.findViewById<TextInputLayout>(R.id.tilName)
        val tilEmail = dialogView.findViewById<TextInputLayout>(R.id.tilEmail)
        val tilContact = dialogView.findViewById<TextInputLayout>(R.id.tilContact)
        val spinnerGender = dialogView.findViewById<Spinner>(R.id.spinnerGender)
        val spinnerBirthplace = dialogView.findViewById<Spinner>(R.id.spinnerBirthplace)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        ivPreview = dialogView.findViewById(R.id.ivStudentImage)
        val btnPickImage = dialogView.findViewById<FloatingActionButton>(R.id.btnPickImage)

        // Setup Voice Input
        tilName.setEndIconOnClickListener { startVoiceInput(etName) }
        tilEmail.setEndIconOnClickListener { startVoiceInput(etEmail) }
        tilContact.setEndIconOnClickListener { startVoiceInput(etContact) }

        // Setup Image Picking
        btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

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

        // Make background transparent to see rounded corners and glass effect
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        if (student != null) {
            txtTitle.text = "Edit Student"
            etName.setText(student.name)
            etEmail.setText(student.email)
            etContact.setText(student.contact)
            
            if (student.imageUri != null) {
                try {
                    val uri = Uri.parse(student.imageUri)
                    selectedImageUri = uri
                    if (uri.scheme == "file") {
                        ivPreview?.load(File(uri.path!!))
                    } else {
                        ivPreview?.load(uri)
                    }
                    ivPreview?.imageTintList = null
                } catch (e: Exception) {
                    selectedImageUri = null
                    ivPreview?.setImageResource(R.drawable.ic_menu_compass)
                }
            } else {
                selectedImageUri = null
            }

            // Set spinner selections
            val genderPos = genderAdapter.getPosition(student.gender)
            if (genderPos >= 0) spinnerGender.setSelection(genderPos)
            
            val countryPos = countryAdapter.getPosition(student.birthplace)
            if (countryPos >= 0) spinnerBirthplace.setSelection(countryPos)
        } else {
            txtTitle.text = "Add Student"
            selectedImageUri = null
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val contact = etContact.text.toString().trim()
            val gender = spinnerGender.selectedItem.toString()
            val birthplace = spinnerBirthplace.selectedItem.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && contact.isNotEmpty()) {
                val imageStr = selectedImageUri?.toString()
                if (student != null) {
                    viewModel.update(
                        student.copy(
                            name = name,
                            email = email,
                            contact = contact,
                            gender = gender,
                            birthplace = birthplace,
                            imageUri = imageStr
                        )
                    )
                } else {
                    viewModel.insert(
                        Student(
                            name = name,
                            email = email,
                            contact = contact,
                            gender = gender,
                            birthplace = birthplace,
                            imageUri = imageStr
                        )
                    )
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
