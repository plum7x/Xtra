package com.github.andreyasadchy.xtra.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shown_notifications")
data class ShownNotification(
    @PrimaryKey
    val channelId: String,
    val startedAt: Long)