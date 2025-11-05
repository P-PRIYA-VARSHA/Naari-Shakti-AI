package com.example.secureshe.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.secureshe.data.dao.EmergencyContactDao
import com.example.secureshe.data.entity.EmergencyContactEntity
import com.example.secureshe.data.EmergencyContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactsRepository @Inject constructor(
    private val emergencyContactDao: EmergencyContactDao,
    private val context: Context
) {
    
    // Get emergency contacts from local database
    fun getEmergencyContacts(): Flow<List<EmergencyContact>> {
        return emergencyContactDao.getEmergencyContacts().map { entities ->
            entities.map { entity ->
                EmergencyContact(
                    id = entity.id.toString(),
                    name = entity.name,
                    phoneNumber = entity.phoneNumber,
                    relationship = entity.relationship
                )
            }
        }
    }
    
    // Get all emergency contacts from local database
    fun getAllEmergencyContacts(): Flow<List<EmergencyContact>> {
        return emergencyContactDao.getAllEmergencyContacts().map { entities ->
            entities.map { entity ->
                EmergencyContact(
                    id = entity.id.toString(),
                    name = entity.name,
                    phoneNumber = entity.phoneNumber,
                    relationship = entity.relationship
                )
            }
        }
    }
    
    // Fetch contacts from phone
    suspend fun getPhoneContacts(): List<EmergencyContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<EmergencyContact>()
        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        )
        
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                
                contacts.add(
                    EmergencyContact(
                        id = contactId,
                        name = name,
                        phoneNumber = number.replace("\\s".toRegex(), "")
                    )
                )
            }
        }
        
        contacts.distinctBy { it.phoneNumber }
    }
    
    // Add emergency contact to local database
    suspend fun addEmergencyContact(contact: EmergencyContact) {
        val entity = EmergencyContactEntity(
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            relationship = contact.relationship
        )
        emergencyContactDao.insertEmergencyContact(entity)
    }
    
    // Remove emergency contact from local database
    suspend fun removeEmergencyContact(contact: EmergencyContact) {
        val entity = EmergencyContactEntity(
            id = contact.id.toLongOrNull() ?: 0,
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            relationship = contact.relationship
        )
        emergencyContactDao.deleteEmergencyContact(entity)
    }
    
    // Get count of emergency contacts
    suspend fun getEmergencyContactCount(): Int {
        return emergencyContactDao.getEmergencyContactCount()
    }
} 