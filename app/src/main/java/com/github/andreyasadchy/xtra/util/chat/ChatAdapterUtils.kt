package com.github.andreyasadchy.xtra.util.chat

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.util.Patterns
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.model.chat.ChatMessage
import com.github.andreyasadchy.xtra.model.chat.CheerEmote
import com.github.andreyasadchy.xtra.model.chat.Emote
import com.github.andreyasadchy.xtra.model.chat.Image
import com.github.andreyasadchy.xtra.model.chat.TwitchBadge
import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import com.github.andreyasadchy.xtra.ui.view.chat.CenteredImageSpan
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import java.util.Random
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.collections.set
import kotlin.math.floor
import kotlin.math.pow
import kotlin.text.toInt

object ChatAdapterUtils {

    private val twitchColors = intArrayOf(-65536, -16776961, -16744448, -5103070, -32944, -6632142, -47872, -13726889, -2448096, -2987746, -10510688, -14774017, -38476, -7722014, -16711809)
    private const val RED_HUE_DEGREES = 0f
    private const val GREEN_HUE_DEGREES = 120f
    private const val BLUE_HUE_DEGREES = 240f
    private const val PI_DEGREES = 180f
    private const val TWO_PI_DEGREES = 360f

    fun prepareChatMessage(chatMessage: ChatMessage, itemView: View, enableTimestamps: Boolean, timestampFormat: String?, firstMsgVisibility: Int, firstChatMsg: String, redeemedChatMsg: String, redeemedNoMsg: String, rewardChatMsg: String, useRandomColors: Boolean, random: Random, useReadableColors: Boolean, isLightTheme: Boolean, nameDisplay: String?, useBoldNames: Boolean, showSystemMessageEmotes: Boolean, loggedInUser: String?, chatUrl: String?, getEmoteBytes: ((String, Pair<Long, Int>) -> ByteArray?)?, userColors: HashMap<String, Int>, savedColors: HashMap<String, Int>, localTwitchEmotes: List<TwitchEmote>?, globalStvEmotes: List<Emote>?, channelStvEmotes: List<Emote>?, globalBttvEmotes: List<Emote>?, channelBttvEmotes: List<Emote>?, globalFfzEmotes: List<Emote>?, channelFfzEmotes: List<Emote>?, globalBadges: List<TwitchBadge>?, channelBadges: List<TwitchBadge>?, cheerEmotes: List<CheerEmote>?, savedLocalTwitchEmotes: MutableMap<String, ByteArray>, savedLocalBadges: MutableMap<String, ByteArray>, savedLocalCheerEmotes: MutableMap<String, ByteArray>, savedLocalEmotes: MutableMap<String, ByteArray>): Pair<SpannableStringBuilder, ArrayList<Image>> {
        val builder = SpannableStringBuilder()
        val images = ArrayList<Image>()
        var builderIndex = 0
        var badgesCount = 0
        if (chatMessage.message.isNullOrBlank()) {
            if (chatMessage.timestamp != null && enableTimestamps) {
                val timestamp = TwitchApiHelper.getTimestamp(chatMessage.timestamp, timestampFormat)
                if (timestamp != null) {
                    builder.append("$timestamp ")
                    builder.setSpan(ForegroundColorSpan(getSavedColor("#999999", savedColors, useReadableColors, isLightTheme)), 0, timestamp.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                    builderIndex += timestamp.length + 1
                }
            }
            if (chatMessage.systemMsg != null) {
                builder.append(chatMessage.systemMsg)
                builder.setSpan(ForegroundColorSpan(getSavedColor("#999999", savedColors, useReadableColors, isLightTheme)), builderIndex, builderIndex + chatMessage.systemMsg.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                if (showSystemMessageEmotes) {
                    prepareEmotes(chatMessage, chatMessage.systemMsg, builder, builderIndex, badgesCount, images, useReadableColors, isLightTheme, useBoldNames, loggedInUser, chatUrl, getEmoteBytes, savedColors, localTwitchEmotes, globalStvEmotes, channelStvEmotes, globalBttvEmotes, channelBttvEmotes, globalFfzEmotes, channelFfzEmotes, cheerEmotes, savedLocalTwitchEmotes, savedLocalCheerEmotes, savedLocalEmotes)
                }
                builderIndex = builder.length
            } else {
                if (chatMessage.reward?.title != null) {
                    val string = redeemedNoMsg.format(chatMessage.userName, chatMessage.reward.title)
                    builder.append("$string ")
                    builder.setSpan(ForegroundColorSpan(getSavedColor("#999999", savedColors, useReadableColors, isLightTheme)), builderIndex, builderIndex + string.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                    if (showSystemMessageEmotes) {
                        prepareEmotes(chatMessage, string, builder, builderIndex, badgesCount, images, useReadableColors, isLightTheme, useBoldNames, loggedInUser, chatUrl, getEmoteBytes, savedColors, localTwitchEmotes, globalStvEmotes, channelStvEmotes, globalBttvEmotes, channelBttvEmotes, globalFfzEmotes, channelFfzEmotes, cheerEmotes, savedLocalTwitchEmotes, savedLocalCheerEmotes, savedLocalEmotes)
                    }
                    builderIndex = builder.length
                    builder.append(". ")
                    builder.setSpan(ForegroundColorSpan(Color.TRANSPARENT), builderIndex, builderIndex + 1, SPAN_EXCLUSIVE_EXCLUSIVE)
                    images.add(Image(
                        url1x = chatMessage.reward.url1x,
                        url2x = chatMessage.reward.url2x,
                        url3x = chatMessage.reward.url4x,
                        url4x = chatMessage.reward.url4x,
                        start = builderIndex++,
                        end = builderIndex++
                    ))
                    if (chatMessage.reward.cost != null) {
                        builder.append("${chatMessage.reward.cost}")
                        builder.setSpan(ForegroundColorSpan(getSavedColor("#999999", savedColors, useReadableColors, isLightTheme)), builderIndex, builderIndex + chatMessage.reward.cost.toString().length, SPAN_EXCLUSIVE_EXCLUSIVE)
                        builderIndex += chatMessage.reward.cost.toString().length
                    }
                }
            }
            itemView.setBackgroundResource(0)
        } else {
            if (chatMessage.systemMsg != null) {
                builder.append("${chatMessage.systemMsg}\n")
                builderIndex += chatMessage.systemMsg.length + 1
            } else {
                if (chatMessage.msgId != null) {
                    val msgId = TwitchApiHelper.getMessageIdString(chatMessage.msgId) ?: chatMessage.msgId
                    builder.append("$msgId\n")
                    builderIndex += msgId.length + 1
                }
            }
            if (chatMessage.isFirst && firstMsgVisibility == 0) {
                builder.append("$firstChatMsg\n")
                builderIndex += firstChatMsg.length + 1
            }
            if (chatMessage.reward?.title != null) {
                val string = redeemedChatMsg.format(chatMessage.reward.title)
                builder.append("$string ")
                builderIndex += string.length + 1
                builder.append(". ")
                builder.setSpan(ForegroundColorSpan(Color.TRANSPARENT), builderIndex, builderIndex + 1, SPAN_EXCLUSIVE_EXCLUSIVE)
                images.add(Image(
                    url1x = chatMessage.reward.url1x,
                    url2x = chatMessage.reward.url2x,
                    url3x = chatMessage.reward.url4x,
                    url4x = chatMessage.reward.url4x,
                    start = builderIndex++,
                    end = builderIndex++
                ))
                badgesCount++
                if (chatMessage.reward.cost != null) {
                    builder.append("${chatMessage.reward.cost}")
                    builderIndex += chatMessage.reward.cost.toString().length
                }
                builder.append("\n")
                builderIndex += 1
            } else {
                if (chatMessage.reward?.id != null && firstMsgVisibility == 0) {
                    builder.append("$rewardChatMsg\n")
                    builderIndex += rewardChatMsg.length + 1
                }
            }
            if (chatMessage.timestamp != null && enableTimestamps) {
                val timestamp = TwitchApiHelper.getTimestamp(chatMessage.timestamp, timestampFormat)
                if (timestamp != null) {
                    builder.append("$timestamp ")
                    builder.setSpan(ForegroundColorSpan(getSavedColor("#999999", savedColors, useReadableColors, isLightTheme)), builderIndex, builderIndex + timestamp.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                    builderIndex += timestamp.length + 1
                }
            }
            chatMessage.badges?.forEach { chatBadge ->
                val badge = channelBadges?.find { it.setId == chatBadge.setId && it.version == chatBadge.version } ?: globalBadges?.find { it.setId == chatBadge.setId && it.version == chatBadge.version }
                if (badge != null) {
                    builder.append(". ")
                    builder.setSpan(ForegroundColorSpan(Color.TRANSPARENT), builderIndex, builderIndex + 1, SPAN_EXCLUSIVE_EXCLUSIVE)
                    images.add(Image(
                        localData = badge.localData?.let { getLocalBadgeData(badge.setId + badge.version, it, savedLocalBadges, chatUrl, getEmoteBytes) },
                        url1x = badge.url1x,
                        url2x = badge.url2x,
                        url3x = badge.url3x,
                        url4x = badge.url4x,
                        start = builderIndex++,
                        end = builderIndex++
                    ))
                    badgesCount++
                }
            }
            val color = if (chatMessage.color != null) {
                getSavedColor(chatMessage.color, savedColors, useReadableColors, isLightTheme)
            } else {
                userColors[chatMessage.userName] ?: if (useRandomColors) {
                    twitchColors[random.nextInt(twitchColors.size)]
                } else {
                    -10066329
                }.let { newColor ->
                    if (useReadableColors) {
                        adaptUsernameColor(newColor, isLightTheme)
                    } else {
                        newColor
                    }.also { if (chatMessage.userName != null) userColors[chatMessage.userName] = it }
                }
            }
            if (!chatMessage.userName.isNullOrBlank()) {
                val userName = if (chatMessage.userLogin != null && !chatMessage.userLogin.equals(chatMessage.userName, true)) {
                    when (nameDisplay) {
                        "0" -> "${chatMessage.userName}(${chatMessage.userLogin})"
                        "1" -> chatMessage.userName
                        else -> chatMessage.userLogin
                    }
                } else {
                    chatMessage.userName
                }
                builder.append(userName)
                builder.setSpan(ForegroundColorSpan(color), builderIndex, builderIndex + userName.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                if (useBoldNames) {
                    builder.setSpan(StyleSpan(Typeface.BOLD), builderIndex, builderIndex + userName.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                builderIndex += userName.length
                if (!chatMessage.isAction) {
                    builder.append(": ")
                    builderIndex += 2
                } else {
                    builder.append(" ")
                    builderIndex += 1
                }
            }
            builder.append(chatMessage.message)
            if (chatMessage.isAction) {
                builder.setSpan(ForegroundColorSpan(color), builderIndex, builderIndex + chatMessage.message.length, SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val wasMentioned = prepareEmotes(chatMessage, chatMessage.message, builder, builderIndex, badgesCount, images, useReadableColors, isLightTheme, useBoldNames, loggedInUser, chatUrl, getEmoteBytes, savedColors, localTwitchEmotes, globalStvEmotes, channelStvEmotes, globalBttvEmotes, channelBttvEmotes, globalFfzEmotes, channelFfzEmotes, cheerEmotes, savedLocalTwitchEmotes, savedLocalCheerEmotes, savedLocalEmotes) == true
            when {
                chatMessage.isFirst && firstMsgVisibility < 2 -> itemView.setBackgroundResource(R.color.chatMessageFirst)
                chatMessage.reward?.id != null && firstMsgVisibility < 2 -> itemView.setBackgroundResource(R.color.chatMessageReward)
                chatMessage.systemMsg != null || chatMessage.msgId != null -> itemView.setBackgroundResource(R.color.chatMessageNotice)
                wasMentioned -> itemView.setBackgroundResource(R.color.chatMessageMention)
                else -> itemView.setBackgroundResource(0)
            }
        }
        return Pair(builder, images)
    }

    private fun getSavedColor(color: String, savedColors: HashMap<String, Int>, useReadableColors: Boolean, isLightTheme: Boolean): Int {
        return savedColors[color] ?: Color.parseColor(color).let { newColor ->
            if (useReadableColors) {
                adaptUsernameColor(newColor, isLightTheme)
            } else {
                newColor
            }.also { savedColors[color] = it }
        }
    }

    private fun adaptUsernameColor(color: Int, isLightTheme: Boolean): Int {
        val colorArray = FloatArray(3)
        ColorUtils.colorToHSL(color, colorArray)
        if (isLightTheme) {
            val luminanceMax = 0.75f -
                    maxOf(1f - ((colorArray[0] - GREEN_HUE_DEGREES) / 100f).pow(2f), RED_HUE_DEGREES) * 0.4f
            colorArray[2] = minOf(colorArray[2], luminanceMax)
        } else {
            val distToRed = RED_HUE_DEGREES - colorArray[0]
            val distToBlue = BLUE_HUE_DEGREES - colorArray[0]
            val normDistanceToRed = distToRed - TWO_PI_DEGREES * floor((distToRed + PI_DEGREES) / TWO_PI_DEGREES)
            val normDistanceToBlue = distToBlue - TWO_PI_DEGREES * floor((distToBlue + PI_DEGREES) / TWO_PI_DEGREES)

            val luminanceMin = 0.3f +
                    maxOf((1f - (normDistanceToBlue / 40f).pow(2f)) * 0.35f, RED_HUE_DEGREES) +
                    maxOf((1f - (normDistanceToRed / 40f).pow(2f)) * 0.1f, RED_HUE_DEGREES)
            colorArray[2] = maxOf(colorArray[2], luminanceMin)
        }

        return ColorUtils.HSLToColor(colorArray)
    }

    private fun prepareEmotes(chatMessage: ChatMessage, message: String, builder: SpannableStringBuilder, builderIndex: Int, badgesCount: Int, images: ArrayList<Image>, useReadableColors: Boolean, isLightTheme: Boolean, useBoldNames: Boolean, loggedInUser: String?, chatUrl: String?, getEmoteBytes: ((String, Pair<Long, Int>) -> ByteArray?)?, savedColors: HashMap<String, Int>, localTwitchEmotes: List<TwitchEmote>?, globalStvEmotes: List<Emote>?, channelStvEmotes: List<Emote>?, globalBttvEmotes: List<Emote>?, channelBttvEmotes: List<Emote>?, globalFfzEmotes: List<Emote>?, channelFfzEmotes: List<Emote>?, cheerEmotes: List<CheerEmote>?, savedLocalTwitchEmotes: MutableMap<String, ByteArray>, savedLocalCheerEmotes: MutableMap<String, ByteArray>, savedLocalEmotes: MutableMap<String, ByteArray>): Boolean {
        return try {
            var builderIndex = builderIndex
            chatMessage.emotes?.let { emotes ->
                val copy = emotes.map {
                    val realBegin = message.offsetByCodePoints(0, it.begin)
                    val realEnd = if (it.begin == realBegin) {
                        it.end
                    } else {
                        it.end + realBegin - it.begin
                    }
                    localTwitchEmotes?.let { localEmotes ->
                        localEmotes.find { emote -> emote.id == it.id }?.let { emote ->
                            TwitchEmote(
                                id = emote.id,
                                name = emote.name,
                                localData = emote.localData,
                                format = emote.format,
                                isAnimated = emote.isAnimated,
                                begin = realBegin,
                                end = realEnd,
                                setId = emote.setId,
                                ownerId = emote.ownerId
                            )
                        }
                    } ?: TwitchEmote(id = it.id, begin = realBegin, end = realEnd)
                }
                for (e in copy) {
                    val begin = builderIndex + e.begin
                    builder.replace(begin, builderIndex + e.end + 1, ".")
                    builder.setSpan(ForegroundColorSpan(Color.TRANSPARENT), begin, begin + 1, SPAN_EXCLUSIVE_EXCLUSIVE)
                    val length = e.end - e.begin
                    for (e1 in copy) {
                        if (e.begin < e1.begin) {
                            e1.begin -= length
                            e1.end -= length
                        }
                    }
                    e.end -= length
                }
                copy.forEach { emote ->
                    images.add(Image(
                        localData = emote.localData?.let { getLocalTwitchEmoteData(emote.id!!, it, savedLocalTwitchEmotes, chatUrl, getEmoteBytes) },
                        url1x = emote.url1x,
                        url2x = emote.url2x,
                        url3x = emote.url3x,
                        url4x = emote.url4x,
                        format = emote.format,
                        isAnimated = emote.isAnimated,
                        isEmote = true,
                        start = builderIndex + emote.begin,
                        end = builderIndex + emote.end + 1
                    ))
                }
            }
            val split = builder.substring(builderIndex).split(" ")
            var emotesFound = 0
            var wasMentioned = false
            for (value in split) {
                val cheerEmote = if (chatMessage.bits != null) {
                    val bitsCount = value.takeLastWhile { it.isDigit() }
                    val bitsName = value.substringBeforeLast(bitsCount)
                    if (bitsCount.isNotEmpty()) {
                        val emote = cheerEmotes?.findLast { it.name.equals(bitsName, true) && it.minBits <= bitsCount.toInt() }
                        if (emote != null) {
                            for (j in images.lastIndex - emotesFound downTo badgesCount) {
                                val e = images[j]
                                if (e.start > builderIndex) {
                                    val remove = bitsName.length - 1
                                    e.start -= remove
                                    e.end -= remove
                                }
                            }
                            builder.replace(builderIndex, builderIndex + bitsName.length, ".")
                            builder.setSpan(ForegroundColorSpan(Color.TRANSPARENT), builderIndex, builderIndex + 1, SPAN_EXCLUSIVE_EXCLUSIVE)
                            images.add(Image(
                                localData = emote.localData?.let { getLocalCheerEmoteData(emote.name + emote.minBits, it, savedLocalCheerEmotes, chatUrl, getEmoteBytes) },
                                url1x = emote.url1x,
                                url2x = emote.url2x,
                                url3x = emote.url3x,
                                url4x = emote.url4x,
                                format = emote.format,
                                isAnimated = emote.isAnimated,
                                isZeroWidth = false,
                                isEmote = true,
                                start = builderIndex,
                                end = builderIndex + 1
                            ))
                            emotesFound++
                            builderIndex += 1
                            if (!emote.color.isNullOrBlank()) {
                                builder.setSpan(ForegroundColorSpan(getSavedColor(emote.color, savedColors, useReadableColors, isLightTheme)), builderIndex, builderIndex + bitsCount.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            builderIndex += bitsCount.length + 1
                        }
                        emote
                    } else null
                } else null
                if (cheerEmote == null) {
                    val emote = channelStvEmotes?.find { it.name == value } ?:
                    channelBttvEmotes?.find { it.name == value } ?:
                    channelFfzEmotes?.find { it.name == value } ?:
                    globalStvEmotes?.find { it.name == value } ?:
                    globalBttvEmotes?.find { it.name == value } ?:
                    globalFfzEmotes?.find { it.name == value }
                    if (emote != null) {
                        for (j in images.lastIndex - emotesFound downTo badgesCount) {
                            val e = images[j]
                            if (e.start > builderIndex) {
                                val remove = value.length - 1
                                e.start -= remove
                                e.end -= remove
                            }
                        }
                        builder.replace(builderIndex, builderIndex + value.length, ".")
                        builder.setSpan(ForegroundColorSpan(Color.TRANSPARENT), builderIndex, builderIndex + 1, SPAN_EXCLUSIVE_EXCLUSIVE)
                        images.add(Image(
                            localData = emote.localData?.let { getLocalEmoteData(emote.name!!, it, savedLocalEmotes, chatUrl, getEmoteBytes) },
                            url1x = emote.url1x,
                            url2x = emote.url2x,
                            url3x = emote.url3x,
                            url4x = emote.url4x,
                            format = emote.format,
                            isAnimated = emote.isAnimated,
                            isZeroWidth = emote.isZeroWidth,
                            isEmote = true,
                            start = builderIndex,
                            end = builderIndex + 1
                        ))
                        emotesFound++
                        builderIndex += 2
                    } else {
                        if (Patterns.WEB_URL.matcher(value).matches()) {
                            val url = if (value.startsWith("http")) value else "https://$value"
                            builder.setSpan(URLSpan(url), builderIndex, builderIndex + value.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else {
                            if (value.startsWith('@') && useBoldNames) {
                                builder.setSpan(StyleSpan(Typeface.BOLD), builderIndex, builderIndex + value.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                            }
                            if (!wasMentioned && !loggedInUser.isNullOrBlank() && value.contains(loggedInUser, true) && chatMessage.userId != null && chatMessage.userLogin != loggedInUser) {
                                wasMentioned = true
                            }
                        }
                        builderIndex += value.length + 1
                    }
                }
            }
            wasMentioned
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalTwitchEmoteData(name: String, data: Pair<Long, Int>, savedLocalTwitchEmotes: MutableMap<String, ByteArray>, chatUrl: String?, getEmoteBytes: ((String, Pair<Long, Int>) -> ByteArray?)?): ByteArray? {
        return savedLocalTwitchEmotes[name] ?: chatUrl?.let{ url ->
            getEmoteBytes?.let { get ->
                get(url, data)?.also {
                    if (savedLocalTwitchEmotes.size >= 100) {
                        savedLocalTwitchEmotes.remove(savedLocalTwitchEmotes.keys.first())
                    }
                    savedLocalTwitchEmotes[name] = it
                }
            }
        }
    }

    private fun getLocalBadgeData(name: String, data: Pair<Long, Int>, savedLocalBadges: MutableMap<String, ByteArray>, chatUrl: String?, getEmoteBytes: ((String, Pair<Long, Int>) -> ByteArray?)?): ByteArray? {
        return savedLocalBadges[name] ?: chatUrl?.let{ url ->
            getEmoteBytes?.let { get ->
                get(url, data)?.also {
                    if (savedLocalBadges.size >= 100) {
                        savedLocalBadges.remove(savedLocalBadges.keys.first())
                    }
                    savedLocalBadges[name] = it
                }
            }
        }
    }

    private fun getLocalCheerEmoteData(name: String, data: Pair<Long, Int>, savedLocalCheerEmotes: MutableMap<String, ByteArray>, chatUrl: String?, getEmoteBytes: ((String, Pair<Long, Int>) -> ByteArray?)?): ByteArray? {
        return savedLocalCheerEmotes[name] ?: chatUrl?.let{ url ->
            getEmoteBytes?.let { get ->
                get(url, data)?.also {
                    if (savedLocalCheerEmotes.size >= 100) {
                        savedLocalCheerEmotes.remove(savedLocalCheerEmotes.keys.first())
                    }
                    savedLocalCheerEmotes[name] = it
                }
            }
        }
    }

    private fun getLocalEmoteData(name: String, data: Pair<Long, Int>, savedLocalEmotes: MutableMap<String, ByteArray>, chatUrl: String?, getEmoteBytes: ((String, Pair<Long, Int>) -> ByteArray?)?): ByteArray? {
        return savedLocalEmotes[name] ?: chatUrl?.let{ url ->
            getEmoteBytes?.let { get ->
                get(url, data)?.also {
                    if (savedLocalEmotes.size >= 100) {
                        savedLocalEmotes.remove(savedLocalEmotes.keys.first())
                    }
                    savedLocalEmotes[name] = it
                }
            }
        }
    }

    fun loadImages(fragment: Fragment, itemView: View, bind: (SpannableStringBuilder) -> Unit, images: List<Image>, imageLibrary: String?, builder: SpannableStringBuilder, emoteSize: Int, badgeSize: Int, emoteQuality: String, animateGifs: Boolean, enableZeroWidth: Boolean) {
        images.forEach {
            loadGlide(fragment, it, itemView, bind, builder, emoteSize, badgeSize, emoteQuality, animateGifs, enableZeroWidth)
        }
    }

    private fun loadGlide(fragment: Fragment, image: Image, itemView: View, bind: (SpannableStringBuilder) -> Unit, builder: SpannableStringBuilder, emoteSize: Int, badgeSize: Int, emoteQuality: String, animateGifs: Boolean, enableZeroWidth: Boolean) {
        Glide.with(fragment)
            .load(image.localData ?: when (emoteQuality) {
                "4" -> image.url4x ?: image.url3x ?: image.url2x ?: image.url1x
                "3" -> image.url3x ?: image.url2x ?: image.url1x
                "2" -> image.url2x ?: image.url1x
                else -> image.url1x
            })
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    val size = if (image.isEmote) {
                        calculateEmoteSize(resource, emoteSize)
                    } else {
                        Pair(badgeSize, badgeSize)
                    }
                    if (image.isZeroWidth && enableZeroWidth) {
                        resource.setBounds(-90, 0, size.first - 90, size.second)
                    } else {
                        resource.setBounds(0, 0, size.first, size.second)
                    }
                    if (resource is Animatable && image.isAnimated && animateGifs) {
                        resource.callback = object : Drawable.Callback {
                            override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                                itemView.removeCallbacks(what)
                            }

                            override fun invalidateDrawable(who: Drawable) {
                                itemView.invalidate()
                            }

                            override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                                itemView.postDelayed(what, `when`)
                            }
                        }
                        (resource as Animatable).start()
                    }
                    try {
                        builder.setSpan(CenteredImageSpan(resource), image.start, image.end, SPAN_EXCLUSIVE_EXCLUSIVE)
                    } catch (e: IndexOutOfBoundsException) {
                    }
                    bind(builder)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    private fun calculateEmoteSize(resource: Drawable, emoteSize: Int): Pair<Int, Int> {
        val widthRatio = resource.intrinsicWidth.toFloat() / resource.intrinsicHeight.toFloat()
        val width: Int
        val height: Int
        when {
            widthRatio == 1f -> {
                width = emoteSize
                height = emoteSize
            }
            widthRatio <= 1.2f -> {
                width = (emoteSize * widthRatio).toInt()
                height = emoteSize
            }
            else -> {
                val scaledEmoteSize = (emoteSize * 0.78f).toInt()
                width = (scaledEmoteSize * widthRatio).toInt()
                height = scaledEmoteSize
            }
        }
        return width to height
    }
}