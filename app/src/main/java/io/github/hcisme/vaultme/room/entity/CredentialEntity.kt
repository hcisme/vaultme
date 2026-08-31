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
    // 云端身份：全局唯一
    val uuid: String,
    val platform: String,
    val account: String,
    val password: String,
    // 最后修改时间（epoch 毫秒），用于同步冲突解决
    val updatedAt: Long = 0
)
