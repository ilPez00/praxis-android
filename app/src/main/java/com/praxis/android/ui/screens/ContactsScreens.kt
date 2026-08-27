package com.praxis.android.ui.screens

import com.praxis.android.ui.components.design.PraxisButton
import com.praxis.android.ui.components.design.PraxisCard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Invite people from the phone's address book.
 *
 * Praxis is an accountability game; its network only grows by pulling real
 * people in. This reads names + numbers locally (READ_CONTACTS, runtime
 * permission) purely to compose an invite — nothing is uploaded.
 */
object ContactsScreens {

    private data class Contact(val id: Long, val name: String, val phone: String?)

    @Composable
    fun ContactsScreen(onBack: () -> Unit) {
        val context = LocalContext.current
        var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
        var granted by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        fun load() {
            try {
                val out = mutableListOf<Contact>()
                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ),
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )
                cursor?.use { c ->
                    while (c.moveToNext() && out.size < 500) {
                        val id = c.getLong(0)
                        val name = c.getString(1) ?: continue
                        val phone = c.getString(2)
                        out.add(Contact(id, name, phone))
                    }
                }
                // One row per (contact, number); collapse to the first number.
                contacts = out.distinctBy { it.id }
                error = if (contacts.isEmpty()) "No contacts with phone numbers found." else null
            } catch (e: SecurityException) {
                error = "Permission revoked."
            } catch (e: Exception) {
                error = e.message ?: "Could not read contacts."
            }
        }

        val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            if (ok) load() else error = "Contacts permission is needed to pick who to invite."
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Invite friends", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Praxis works better with a sparring partner. Pick a contact and send them your invite link.",
                style = MaterialTheme.typography.bodySmall
            )

            if (!granted && contacts.isEmpty()) {
                PraxisButton(onClick = { permission.launch(android.Manifest.permission.READ_CONTACTS) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow contacts access")
                }
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(contacts) { contact ->
                    PraxisCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(contact.name, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val link = "https://praxisweb.xyz/praxis"
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:${Uri.encode(contact.phone ?: "")}")
                                            putExtra("sms_body", "I'm tracking my goals on Praxis — join me: $link")
                                        }
                                        runCatching { context.startActivity(intent) }
                                    },
                                    enabled = !contact.phone.isNullOrBlank()
                                ) { Text("SMS") }
                                OutlinedButton(onClick = {
                                    val link = "https://praxisweb.xyz/praxis"
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "I'm tracking my goals on Praxis — join me: $link")
                                    }
                                    context.startActivity(Intent.createChooser(send, "Invite ${contact.name}"))
                                }) { Text("Share") }
                            }
                        }
                    }
                }
            }

            PraxisButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}
