package io.github.hcisme.vaultme.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY id DESC")
    fun getAllCredentials(): Flow<List<CredentialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: CredentialEntity)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)

    @Query("SELECT * FROM credentials WHERE platform LIKE '%' || :query || '%' OR account LIKE '%' || :query || '%'")
    fun searchCredentials(query: String): Flow<List<CredentialEntity>>
}