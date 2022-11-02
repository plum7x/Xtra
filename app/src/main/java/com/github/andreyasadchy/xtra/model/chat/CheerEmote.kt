package com.github.andreyasadchy.xtra.model.chat

class CheerEmote(
    override val name: String,
    override val url1x: String?,
    override val url2x: String?,
    override val url3x: String?,
    override val url4x: String?,
    override val type: String?,
    val minBits: Int,
    val color: String? = null) : Emote()