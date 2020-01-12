package com.programmerr47.phroom.sample.gallery

import android.content.Intent
import android.os.Bundle
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import com.programmerr47.phroom.MainThreadExecutor
import com.programmerr47.phroom.sample.collage.CollageActivity
import com.programmerr47.phroom.sample.di.LocatorActivity
import com.programmerr47.phroom.sample.R
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors

class GalleryActivity : LocatorActivity() {
    private val userAdapter by lazy { UserAdapter(locator.phroom) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnProceed.setOnClickListener { startActivity(Intent(this, CollageActivity::class.java)) }

        with(rvList) {
            adapter = userAdapter
            layoutManager = GridLayoutManager(context, 4) //todo convert to device dimension related constant
        }

        //Using page list just to have out of the box paging for imitating inifite list
        //In real life we combine here two executors and coroutines, which looks bit of strange
        //and may be ugly as well. So for this example we can just refuse coroutines,
        //but if they mandatory in the project, we need to find a way to improve that
        //may be somehow remove those executors
        //TODO investigate elegant way of mergin PagedList and coroutines together
        userAdapter.submitList(PagedList(
            dataSource = UserDataSource(locator.api),
            config = Config(50),
            notifyExecutor = MainThreadExecutor(),
            fetchExecutor = Executors.newSingleThreadExecutor()
        ))
    }
}
