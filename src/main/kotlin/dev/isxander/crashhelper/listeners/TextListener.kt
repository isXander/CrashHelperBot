package dev.isxander.crashhelper.listeners

import dev.isxander.crashhelper.LOG_TEXT
import dev.isxander.crashhelper.PASTEBIN_REGEX
import dev.isxander.crashhelper.filterText
import dev.isxander.crashhelper.getSolutionText
import dev.isxander.crashhelper.utils.HttpsUtils
import dev.isxander.crashhelper.utils.Multithreading
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.awt.Color
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import kotlin.collections.ArrayList

object TextListener : ListenerAdapter() {

    private val TEXT_EXTENSIONS = setOf(
        "txt", "log", "json", "java",
        "yml", "kt", "toml", "js", "md",
        "gitignore", "gitattributes", "gradle",
        "properties", "bat"
    )

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.isBot || event.isWebhookMessage) return
        Multithreading.runAsync {
            val rawContent = event.message.contentRaw
            var texts = arrayListOf<String>()

            val attachments = event.message.attachments.filter {
                for (ending in TEXT_EXTENSIONS) {
                    if (it.fileName.endsWith(".$ending")) return@filter true
                }
                return@filter false
            }.toList()

            for (attachment in attachments) {
                attachment.retrieveInputStream().get().use {
                    texts.add(BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n")))
                }
            }

            PASTEBIN_REGEX
                .matcher(rawContent)
                .results()
                .map { it.group() }
                .forEach { url ->
                    var key = url.substring(url.lastIndexOf('/'))
                    key = if (url.contains("paste.ee")) "r$key" else "raw$key"

                    texts.add(HttpsUtils.getString(url.substring(0, url.lastIndexOf('/')) + "/$key")!!)
                }


            texts = ArrayList(texts.filter {
                for (log in LOG_TEXT) {
                    if (it.contains(log)) return@filter true
                }
                return@filter false
            })

            if (texts.isNotEmpty()) event.message.delete().reason("Replaced by XanderBot").queue()
            else return@runAsync

            val eb = EmbedBuilder()
            eb.setColor(Color(0xff4747))

            val censoredContent = filterText(rawContent, "").trim()
            if (censoredContent.isNotBlank())
                eb.setDescription("*\"$censoredContent\"*")

            eb.setTitle("Log Files Uploaded")
            eb.setAuthor(event.member?.effectiveName ?: "Unknown", "https://short.isxander.dev/crashhelper-bot", event.author.avatarUrl)
            eb.setFooter("Powered by isXander/MinecraftIssues", "https://dl.isxander.dev/logos/github/mark/normal.png")

            for (_text in texts) {
                val text = filterText(_text, "[redacted]")
                val hastebin = HttpsUtils.uploadToHastebin(text, text.count{ it == '\n' } > 40000)

                val fieldName = hastebin ?: "**Failed to upload text to hastebin!**"

                var solution = getSolutionText(text)
                if (solution.isEmpty()) solution = "Nothing found."

                println(solution)

                eb.addField("**$fieldName**", solution, false)
            }

            event.channel.sendMessageEmbeds(eb.build()).queue()
        }
    }



}