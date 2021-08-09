package dev.isxander.crashhelper.utils

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

object Embeds {

    fun success(message: String?): MessageEmbed {
        val embed = EmbedBuilder()
        embed.setDescription(message)
        embed.setColor(Color(0, 255, 0))
        return embed.build()
    }

    fun warning(message: String?): MessageEmbed {
        val embed = EmbedBuilder()
        embed.setDescription(message)
        embed.setColor(Color(255, 255, 0))
        return embed.build()
    }

    fun error(message: String?): MessageEmbed {
        val embed = EmbedBuilder()
        embed.setDescription(message)
        embed.setColor(Color(255, 0, 0))
        return embed.build()
    }

    fun info(message: String?): MessageEmbed {
        val embed = EmbedBuilder()
        embed.setDescription(message)
        embed.setColor(Color(250, 250, 250))
        return embed.build()
    }

}