package dev.isxander.crashhelper.utils

import dev.isxander.crashhelper.responses
import kotlinx.serialization.json.*
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs


val IP_REGEX: Regex = Regex("([0-9]{1,3})\\\\.([0-9]{1,3})\\\\.([0-9]{1,3})\\\\.([0-9]{1,3})", RegexOption.IGNORE_CASE)
val URL_REGEX: Regex = Regex("https?://(www\\\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\\\.[a-zA-Z0-9()]{1,6}\\\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)", RegexOption.IGNORE_CASE)
val SENSITIVE_INFO_REGEX: Regex = Regex("(\"access_key\":\".+\"|api.sk1er.club/auth|LoginPacket|SentryAPI.cpp|\"authHash\":|\"hash\":\"|--accessToken \\S+|\\(Session ID is token:|Logging in with details: |Server-Hash: |Checking license key :|USERNAME=.*|https://api\\.hypixel\\.net/.+(\\?key=|&key=))", RegexOption.IGNORE_CASE)
val EMAIL_REGEX: Regex = Regex("[a-zA-Z0-9_.+-]{1,50}@[a-zA-Z0-9-]{1,50}\\.[a-zA-Z0-9-.]{1,10}", RegexOption.IGNORE_CASE)
val PASTEBIN_REGEX: Regex = Regex("(?:https?://)?(?<domain>paste\\.ee|pastebin\\.com|has?tebin\\.com|hasteb\\.in|hst\\.sh)/(?:raw/|p/)?([\\w-.]+)", RegexOption.IGNORE_CASE)
val RAM_REGEX: Regex = Regex("-Xmx(?<ram>\\d+)(?<type>[GMK])", RegexOption.IGNORE_CASE)
val CODE_BLOCK_REGEX: Regex = Regex("```(?<language>[a-zA-Z0-9]*)\\n(?<code>.+)\\n```", RegexOption.DOT_MATCHES_ALL)

val WINDOWS_MAC_USERNAME_REGEX: Regex = Regex("Users[/\\\\](?<username>[^/\\\\]+)(?:[/\\\\]*.)*")
val LINUX_USERNAME_REGEX: Regex = Regex("/home/(?<username>[^/]+)(?:/*[^/])*")

val FORGE_MOD_REGEX: Regex = Regex("(?<state>(?:U?L?C?H?I?J?A?D?E?)+)\\t(?<id>(?: ?[\\w-]+)+)\\{(?<version>(?:[0-9-\\w]\\.*)+)\\} \\[(?<name>(?: ?[\\w-]+)+)\\] \\((?<file>(?: ?(?:[^/<>:\\\"\\\\|?* ])+)+)\\)")
val ESSENTIAL_MOD_REGEX: Regex = Regex("(?<state>(?:U?L?C?H?I?J?A?D?E?)+) +\\| +(?<id>(?: ?[\\w-]+)+) +\\| +(?<version>(?:[0-9-\\w]\\.*)+) +\\| +(?<file>(?: ?(?:[^/<>:\\\"\\\\|?* ])+)+)")

val LOG_TEXT = setOf(
    "The game crashed whilst",
    "net.minecraft.launchwrapper.Launch",
    "# A fatal error has been detected by the Java Runtime Environment:",
    "---- Minecraft Crash Report ----",
    "A detailed walkthrough of the error",
    "launchermeta.mojang.com",
    "Running launcher core",
    "Native Launcher Version:",
    "[Client thread/INFO]: Setting user:",
    "[Client thread/INFO]: (Session ID is",
    "MojangTricksIntelDriversForPerformance",
    "[DefaultDispatcher-worker-1] INFO Installer",
    "[DefaultDispatcher-worker-1] ERROR Installer",
    "net.minecraftforge",
    "club.sk1er",
    "gg.essential",
    "View crash report"
)

fun filterText(_raw: String, replacement: String): String {
    var raw = _raw
    raw = PASTEBIN_REGEX.replace(raw, replacement)
    raw = SENSITIVE_INFO_REGEX.replace(raw, replacement)
    raw = EMAIL_REGEX.replace(raw, replacement)
    raw = IP_REGEX.replace(raw, replacement)
    raw = URL_REGEX.replace(raw, replacement)
    raw = CODE_BLOCK_REGEX.replace(raw, replacement)
    raw = WINDOWS_MAC_USERNAME_REGEX.replace(raw, replacement)
    raw = LINUX_USERNAME_REGEX.replace(raw, replacement)
    return raw
}

fun getSolutionText(text: String): String {
    val solutions = linkedMapOf<String, MutableList<String>>()

    for (category in responses.keys) {
        for (categoryElement in responses[category]?.jsonArray ?: JsonArray(listOf())) {
            val issue = categoryElement.jsonObject
            val info = issue["info"]?.jsonPrimitive?.contentOrNull ?: continue

            var andCheck = true
            var orCheck = true

            for (checkElement in issue["and"]?.jsonArray ?: JsonArray(listOf())) {
                val check = checkElement.jsonObject

                var outcome = computeMethod(text, check)
                if (check["not"]?.jsonPrimitive?.booleanOrNull == true) outcome = !outcome

                if (!outcome) {
                    andCheck = false
                    break
                }
            }

            if (issue.contains("or")) {
                orCheck = false;
                for (checkElement in issue["or"]!!.jsonArray) {
                    val check = checkElement.jsonObject

                    var outcome = computeMethod(text, check)
                    if (check["not"]?.jsonPrimitive?.booleanOrNull == true) outcome = !outcome

                    if (outcome) {
                        orCheck = true
                        break
                    }
                }
            }

            if (andCheck && orCheck) {
                solutions.computeIfAbsent(category) { mutableListOf() }
                    .add(info)
            }
        }


        val matched = RAM_REGEX.find(text)
        if (matched != null) {
            var ram = Integer.parseInt(matched.groups["ram"]!!.value)
            val type = matched.groups["type"]!!.value
            if (type.equals("G", true)) ram *= 1024
            if (type.equals("K", true)) ram /= 1000

            if (ram > 4096)
                solutions.computeIfAbsent("Recommendations") { mutableListOf() }
                    .add("You are using more than 4GB of ram. This can cause issues and is generally un-needed - even on high-end PCs.")
        }
    }

    val sb = StringBuilder()
    for ((category, infoList) in solutions) {
        sb.append("**$category**\n")
        for (info in infoList) {
            sb.append("$info\n")
        }
        sb.append("\n")
    }

    return sb.toString()
}

private fun computeMethod(text: String, check: JsonObject): Boolean {
    val value = check["value"]!!.jsonPrimitive.content
    if (value == "") return false

    return when ((check["method"]?.jsonPrimitive?.contentOrNull ?: "contains").lowercase()) {
        "contains" -> {
            if (check["exact"]?.jsonPrimitive?.booleanOrNull == true) return text.contains(value, check["ic"]?.jsonPrimitive?.booleanOrNull ?: false)

            for (i in 0..text.length - value.length + 1) {
                if (countDiffs(text.substring(i until i + value.length - 2), value) < (value.length - 1) * (check["leniency"]?.jsonPrimitive?.floatOrNull ?: 0.2f))
                    return true
            }
            return false
        }
        "regex" -> Pattern.compile(value, Pattern.CASE_INSENSITIVE).matcher(text).find()
        "modloaded" -> {
            val id =
                ESSENTIAL_MOD_REGEX.find(text)
                        ?.let { it.groups["id"]?.value }
                    ?: FORGE_MOD_REGEX.find(text)
                        ?.let { it.groups["id"]?.value }
                    ?: return false

            return id.equals(value, true)
        }
        else -> false
    }
}

private fun countDiffs(x: String, y: String): Int {
    var count = abs(x.length - y.length)

    val (x1, y1) = if (x.length < y.length) x to y else y to x

    for (i in x1.indices) {
        if (x1[i] != y1[i]) count++
    }

    return count
}