package dev.isxander.crashhelper.command.impl

import dev.isxander.crashhelper.command.ICommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import java.awt.Color

class VipCommand : ICommand {

    override fun handle(event: SlashCommandEvent) {
        val eb = EmbedBuilder()

        eb.setTitle("VIP Benefits")
        eb.setColor(Color(0xff4747))
        eb.setDescription("Get VIP by sending $5+ to my [paypal](https://paypal.me/earthmonster) and message me @ **isXander#0162!**")

        eb.addField(
            "Image Scanning (free for now)",
            "Crash Helper will scan posted images for log texts as well as uploaded files and hastebins.",
            false
        )

        event.replyEmbeds(eb.build()).queue()
    }

    override val invoke: String = "vip"
    override val description: String = "Displays information about VIP benefits."

}