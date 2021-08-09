package dev.isxander.crashhelper.command.impl

import dev.isxander.crashhelper.command.ICommand
import dev.isxander.crashhelper.utils.Embeds
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

class InviteCommand : ICommand {

    override fun handle(event: SlashCommandEvent) {
        event.replyEmbeds(Embeds.success("https://short.isxander.dev/crashhelper-bot")).queue()
    }

    override val invoke: String = "invite"
    override val description: String = "Get a link to invite this bot to your server!"

}