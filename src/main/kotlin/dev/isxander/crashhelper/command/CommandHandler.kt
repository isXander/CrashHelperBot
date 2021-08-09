package dev.isxander.crashhelper.command

import dev.isxander.crashhelper.CrashHelper
import dev.isxander.crashhelper.command.impl.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction

object CommandHandler : ListenerAdapter() {

    private lateinit var commandList: CommandListUpdateAction
    private val commands = hashMapOf<String, ICommand>()

    fun init() {
        addCommand(InvalidateResponsesCommand())
        addCommand(VipCommand())
        addCommand(VipCommand())

        registerCommands()
    }

    private fun needsUpdate(): Boolean =
        this.commands.map { it.key } != CrashHelper.JDA.retrieveCommands().complete().map { it.name }


    private fun registerCommands() {
        if (!needsUpdate()) return
        val commandList = CrashHelper.JDA.updateCommands()

        for ((_, command) in commands) {
            val data = CommandData(command.invoke, command.description)
            if (command.options.isNotEmpty()) data.addOptions(command.options)
            else data.addSubcommands(command.subcommands).addSubcommandGroups(command.subcommandsGroup)

            commandList.addCommands(data)
        }

        commandList.queue()
    }

    private fun addCommand(command: ICommand) {
        commands[command.invoke] = command
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        val command = commands[event.name]

        if (command != null) {
            var hasPermission = true
            for (perm in command.requiredPermissions) {
                if (event.member?.hasPermission(perm) == false) {
                    hasPermission = false
                    break
                }
            }
            if (!hasPermission) hasPermission = event.member?.hasPermission(Permission.ADMINISTRATOR) ?: false
            if (!hasPermission) hasPermission = event.member?.id == "320596098689400833"

            if (hasPermission) {
                command.handle(event)
            }
        }
    }

}