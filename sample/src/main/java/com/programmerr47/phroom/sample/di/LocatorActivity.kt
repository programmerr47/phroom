package com.programmerr47.phroom.sample.di

import androidx.appcompat.app.AppCompatActivity
import com.programmerr47.phroom.sample.App

open class LocatorActivity : AppCompatActivity() {
    protected val locator: Locator by lazy { (applicationContext as App).appLocator }
}
