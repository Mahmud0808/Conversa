package com.drdisagree.conversa

import android.app.Application
import com.google.android.material.color.DynamicColors

class Conversa : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}