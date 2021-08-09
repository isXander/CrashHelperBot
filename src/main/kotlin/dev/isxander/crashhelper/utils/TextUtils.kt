package dev.isxander.crashhelper

import kotlinx.serialization.json.*
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs


val IP_REGEX: Pattern = Pattern.compile("([0-9]{1,3})\\\\.([0-9]{1,3})\\\\.([0-9]{1,3})\\\\.([0-9]{1,3})", Pattern.CASE_INSENSITIVE)
val URL_REGEX: Pattern = Pattern.compile("https?://(www\\\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\\\.[a-zA-Z0-9()]{1,6}\\\\b([-a-zA-Z0-9()@:%_+.~#?&//=]*)", Pattern.CASE_INSENSITIVE)
val SENSITIVE_INFO_REGEX: Pattern = Pattern.compile("(\"access_key\":\".+\"|api.sk1er.club/auth|LoginPacket|SentryAPI.cpp|\"authHash\":|\"hash\":\"|--accessToken \\S+|\\(Session ID is token:|Logging in with details: |Server-Hash: |Checking license key :|USERNAME=.*|https://api\\.hypixel\\.net/.+(\\?key=|&key=))", Pattern.CASE_INSENSITIVE)
val EMAIL_REGEX: Pattern = Pattern.compile("[a-zA-Z0-9_.+-]{1,50}@[a-zA-Z0-9-]{1,50}\\.[a-zA-Z0-9-.]{1,10}", Pattern.CASE_INSENSITIVE)
val PASTEBIN_REGEX: Pattern = Pattern.compile("(?:https?://)?(?<domain>paste\\.ee|pastebin\\.com|has?tebin\\.com|hasteb\\.in|hst\\.sh)/(?:raw/|p/)?([\\w-.]+)", Pattern.CASE_INSENSITIVE)
val RAM_REGEX: Pattern = Pattern.compile("-Xmx(?<ram>\\d+)(?<type>[GMK])", Pattern.CASE_INSENSITIVE)

val FORGE_MOD_REGEX: Pattern = Pattern.compile("(?<state>(?:U?L?C?H?I?J?A?D?E?)+)\\t(?<id>(?: ?[\\w-]+)+)\\{(?<version>(?:[0-9-\\w]\\.*)+)\\} \\[(?<name>(?: ?[\\w-]+)+)\\] \\((?<file>(?: ?(?:[^/<>:\\\"\\\\|?* ])+)+)\\)")
val ESSENTIAL_MOD_REGEX: Pattern = Pattern.compile("(?<state>(?:U?L?C?H?I?J?A?D?E?)+) +\\| +(?<id>(?: ?[\\w-]+)+) +\\| +(?<version>(?:[0-9-\\w]\\.*)+) +\\| +(?<file>(?: ?(?:[^/<>:\\\"\\\\|?* ])+)+)")

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
    raw = PASTEBIN_REGEX.matcher(raw).replaceAll(replacement)
    raw = SENSITIVE_INFO_REGEX.matcher(raw).replaceAll(replacement)
    raw = EMAIL_REGEX.matcher(raw).replaceAll(replacement)
    raw = IP_REGEX.matcher(raw).replaceAll(replacement)
    raw = URL_REGEX.matcher(raw).replaceAll(replacement)
    return raw
}

fun getSolutionText(text: String): String {
    val solutions = linkedMapOf<String, MutableList<String>>()

    for (category in CrashHelper.responses.keys) {
        for (categoryElement in CrashHelper.responses[category]) {
            val issue = JsonObjectExt(categoryElement.asJsonObject)
            val info = issue["info", ""]!!
            if (info.isEmpty()) continue

            var andCheck = true
            var orCheck = true

            for (checkElement in issue["and", JsonArray()]!!) {
                val check = JsonObjectExt(checkElement.asJsonObject)

                var outcome = computeMethod(text, check)
                if (check["not", false]) outcome = !outcome

                if (!outcome) {
                    andCheck = false
                    break
                }
            }

            if (issue.has("or")) {
                orCheck = false;
                for (checkElement in issue["or"]!!) {
                    val check = JsonObjectExt(checkElement.asJsonObject)

                    var outcome = computeMethod(text, check)
                    if (check["not", false]) outcome = !outcome

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

        val matcher = RAM_REGEX.matcher(text)
        if (matcher.find()) {
            var ram = Integer.parseInt(matcher.group("ram"))
            val type = matcher.group("type")
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
                if (countDiffs(text.substring(i until i + value.length - 2), value) < (value.length - 1) * check["leniency", 0.2f])
                    return true
            }
            return false
        }
        "regex" -> Pattern.compile(value, Pattern.CASE_INSENSITIVE).matcher(text).find()
        "modloaded" -> {
            val id =
                ESSENTIAL_MOD_REGEX.matcher(text)
                        .let { if (it.find()) it.group("id") else null }
                    ?: FORGE_MOD_REGEX.matcher(text)
                        .let { if (it.find()) it.group("id") else null }
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

fun costOfSubstitution(a: Char, b: Char): Int {
    return if (a == b) 0 else 1
}

fun min(vararg numbers: Int): Int {
    return Arrays.stream(numbers)
        .min().orElse(Int.MAX_VALUE)
}