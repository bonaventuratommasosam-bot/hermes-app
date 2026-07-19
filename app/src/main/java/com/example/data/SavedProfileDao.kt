package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedProfileDao {
    @Query("SELECT * FROM saved_profiles ORDER BY timestamp DESC")
    fun getAllSavedProfiles(): Flow<List<SavedProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SavedProfile): Long

    @Query("DELETE FROM saved_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)
}
