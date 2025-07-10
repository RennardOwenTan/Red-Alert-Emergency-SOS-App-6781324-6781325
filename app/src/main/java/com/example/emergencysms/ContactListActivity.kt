package com.example.emergencysms

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.emergencysms.adapter.ContactAdapter
import java.io.File

class ContactListActivity : AppCompatActivity() {

    private lateinit var contactList: MutableList<Contact>
    private lateinit var adapter: ContactAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_list)

        recyclerView = findViewById(R.id.contactRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        contactList = loadContactsFromFile(this).toMutableList()
        adapter = ContactAdapter(this, contactList) { position ->
            recyclerView.smoothScrollToPosition(Math.min(position, adapter.itemCount - 1))
        }
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnAddContact).setOnClickListener {
            showAddContactDialog()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun showAddContactDialog() {
        val inputName = EditText(this).apply {
            hint = "Name"
        }
        val inputPhone = EditText(this).apply {
            hint = "Phone"
            inputType = InputType.TYPE_CLASS_PHONE
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
            addView(inputName)
            addView(inputPhone)
        }

        AlertDialog.Builder(this)
            .setTitle("Add Contact")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = inputName.text.toString().trim()
                val phone = inputPhone.text.toString().trim()

                if (name.isNotBlank() && phone.isNotBlank()) {
                    val contact = Contact(name, phone)
                    contactList.add(contact)
                    saveContactsToFile(this@ContactListActivity, contactList)
                    adapter.notifyItemInserted(contactList.size - 1)
                    recyclerView.smoothScrollToPosition(contactList.size - 1)
                } else {
                    Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun scrollToPosition(position: Int) {
        if (position in 0 until adapter.itemCount - 1) {
            recyclerView.smoothScrollToPosition(position)
        }
    }
}