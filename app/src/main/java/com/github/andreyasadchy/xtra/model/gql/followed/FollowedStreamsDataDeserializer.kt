package com.github.andreyasadchy.xtra.model.gql.followed

import com.github.andreyasadchy.xtra.model.ui.Stream
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class FollowedStreamsDataDeserializer : JsonDeserializer<FollowedStreamsDataResponse> {

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FollowedStreamsDataResponse {
        json.takeIf { it.isJsonObject }?.asJsonObject?.get("errors")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { item ->
            item.takeIf { it.isJsonObject }?.asJsonObject?.get("message")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString?.let { if (it == "failed integrity check") throw Exception(it) }
        }
        val data = mutableListOf<Stream>()
        val dataJson = json.asJsonObject.get("data").asJsonObject.get("currentUser").asJsonObject.get("followedLiveUsers").asJsonObject.get("edges").asJsonArray
        val cursor = dataJson?.lastOrNull()?.takeIf { it.isJsonObject }?.asJsonObject?.get("cursor")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString
        val hasNextPage = json.takeIf { it.isJsonObject }?.asJsonObject?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject?.get("currentUser")?.takeIf { it.isJsonObject }?.asJsonObject?.get("followedLiveUsers")?.takeIf { it.isJsonObject }?.asJsonObject?.get("pageInfo")?.takeIf { it.isJsonObject }?.asJsonObject?.get("hasNextPage")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isBoolean }?.asBoolean
        dataJson?.forEach { item ->
            item.takeIf { it.isJsonObject }?.asJsonObject?.get("node")?.takeIf { it.isJsonObject }?.asJsonObject?.let { obj ->
                data.add(Stream(
                    id = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    channelId = obj.get("id")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    channelLogin = obj.get("login")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    channelName = obj.get("displayName")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    gameId = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("id")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    gameName = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("game")?.takeIf { it.isJsonObject }?.asJsonObject?.get("displayName")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    type = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("type")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    title = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("title")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    viewerCount = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("viewersCount")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isNumber }?.asInt,
                    thumbnailUrl = obj.get("stream")?.takeIf { it.isJsonObject }?.asJsonObject?.get("previewImageURL")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    profileImageUrl = obj.get("profileImageURL")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString,
                    tags = obj.get("freeformTags")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { tagElement ->
                        tagElement.takeIf { it.isJsonObject }?.asJsonObject?.get("name")?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive?.takeIf { it.isString }?.asString
                    }
                ))
            }
        }
        return FollowedStreamsDataResponse(data, cursor, hasNextPage)
    }
}
