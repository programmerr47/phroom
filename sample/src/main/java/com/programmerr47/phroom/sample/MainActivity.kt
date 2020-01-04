package com.programmerr47.phroom.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private val locator = Locator() //todo make a global locator
    private val userAdapter = UserAdapter(locator.phroom)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        with(rvList) {
            adapter = userAdapter
            layoutManager = GridLayoutManager(context, 4) //todo convert to device dimension related constant
        }


        GlobalScope.launch(Dispatchers.Main) {
//            val users = withContext(Dispatchers.IO) { locator.api.getUsers(100) }
//            userAdapter.updateList(users.results.map {
//                //DON'T use that in production code. I've added small probation of not valid url,
//                //because all urls in that api are valid and I want to show and see how
//                //error fallback looks like
//                val isValidUrl = (0..20).random() != 0
//                if (isValidUrl) it.picture.large else "error_url"
//            })

            //random user is not working right now
            val photos = listOf(
                "https://cdn.pixabay.com/photo/2013/07/21/13/00/rose-165819__340.jpg",
                "https://image.shutterstock.com/image-photo/mountains-during-sunset-beautiful-natural-600w-407021107.jpg",
                "https://as.ftcdn.net/r/v1/pics/7b11b8176a3611dbfb25406156a6ef50cd3a5009/home/discover_collections/optimized/image-2019-10-11-11-36-27-681.jpg",
                "https://images.unsplash.com/photo-1513366208864-87536b8bd7b4?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&w=1000&q=80",
                "http://3v6x691yvn532gp2411ezrib-wpengine.netdna-ssl.com/wp-content/uploads/2019/05/imagetext01.jpg",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTn2_KYvB0QQxM_v1Zt-qGn3MmiC9k3uzsWw5YrpUDZIUq0971QxA&s",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcS8EG9vn-Pe4i6ww4NQdM9Sar3RMf79Drp-Gf5WsNndord8MAiDgw&s",
                "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRd8_fBDqAPS1-J5GBgyMq_jLei5wW25oX1wbIjjEZo_KWku3e2&s"
            )
            val final = mutableListOf<String>().apply {
                repeat(50) {
                    add(photos.random())
                }
            }

            userAdapter.updateList(final.map {
                //DON'T use that in production code. I've added small probation of not valid url,
                //because all urls in that api are valid and I want to show and see how
                //error fallback looks like
                val isValidUrl = (0..20).random() != 0
                if (isValidUrl) it else "error_url"
            })
        }
    }
}
