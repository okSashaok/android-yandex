package ru.netology.nmedia.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.data.entity.MarkerEntity

@Dao
interface MarkerDao {
    @Query("SELECT * FROM markers")
    fun getAllMarkers(): Flow<List<MarkerEntity>>
    @Query("SELECT * FROM markers WHERE id = :id")
    suspend fun getMarkerById(id: Long): MarkerEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: MarkerEntity): Long

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteMarkerById(id: Long)
}