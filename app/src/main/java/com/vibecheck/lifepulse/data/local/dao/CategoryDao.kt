package com.vibecheck.lifepulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vibecheck.lifepulse.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET isDefault = 1 WHERE name = :name COLLATE NOCASE")
    suspend fun markAsDefault(name: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)
}

