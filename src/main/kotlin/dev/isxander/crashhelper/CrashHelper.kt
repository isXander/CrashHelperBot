package dev.isxander.crashhelper

import dev.isxander.crashhelper.utils.http
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private var lastCacheUpdate = System.currentTimeMillis()
private var responseCache: JsonObject? = null
val responses: JsonObject
    get() {
        if (responseCache == null || System.currentTimeMillis() - lastCacheUpdate > 600000) {
            invalidateResponses()
            responseCache = Json.decodeFromString(runBlocking { http.get("https://raw.githubusercontent.com/isXander/MinecraftIssues/main/issues.json") })
            lastCacheUpdate = System.currentTimeMillis()
        }

        return responseCache!!
    }

fun invalidateResponses() {
    System.gc() // might as well
    responseCache = null
}