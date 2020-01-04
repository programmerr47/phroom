package com.programmerr47.phroom.sample

import androidx.recyclerview.widget.DiffUtil

class SimpleItemCallback<T> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
    override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}