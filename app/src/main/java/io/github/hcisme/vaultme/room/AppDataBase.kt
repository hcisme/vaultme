package io.github.hcisme.vaultme.room

import android.content.Context
import io.github.hcisme.vaultme.room.dao.CredentialDao

/**
 * 快速获取数据库实例
 */
val Context.appDatabase: RoomImpl
    get() = RoomImpl.getDatabase(this)

val Context.credentialDao: CredentialDao
    get() = appDatabase.credentialDao()

