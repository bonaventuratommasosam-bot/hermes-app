package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_profiles")
data class SavedProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val profileKey: String,
    val provider: String,
    val model: String,
    val maxTokens: Int,
    val apiKey: String,
    val deployMode: String,
    val telegramToken: String,
    val vpsUser: String,
    val vpsHost: String,
    val vpsPath: String,
    val soulPrompt: String,
    val configYaml: String,
    val installScript: String,
    val timestamp: Long = System.currentTimeMillis()
)
