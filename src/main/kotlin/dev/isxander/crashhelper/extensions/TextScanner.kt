package dev.isxander.crashhelper.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.download
import com.kotlindiscord.kord.extensions.utils.runSuspended
import dev.isxander.crashhelper.utils.*
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.request.*
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking

object TextScanner : Extension() {

    private val TEXT_EXTENSIONS = setOf(
        "txt", "log", "json", "java",
        "yml", "kt", "toml", "js", "md",
        "gitignore", "gitattributes", "gradle",
        "properties", "bat"
    )

    override val name: String = "Text Scanner"
    private val dispatcher = newFixedThreadPoolContext(8, "Text Scanner")

    override suspend fun setup() {
        event<MessageCreateEvent> {
            booleanCheck { !(it.member?.isBot ?: false) }
            booleanCheck { it.message.webhookId == null }

            action {
                runSuspended(dispatcher) {
                    val content = event.message.content
                    val texts = mutableListOf<String>()

                    event.message.attachments
                        .filter { TEXT_EXTENSIONS.stream().anyMatch { extension -> it.filename.endsWith(".$extension") } }
                        .map { it.download().decodeToString() }
                        .filter { LOG_TEXT.any(it::contains) }
                        .forEach { texts.add(it) }

                    PASTEBIN_REGEX
                        .findAll(content)
                        .map { it.groups[0]!!.value }
                        .map { url ->
                            var key = url.substring(url.lastIndexOf('/'))
                            key = if (url.contains("paste.ee")) "r$key" else "raw$key"

                            runBlocking { http.get<String>(url.substring(0, url.lastIndexOf('/')) + "/$key") }
                        }
                        .filter { LOG_TEXT.any(it::contains) }
                        .forEach { texts.add(it) }

                    CODE_BLOCK_REGEX
                        .findAll(content)
                        .map { it.groups["code"]!!.value }
                        .filter { LOG_TEXT.any(it::contains) }
                        .forEach { texts.add(it) }

                    if (texts.isNotEmpty()) event.message.delete("Replaced by CrashHelper")
                    else return@runSuspended

                    event.message.channel.createMessage {
                        embed {
                            title = "Text Scanning"
                            color = Color(0xff4747)

                            val censoredContent = filterText(content, "")
                                .replace(" +".toRegex(), " ")
                                .trim()
                            if (censoredContent.isNotBlank()) description = "**$censoredContent**"

                            author {
                                name = event.member?.displayName
                                url = "https://short.isxander.dev/crashhelper-bot"
                                icon = event.member?.avatar?.url
                            }

                            footer {
                                text = "Powered by isXander/MinecraftIssues"
                                icon = "https://dl.isxander.dev/logos/github/mark/normal.png"
                            }

                            for (_text in texts) {
                                val text = filterText(_text, "[redacted]")
                                val hastebin = uploadToHastebin(text, text.count{ it == '\n' } > 40000) + ".txt"

                                var solution = getSolutionText(text)
                                if (solution.isEmpty()) solution = "Nothing found."

                                field {
                                    name = "**$hastebin**"
                                    value = solution
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}