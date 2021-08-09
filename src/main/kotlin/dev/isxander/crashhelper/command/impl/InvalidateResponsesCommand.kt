package dev.isxander.crashhelper.command.impl

import dev.isxander.crashhelper.CrashHelper
import dev.isxander.crashhelper.command.ICommand
import dev.isxander.crashhelper.utils.Embeds

class InvalidateResponsesCommand : ICommand {
    override fun handle(event: SlashCommandEvent) {
        if (event.user.id != "320596098689400833") {
            event.replyEmbeds(Embeds.error("You need to be isXander to run this command.")).queue()
        } else {
            event.replyEmbeds(Embeds.success("Successfully invalidated response cache. It will be re-downloaded the next time a log is posted.")).queue()
            CrashHelper.invalidateResponses()
        }
    }

    override val invoke: String = "invalidate"
    override val description: String = "isXander only command!"

    override val options: List<OptionData> = listOf(
        OptionData(bla bla bla)
    )

}