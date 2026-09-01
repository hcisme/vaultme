package io.github.hcisme.vaultme.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.hcisme.vaultme.room.entity.CredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Query("SELECT * FROM credentials ORDER BY id DESC")
    fun getAllCredentials(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials")
    suspend fun getAllCredentialsOnce(): List<CredentialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: CredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredentials(credentials: List<CredentialEntity>)

    @Update
    suspend fun updateCredentials(credentials: List<CredentialEntity>)

    @Delete
    suspend fun deleteCredential(credential: CredentialEntity)

    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getCredentialById(id: Long): CredentialEntity?

    @Query("SELECT * FROM credentials WHERE platform LIKE '%' || :query || '%' OR account LIKE '%' || :query || '%'")
    fun searchCredentials(query: String): Flow<List<CredentialEntity>>
}
