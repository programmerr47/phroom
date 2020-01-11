package com.programmerr47.phroom.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_collage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class CollageActivity : AppCompatActivity() {
    private val locator = Locator() //todo make a global locator
    private val rnd = Random(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collage)

        scvCollage.phroom = locator.phroom
        scvCollage.framePadding = resources.getDimensionPixelSize(R.dimen.padding_collage_frame)
        scvCollage.frameColor = ContextCompat.getColor(this, R.color.bgFrame)

        generate()
        btnGenerate.setOnClickListener { generate() }
    }

    private fun generate() {
        GlobalScope.launch(Dispatchers.Main) {
            btnGenerate.isEnabled = false

            val users = withContext(Dispatchers.IO) { locator.api.getUsers(rnd.nextInt(5, 40)) }
            scvCollage.generateAgain(users.results.map { it.picture.large })

            btnGenerate.isEnabled = true
        }
    }
}
