package dev.isxander.crashhelper.extensions

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.isxander.crashhelper.TEST_SERVER_ID
import dev.isxander.crashhelper.invalidateResponses
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.rest.builder.message.create.embed

object CommandManager : Extension() {
    override val name: String = "commands"

    override suspend fun setup() {
        slashCommand {
            name = "invalidate"
            description = "Invalidates response cache."

            autoAck = AutoAckType.PUBLIC

            check(hasPermission(Permission.Administrator))

            action {
                invalidateResponses()

                publicFollowUp {
                    embed {
                        description = "Successfully invalidated response cache."
                        color = Color(0xff4747)
                    }
                }
            }
        }

        slashCommand {
            name = "invite"
            description = "Gets a link to invite the bot."

            autoAck = AutoAckType.PUBLIC

            action {
                publicFollowUp {
                    embed {
                        description = "https://short.isxander.dev/crashhelper-bot"
                        color = Color(0xff4747)
                    }
                }
            }
        }

        slashCommand {
            name = "vip"
            description = "Get information about VIP benefits."

            autoAck = AutoAckType.PUBLIC

            action {
                publicFollowUp {
                    embed {
                        title = "VIP Benefits"
                        color = Color(0xff4747)
                        description = "Get VIP by sending \$5+ to my [paypal](https://paypal.me/earthmonster) and message me @ **isXander#0162!**"
                        field {
                            name = "Image Scanning (free for now)"
                            value = "Crash Helper will scan posted images for log texts and give insightful solutions and recommendations."
                        }
                    }
                }
            }
        }
    }
}