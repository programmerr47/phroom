package com.programmerr47.phroom.sample

import android.app.Application

class App : Application() {
    val appLocator by lazy { Locator(this) }
}
