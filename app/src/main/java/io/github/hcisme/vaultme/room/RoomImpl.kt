package io.github.hcisme.vaultme.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.hcisme.vaultme.room.dao.CredentialDao
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import io.github.hcisme.vaultme.utils.Constant
import kotlin.concurrent.Volatile

@Database(
    entities = [CredentialEntity::class],
    version = Constant.ROOM_DATABASE_VERSION,
    exportSchema = false
)
abstract class RoomImpl : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao

    companion object {
        @Volatile
        private var INSTANCE: RoomImpl? = null

        fun getDatabase(context: Context): RoomImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): RoomImpl {
            return Room.databaseBuilder(
                context.applicationContext,
                RoomImpl::class.java,
                Constant.ROOM_DATABASE_NAME
            ).build()
        }
    }
}


