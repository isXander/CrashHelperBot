package dev.isxander.crashhelper.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData

interface ICommand {

    fun handle(event: SlashCommandEvent)

    val invoke: String
    val description: String

    val options: List<OptionData>
        get() = listOf()

    val subcommands: List<SubcommandData>
        get() = listOf()

    val subcommandsGroup: List<SubcommandGroupData>
        get() = listOf()

    val requiredPermissions: List<Permission>
        get() = listOf()

}