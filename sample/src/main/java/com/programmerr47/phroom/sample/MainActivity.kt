package com.programmerr47.phroom.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private val locator = Locator() //todo make a global locator
    private val userAdapter = UserAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(rvList) {
            adapter = userAdapter
            layoutManager = GridLayoutManager(context, 4) //todo convert to device dimension related constant
        }

        GlobalScope.launch(Dispatchers.Main) {
            val users = withContext(Dispatchers.IO) { locator.api.getUsers(100) }
            userAdapter.updateList(users.results.map { it.picture.large })
        }
    }
}
