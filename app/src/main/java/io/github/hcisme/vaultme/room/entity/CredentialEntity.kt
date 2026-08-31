package io.github.hcisme.vaultme.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: String,
    val account: String,
    val password: String
)