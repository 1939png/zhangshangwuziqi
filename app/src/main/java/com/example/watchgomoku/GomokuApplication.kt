package com.example.watchgomoku

import android.app.Application
import com.example.watchgomoku.data.AppContainer

class GomokuApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
