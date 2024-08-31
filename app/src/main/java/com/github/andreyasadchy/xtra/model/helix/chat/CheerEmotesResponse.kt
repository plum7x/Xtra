package com.github.andreyasadchy.xtra.model.helix.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheerEmotesResponse(
    val data: List<Set>,
) {
    @Serializable
    data class Set(
        val prefix: String,
        val tiers: List<Tier>,
    )

    @Serializable
    data class Tier(
        @SerialName("min_bits")
        val minBits: Int,
        val color: String? = null,
        val images: Themes,
    )

    @Serializable
    data class Themes(
        val dark: Formats? = null,
        val light: Formats? = null,
    )

    @Serializable
    data class Formats(
        val animated: Urls? = null,
        val static: Urls? = null,
    )

    @Serializable
    data class Urls(
        @SerialName("1")
        val url1x: String? = null,
        @SerialName("2")
        val url2x: String? = null,
        @SerialName("3")
        val url3x: String? = null,
        @SerialName("4")
        val url4x: String? = null,
    )
}