package com.example.student_directory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class StudentAdapter(
    private val onItemClick: (Student) -> Unit,
    private val onEditClick: (Student) -> Unit,
    private val onDeleteClick: (Student) -> Unit
) : ListAdapter<Student, StudentAdapter.StudentViewHolder>(StudentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = getItem(position)
        holder.bind(student, onItemClick, onEditClick, onDeleteClick)
    }

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)
        private val txtContact: TextView = itemView.findViewById(R.id.txtContact)
        private val txtGender: TextView = itemView.findViewById(R.id.txtGender)
        private val txtBirthplace: TextView = itemView.findViewById(R.id.txtBirthplace)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(
            student: Student,
            onItemClick: (Student) -> Unit,
            onEditClick: (Student) -> Unit,
            onDeleteClick: (Student) -> Unit
        ) {
            txtName.text = student.name
            txtEmail.text = student.email
            txtContact.text = student.contact
            txtGender.text = student.gender
            txtBirthplace.text = student.birthplace
            
            itemView.setOnClickListener { onItemClick(student) }
            btnEdit.setOnClickListener { onEditClick(student) }
            btnDelete.setOnClickListener { onDeleteClick(student) }
        }
    }

    class StudentDiffCallback : DiffUtil.ItemCallback<Student>() {
        override fun areItemsTheSame(oldItem: Student, newItem: Student): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Student, newItem: Student): Boolean {
            return oldItem == newItem
        }
    }
}
