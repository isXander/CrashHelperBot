package dev.isxander.crashhelper.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

val http = HttpClient()

fun getContentLength(url: String): Long {
    return runBlocking {
        http.head<Headers>(url)["Content-Length"]!!.toLong()
    }
}

fun uploadToHastebin(text: String, raw: Boolean = true): String {
    return runBlocking {
        val response = http.post<HttpResponse>("https://hst.sh/documents") {
            body = text
        }

        response.readBytes().decodeToString().let {
            val json = Json.decodeFromString<JsonObject>(it)
            return@let "https://hst.sh/${if (raw) "raw/" else ""}${json["key"]!!.jsonPrimitive.content}"
        }
    }
}