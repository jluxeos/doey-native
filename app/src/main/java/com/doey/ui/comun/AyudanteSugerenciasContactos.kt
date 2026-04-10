package com.doey.ui.comun

import android.content.Context
import android.provider.ContactsContract
import com.doey.AplicacionDoey

// ── Datos de contacto ─────────────────────────────────────────────────────────

data class ContactSuggestion(
    val name: String,
    val phone: String? = null,
    val email: String? = null
)

data class LocationSuggestion(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

// ── Helper para obtener sugerencias ───────────────────────────────────────────

object ContactsSuggestionsHelper {
    
    /**
     * Obtiene lista de contactos del teléfono.
     * Requiere permiso READ_CONTACTS.
     */
    fun getContactsSuggestions(): List<ContactSuggestion> {
        val ctx = AplicacionDoey.instance
        val contacts = mutableListOf<ContactSuggestion>()
        
        try {
            val cursor = ctx.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null, null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    val hasPhone = c.getInt(c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
                    
                    var phone: String? = null
                    if (hasPhone) {
                        val phoneCursor = ctx.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                phone = pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            }
                        }
                    }
                    
                    contacts.add(ContactSuggestion(name = name, phone = phone))
                }
            }
        } catch (e: Exception) {
            // Sin permiso o error al leer contactos
        }
        
        return contacts
    }
    
    /**
     * Filtra contactos por nombre (búsqueda mientras escribes).
     */
    fun filterContacts(query: String, allContacts: List<ContactSuggestion>): List<ContactSuggestion> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return allContacts.filter { it.name.lowercase().contains(q) }.take(10)
    }
    
    /**
     * Obtiene lista de lugares frecuentes (simulado con ubicaciones guardadas).
     * En una app real, esto vendría de historial de ubicaciones o Google Places.
     */
    fun getLocationsSuggestions(): List<LocationSuggestion> {
        // Lugares predefinidos comunes
        return listOf(
            LocationSuggestion("Casa", "Tu dirección de casa"),
            LocationSuggestion("Trabajo", "Tu dirección de trabajo"),
            LocationSuggestion("Supermercado", "Tienda de comestibles"),
            LocationSuggestion("Farmacia", "Farmacia cercana"),
            LocationSuggestion("Banco", "Sucursal bancaria"),
            LocationSuggestion("Escuela", "Institución educativa"),
            LocationSuggestion("Hospital", "Centro de salud"),
            LocationSuggestion("Restaurante", "Lugar para comer"),
            LocationSuggestion("Gimnasio", "Centro deportivo"),
            LocationSuggestion("Cine", "Sala de cine")
        )
    }
    
    /**
     * Filtra lugares por nombre (búsqueda mientras escribes).
     */
    fun filterLocations(query: String, allLocations: List<LocationSuggestion>): List<LocationSuggestion> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return allLocations.filter { it.name.lowercase().contains(q) }.take(10)
    }
    
    /**
     * Obtiene sugerencias de apps instaladas.
     */
    fun getAppsSuggestions(): List<String> {
        val ctx = AplicacionDoey.instance
        val apps = mutableListOf<String>()
        
        try {
            val pm = ctx.packageManager
            val packages = pm.getInstalledPackages(0)
            packages.forEach { pkg ->
                val appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg.packageName, 0)).toString()
                if (!appName.startsWith("com.android") && !appName.startsWith("android")) {
                    apps.add(appName)
                }
            }
        } catch (e: Exception) {
            // Error al obtener apps
        }
        
        return apps.distinct().sorted().take(20)
    }
    
    /**
     * Filtra apps por nombre.
     */
    fun filterApps(query: String, allApps: List<String>): List<String> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return allApps.filter { it.lowercase().contains(q) }.take(10)
    }
}
