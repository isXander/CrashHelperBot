package dev.isxander.crashhelper

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.env
import dev.isxander.crashhelper.extensions.CommandManager
import dev.isxander.crashhelper.extensions.ImageScanner
import dev.isxander.crashhelper.extensions.PresenceManager
import dev.isxander.crashhelper.extensions.TextScanner
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val TEST_SERVER_ID = Snowflake(873865925361803274)

private val TOKEN = env("CRASHELPER_TOKEN")
    ?: error("Could not retrieve token!")

val LOGGER: Logger = KotlinLogging.logger("Crash Helper")

suspend fun main() {
    val bot = ExtensibleBot(TOKEN) {
        slashCommands {
            enabled = true
        }

        intents {
            +Intent.GuildMessages
            +Intent.GuildMessageReactions
            +Intent.Guilds
            +Intent.GuildEmojis
        }

        extensions {
            add { PresenceManager }
            add { CommandManager }
            add { ImageScanner }
            add { TextScanner }
        }
    }

    bot.start()
}

