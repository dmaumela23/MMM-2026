package com.maumela.magnummanagement

import android.app.Application
import com.maumela.magnummanagement.notifications.NotificationChannels

class MmmApplication : Application() {
    /** Created once for the whole process; screens reach it through (application as MmmApplication). */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.create(this)
    }
}
