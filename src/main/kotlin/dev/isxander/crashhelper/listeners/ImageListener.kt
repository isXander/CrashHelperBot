package dev.isxander.crashhelper.listeners

import dev.isxander.crashhelper.CrashHelper
import dev.isxander.crashhelper.LOG_TEXT
import dev.isxander.crashhelper.getSolutionText
import dev.isxander.crashhelper.utils.HttpsUtils
import dev.isxander.crashhelper.utils.Multithreading
import dev.isxander.crashhelper.utils.scaleImageToPixelCount
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageEmbedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.sourceforge.tess4j.Tesseract
import java.awt.Color
import java.awt.image.BufferedImage
import java.lang.Exception
import javax.imageio.ImageIO

object ImageListener : ListenerAdapter() {

    private val tesseract = Tesseract()

    init {
        tesseract.setTessVariable("user_defined_dpi", "300")
        tesseract.setLanguage("eng")
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        try {
            Multithreading.runAsync {
                event.message.attachments
                    .filter { it.isImage }
                    .map { ImageIO.read(it.retrieveInputStream().get()) }
                    .forEach { handleImage(it, event.message.id, event.channel) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGuildMessageEmbed(event: GuildMessageEmbedEvent) {
        CrashHelper.logger.info("embed!")
        try {
            Multithreading.runAsync {
                event.messageEmbeds
                    .filter { it.image != null }
                    //.filter { HttpsUtils.getContentLength(it.image!!.url!!) / 1000 / 1000 < 5 }
                    .map { ImageIO.read(HttpsUtils.getBytes(it.image!!.url!!).inputStream()) }
                    .forEach { handleImage(it, event.messageId, event.channel) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleImage(img: BufferedImage, messageId: String, channel: TextChannel) {
        val loading = CrashHelper.JDA.getEmoteById("873882132899061811")
        if (loading != null) channel.addReactionById(messageId, loading).queue()
        else CrashHelper.logger.error("No loading emote!")

        val text = tesseract.doOCR(scaleImageToPixelCount(img, 1920*1080))

        if (loading != null) channel.clearReactionsById(messageId, loading).queue()

        if (!LOG_TEXT.any { text.contains(it, true) }) return

        val eb = EmbedBuilder()
        eb.setTitle("Image Scanning")
        eb.setDescription(getSolutionText(text))
        eb.setColor(Color(0xff4747))
        eb.setFooter("Powered by isXander/MinecraftIssues", "https://dl.isxander.dev/logos/github/mark/normal.png")

        channel.sendMessageEmbeds(eb.build())
            .referenceById(messageId)
            .mentionRepliedUser(true)
            .queue()
    }

}