package com.example.emergencysms

import android.content.Context
import java.io.File

fun loadContactsFromFile(context: Context): List<Contact> {
    val file = File(context.filesDir, "contacts.txt")
    if (!file.exists()) return emptyList()

    return file.readLines().mapNotNull { line ->
        val parts = line.split(",")
        if (parts.size == 2) Contact(parts[0], parts[1]) else null
    }
}

fun saveContactsToFile(context: Context, contacts: List<Contact>) {
    val file = File(context.filesDir, "contacts.txt")
    file.writeText("")
    contacts.forEach { contact ->
        file.appendText("${contact.name},${contact.phone}\n")
    }
}
