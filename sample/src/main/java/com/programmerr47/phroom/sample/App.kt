package com.programmerr47.phroom.sample

import android.app.Application
import com.programmerr47.phroom.sample.di.Locator

class App : Application() {
    val appLocator by lazy { Locator(this) }
}
