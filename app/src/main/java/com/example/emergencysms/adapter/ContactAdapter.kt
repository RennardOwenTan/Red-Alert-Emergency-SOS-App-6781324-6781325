package com.example.emergencysms.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.emergencysms.Contact
import com.example.emergencysms.ContactListActivity
import com.example.emergencysms.R
import com.example.emergencysms.saveContactsToFile
import java.io.File

class ContactAdapter(
    private val context: Context,
    private val contacts: MutableList<Contact>,
    private val onItemDeleted: (Int) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.contactName)
        val phone: TextView = itemView.findViewById(R.id.contactPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun getItemCount() = contacts.size

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.phone.text = contact.phone
        holder.itemView.setOnLongClickListener {
            showDeleteDialog(position)
            true
        }
    }

    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItem(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem(position: Int) {
        if (position in 0 until contacts.size) {
            contacts.removeAt(position)
            notifyItemRemoved(position)
            saveContactsToFile(context, contacts)

            if (contacts.isNotEmpty()) {
                val newPosition = if (position >= contacts.size) contacts.size - 1 else position
                (context as? ContactListActivity)?.scrollToPosition(newPosition)
            }
        }
    }
}