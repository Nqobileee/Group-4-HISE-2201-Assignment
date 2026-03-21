package com.example.geotracker.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {
    @Insert
    void insert(LocationEntity location);

    @Delete
    void delete(LocationEntity location);

    @Query("SELECT * FROM locations")
    LiveData<List<LocationEntity>> getAllLocations();

    @Query("SELECT * FROM locations")
    List<LocationEntity> getAllLocationsSync();
}
