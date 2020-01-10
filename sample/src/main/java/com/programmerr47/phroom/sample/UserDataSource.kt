package com.programmerr47.phroom.sample

import androidx.paging.ItemKeyedDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserDataSource(
    private val api: Api
) : ItemKeyedDataSource<Unit, String>() {

    override fun loadInitial(params: LoadInitialParams<Unit>, callback: LoadInitialCallback<String>) =
        load(params.requestedLoadSize) { callback.onResult(it, 0, it.size) }

    override fun loadAfter(params: LoadParams<Unit>, callback: LoadCallback<String>) =
        load(params.requestedLoadSize) { callback.onResult(it) }

    override fun loadBefore(params: LoadParams<Unit>, callback: LoadCallback<String>) {}

    override fun getKey(item: String) {}

    private fun load(size: Int, onResult: (List<String>) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            //TODO handle not internet related exceptions
            val users = withContext(Dispatchers.IO) { api.getUsers(size) }
            onResult(users.results.map {
                //DON'T use that in production code. I've added small probation of not valid url,
                //because all urls in that api are valid and I want to show and see how
                //error fallback looks like
                val isValidUrl = (0..20).random() != 0
                if (isValidUrl) it.picture.large else "error_url"
            })
        }
    }
}
