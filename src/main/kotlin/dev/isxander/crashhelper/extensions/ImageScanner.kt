package dev.isxander.crashhelper.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.download
import com.kotlindiscord.kord.extensions.utils.runSuspended
import dev.isxander.crashhelper.utils.*
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.builder.message.create.embed
import io.ktor.client.request.*
import kotlinx.coroutines.newFixedThreadPoolContext
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object ImageScanner : Extension() {

    override val name: String = "Image Scanner"

    private val dispatcher = newFixedThreadPoolContext(5, "Image Scanner")
    private val tesseract = Tesseract()

    override suspend fun setup() {
        tesseract.setTessVariable("user_defined_dpi", "300")
        tesseract.setLanguage("eng")

        event<MessageCreateEvent> {
            action {
                runSuspended(dispatcher) {
                    event.message.attachments
                        .filter { it.isImage }
                        .filter { it.size / 1000 / 1000 < 5 }
                        .map { it.download().inputStream().use(ImageIO::read) }
                        .forEach { handleImage(it, event.message) }
                }
            }
        }

        event<MessageUpdateEvent> {
            action {
                runSuspended(dispatcher) {
                    event.message.asMessage().embeds
                        .filter { it.image != null }
                        .filter { getContentLength(it.image!!.url!!) / 1000 / 1000 < 5 }
                        .map { http.get<ByteArray>(it.image!!.url!!).inputStream().use(ImageIO::read) }
                        .forEach { handleImage(it, event.message.asMessage()) }
                }
            }
        }
    }

    private suspend fun handleImage(img: BufferedImage, message: Message) {
        val imgText = tesseract.doOCR(scaleImageToPixelCount(img, 1920*1080))

        if (!LOG_TEXT.any { imgText.contains(it, true) }) return

        message.channel.createMessage {
            embed {
                title = "Image Scanning"
                description = getSolutionText(imgText)
                color = Color(0xff4747)
                footer {
                    text = "Powered by isXander/MinecraftIssues"
                    icon = "https://dl.isxander.dev/logos/github/mark/normal.png"
                }
            }

            messageReference = message.id
        }
    }

}