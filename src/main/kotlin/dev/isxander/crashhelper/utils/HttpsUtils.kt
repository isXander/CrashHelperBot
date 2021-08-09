package dev.isxander.crashhelper.utils

import dev.isxander.crashhelper.CrashHelper
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object HttpsUtils {

    fun setupRequest(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .addHeader("User-Agent", "XanderBot/1.0")
    }

    fun getResponse(url: String): Response {
        val client: OkHttpClient = CrashHelper.JDA.httpClient
        val request = setupRequest(url).build()

        return client.newCall(request).execute().use { it }
    }

    fun getContentLength(url: String): Long {
        val client = CrashHelper.JDA.httpClient
        val request = setupRequest(url)
            .head()
            .build()

        return client.newCall(request).execute().use {
            it.header("Content-Length")!!.toLong()
        }
    }

    fun getBytes(url: String): ByteArray {
        return getResponse(url).body()!!.bytes()
    }

    fun getString(url: String): String {
        return getResponse(url).body()!!.string()
    }

    fun downloadFile(url: String, file: File) {
        FileOutputStream(file).use {
            it.write(getBytes(url))
        }
    }

    fun uploadToHastebin(text: String, raw: Boolean = true): String? {
        val request = setupRequest("https://hst.sh/documents")
            .post(RequestBody.create(MediaType.parse("text/utf-8"), text))
            .build()
        try {
            CrashHelper.JDA.httpClient.newCall(request).execute().use { response ->
                val string: String = response.body()!!.string()
                if (string.startsWith("{")) {
                    val json = JsonObjectExt(string)
                    return "https://hst.sh/${if (raw) "raw/" else ""}" + json["key", ""]
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

}