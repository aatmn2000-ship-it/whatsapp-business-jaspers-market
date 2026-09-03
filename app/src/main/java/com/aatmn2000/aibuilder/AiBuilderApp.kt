package com.aatmn2000.aibuilder

import android.app.Application

/**
 * Application entry point. Owns the [AppContainer] that wires the AI gateway,
 * repositories and shared services together.
 */
class AiBuilderApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
