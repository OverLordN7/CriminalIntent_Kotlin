package com.example.criminalintent_kotlin

import android.app.Application

class CriminalIntentAplication: Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}