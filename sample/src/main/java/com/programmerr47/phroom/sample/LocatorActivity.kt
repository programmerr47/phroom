package com.programmerr47.phroom.sample

import androidx.appcompat.app.AppCompatActivity

open class LocatorActivity : AppCompatActivity() {
    protected val locator: Locator by lazy { (applicationContext as App).appLocator }
}
