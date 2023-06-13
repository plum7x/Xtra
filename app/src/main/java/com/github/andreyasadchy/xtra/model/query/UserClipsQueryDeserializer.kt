package com.github.andreyasadchy.xtra.model.query

import com.github.andreyasadchy.xtra.model.ui.Clip
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class UserClipsQueryDeserializer : JsonDeserializer<UserClipsQueryResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): UserClipsQueryResponse {
        json.takeIf { it.isJsonObject }?.asJsonObject?.get("errors")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { item ->
            item.takeIf { it.isJsonObject }?.asJsonObject?.get("message")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString?.let { if (it == "failed integrity check") throw Exception(it) }
        }
        val data = mutableListOf<Clip>()
        val dataJson = json.asJsonObject.get("data").asJsonObject.get("user").asJsonObject.get("clips").asJsonObject.get("edges").asJsonArray
        val cursor = dataJson?.lastOrNull()?.takeIf { it.isJsonObject }?.asJsonObject?.get("cursor")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString
        val hasNextPage = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("user")?.takeIf { it.isJsonObject }?.asJsonObject?.get("clips")?.takeIf { it.isJsonObject }?.asJsonObject?.get("pageInfo")?.takeIf { it.isJsonObject }?.asJsonObject?.get("hasNextPage")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isBoolean }?.asBoolean
        dataJson?.forEach { item ->
            item.takeIf { it.isJsonObject }?.asJsonObject?.get("node")?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
                data.add(Clip(
                    id = obj.get("slug")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    channelId = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("user")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    channelLogin = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("user")?.takeIf { it.isJsonObject }?.asJsonObject?.get("login")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    channelName = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("user")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    videoId = obj.get("video")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    vodOffset = obj.get("videoOffsetSeconds")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isNumber }?.asInt,
                    gameId = obj.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    gameName = obj.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    title = obj.get("title")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    viewCount = obj.get("viewCount")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isNumber }?.asInt,
                    uploadDate = obj.get("createdAt")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    thumbnailUrl = obj.get("thumbnailURL")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    duration = obj.get("durationSeconds")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isNumber }?.asDouble,
                    profileImageUrl = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("user")?.takeIf { it.isJsonObject }?.asJsonObject?.get("profileImageURL")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    videoAnimatedPreviewURL = obj.get("video")?.takeIf { it.isJsonObject }?.asJsonObject?.get("animatedPreviewURL")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString
                ))
            }
        }
        return UserClipsQueryResponse(data, cursor, hasNextPage)
    }
}
