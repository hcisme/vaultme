package io.github.hcisme.vaultme.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credentials",
    indices = [
        Index(value = ["uuid"], name = "index_credentials_uuid", unique = true)
    ]
)
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val uuid: String,
    val platform: String,
    val account: String,
    val password: String,

    val updatedAt: Long = 0
)
