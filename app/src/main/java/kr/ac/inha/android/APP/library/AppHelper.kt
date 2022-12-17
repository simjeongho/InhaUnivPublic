package kr.ac.inha.android.APP.library

import android.app.Application
import android.content.Context

class AppHelper : Application() {
    init {
        instance = this
    }

    companion object {
        private var instance: AppHelper? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        val context: Context = AppHelper.applicationContext()
    }
}
