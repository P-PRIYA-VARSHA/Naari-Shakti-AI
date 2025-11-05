package com.example.secureshe.data.dao

import androidx.room.*
import com.example.secureshe.data.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    
    @Query("SELECT * FROM emergency_contacts ORDER BY createdAt DESC")
    fun getAllEmergencyContacts(): Flow<List<EmergencyContactEntity>>
    
    @Query("SELECT * FROM emergency_contacts ORDER BY createdAt DESC LIMIT 3")
    fun getEmergencyContacts(): Flow<List<EmergencyContactEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContact(contact: EmergencyContactEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContacts(contacts: List<EmergencyContactEntity>)
    
    @Delete
    suspend fun deleteEmergencyContact(contact: EmergencyContactEntity)
    
    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAllEmergencyContacts()
    
    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getEmergencyContactCount(): Int
} 