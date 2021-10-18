package dev.isxander.crashhelper.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import kotlinx.coroutines.flow.collect

object PresenceManager : Extension() {
    override val name: String = "Presence Manager"

    override suspend fun setup() {
        event<ReadyEvent> {
            action {
                kord.editPresence {
                    status = PresenceStatus.Idle
                    competing("watching crashes")
                }
            }
        }

        event<GuildCreateEvent> {
            action {
                kord.editPresence {
                    status = PresenceStatus.Online
                    watching("my plugs getting pulled out")
                }
            }
        }

        event<GuildDeleteEvent> {
            action {
                kord.editPresence {
                    status = PresenceStatus.DoNotDisturb
                    listening("to sad music")
                }
            }
        }
    }

    private suspend fun updateActivity() {
        kord.editPresence {
            status = PresenceStatus.DoNotDisturb

            var count = 0
            kord.guilds.collect { count += it.memberCount ?: 0 }
            watching("over $count people.")
        }
    }

}
