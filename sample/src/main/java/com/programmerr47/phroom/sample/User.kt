package com.programmerr47.phroom.sample

//TODO remove redundant response by providing custom ResponseBodyConverter
data class UsersResponse(val results: List<User>)

data class User(
    val name: Name,
    val picture: Photo
) {
    data class Name(
        val first: String,
        val last: String
    )

    data class Photo(
        val large: String,
        val medium: String,
        val thumbnail: String
    )
}