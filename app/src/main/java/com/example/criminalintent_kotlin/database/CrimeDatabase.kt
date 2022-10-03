package com.example.criminalintent_kotlin.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.criminalintent_kotlin.Crime

@Database(entities = [Crime::class], version = 1)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase: RoomDatabase() {

    abstract fun crimeDao(): CrimeDao

}