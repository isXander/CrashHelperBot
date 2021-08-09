package dev.isxander.crashhelper

import dev.isxander.crashhelper.command.CommandHandler
import dev.isxander.crashhelper.listeners.ImageListener
import dev.isxander.crashhelper.listeners.TextListener
import dev.isxander.crashhelper.utils.HttpsUtils
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

lateinit var client: Kord
    private set

private var lastCacheUpdate = System.currentTimeMillis()
private var responseCache: JsonObject? = null
val responses: JsonObject
    get() {
        if (responseCache == null || System.currentTimeMillis() - lastCacheUpdate > 600000) {
            invalidateResponses()
            responseCache = Json.decodeFromString(HttpsUtils.getString("https://raw.githubusercontent.com/isXander/MinecraftIssues/main/issues.json"))
            lastCacheUpdate = System.currentTimeMillis()
        }

        return responseCache!!
    }

val logger: Logger = LoggerFactory.getLogger("Crash Helper")
val dotenv = dotenv()

// Main Token: ODY5MTgxMDc5MDkxODEwMzY1.YP6d9Q.ZshEoVQ5TyC2KP5wxlqKDZ3h-Qs
// Beta Token: ODczODY1MDg0NDYyNTcxNTMw.YQ-oRw.8X5HLm9xd4wQdoVkQ5Qlu-mTEdI

suspend fun main() {
    client = Kord(dotenv["CRASHELPER_TOKEN"])
    updateActivity()
    printGuildsSummary()
    JDA = JDABuilder.createLight(
        "ODczODY1MDg0NDYyNTcxNTMw.YQ-oRw.8X5HLm9xd4wQdoVkQ5Qlu-mTEdI",
        GatewayIntent.GUILD_EMOJIS,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_WEBHOOKS,)
        .addEventListeners(this, CommandHandler, TextListener, ImageListener)
        .enableCache(CacheFlag.EMOTE)
        .setActivity(Activity.watching("at least 1 server."))
        .setStatus(OnlineStatus.DO_NOT_DISTURB)
        .build()
    JDA.awaitReady()
    updateActivity()

    CommandHandler.init()

    printGuildsSummary()
}

private fun updateActivity() {
    runBlocking { client.editPresence {
        status = PresenceStatus.DoNotDisturb

        watching("over ${client.guilds.toList().sumOf { it.memberCount ?: 0 }} people.")
    }}
}

override fun onGuildJoin(event: GuildJoinEvent) {
    updateActivity()
    logger.info("${event.guild.name} just added Crash Helper!")
}

override fun onGuildLeave(event: GuildLeaveEvent) {
    updateActivity()
    logger.info("${event.guild.name} just removed Crash Helper!")
}

private fun printGuildsSummary() {
    logger.info("--- GUILDS SUMMARY ---")
    for (guild in JDA.guilds) {
        logger.info("  ${guild.name} - ${guild.memberCount} members")
    }
    logger.info("Watching over a total of ${JDA.guilds.sumOf { it.memberCount }} people")
}

fun invalidateResponses() {
    System.gc() // might as well
    responseCache = null
}