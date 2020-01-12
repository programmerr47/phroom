package com.programmerr47.phroom.sample.collage

import android.os.Bundle
import androidx.core.content.ContextCompat
import com.programmerr47.phroom.sample.R
import com.programmerr47.phroom.sample.di.LocatorActivity
import kotlinx.android.synthetic.main.activity_collage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class CollageActivity : LocatorActivity() {
    private val rnd = Random(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collage)

        with(scvCollage) {
            phroom = locator.phroom
            framePadding = resources.getDimensionPixelSize(R.dimen.padding_collage_frame)
            frameColor = ContextCompat.getColor(context, R.color.bgFrame)
            errorFrameColor = ContextCompat.getColor(context, R.color.colorAccent)
        }

        generate()
        btnGenerate.setOnClickListener { generate() }
    }

    private fun generate() {
        GlobalScope.launch(Dispatchers.Main) {
            btnGenerate.isEnabled = false

            val users = withContext(Dispatchers.IO) { locator.api.getUsers(rnd.nextInt(5, 50)) }
            scvCollage.generateAgain(users.results.map { it.picture.large })

            btnGenerate.isEnabled = true
        }
    }
}
