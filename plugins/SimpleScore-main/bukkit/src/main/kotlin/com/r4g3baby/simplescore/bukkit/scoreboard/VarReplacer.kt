package com.r4g3baby.simplescore.bukkit.scoreboard

import com.r4g3baby.simplescore.BukkitPlugin
import com.r4g3baby.simplescore.api.scoreboard.VarReplacer
import com.r4g3baby.simplescore.bukkit.util.Adventure
import com.r4g3baby.simplescore.bukkit.util.lazyReplace
import com.r4g3baby.simplescore.core.util.translateColorCodes
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class VarReplacer(private val plugin: BukkitPlugin) : VarReplacer<Player> {
    private val usePlaceholderAPI = plugin.server.pluginManager.getPlugin("PlaceholderAPI") != null
    
    // LevelPlugin integration
    private val levelPlugin = plugin.server.pluginManager.getPlugin("LevelPlugin")
    private var levelAPIMethod: java.lang.reflect.Method? = null
    
    // PandoraBases integration
    private val pandoraBasesPlugin = plugin.server.pluginManager.getPlugin("PandoraBases")
    private var pandoraBasesGetFactionMethod: java.lang.reflect.Method? = null
    private var pandoraBasesGetTagMethod: java.lang.reflect.Method? = null
    
    init {
        // Setup LevelPlugin API
        try {
            if (levelPlugin != null) {
                val getAPIMethod = levelPlugin.javaClass.getMethod("getAPI")
                val apiInstance = getAPIMethod.invoke(levelPlugin)
                if (apiInstance != null) {
                    levelAPIMethod = apiInstance.javaClass.getMethod("getLevel", Player::class.java)
                }
            }
        } catch (e: Exception) {
            // LevelPlugin not available or API changed
        }
        
        // Setup PandoraBases API
        try {
            if (pandoraBasesPlugin != null) {
                val getFPlayersMethod = pandoraBasesPlugin.javaClass.getMethod("getFPlayers")
                val fPlayersInstance = getFPlayersMethod.invoke(pandoraBasesPlugin)
                if (fPlayersInstance != null) {
                    pandoraBasesGetFactionMethod = fPlayersInstance.javaClass.getMethod("getByPlayer", Player::class.java)
                    val fPlayerClass = Class.forName("com.massivecraft.factions.FPlayer")
                    pandoraBasesGetTagMethod = fPlayerClass.getMethod("getTag")
                }
            }
        } catch (e: Exception) {
            // PandoraBases not available or API changed
        }
    }

    override fun replace(text: String, viewer: Player): String {
        var result = if (usePlaceholderAPI) PlaceholderAPI.setPlaceholders(viewer, text) else text

        // Get level from LevelPlugin
        val playerLevel = try {
            if (levelAPIMethod != null && levelPlugin != null) {
                val apiInstance = levelPlugin.javaClass.getMethod("getAPI").invoke(levelPlugin)
                (levelAPIMethod!!.invoke(apiInstance, viewer) as? Int)?.toString() ?: "0"
            } else "0"
        } catch (e: Exception) {
            "0"
        }
        
        // Get base name from PandoraBases
        val baseName = try {
            if (pandoraBasesGetFactionMethod != null && pandoraBasesGetTagMethod != null && pandoraBasesPlugin != null) {
                val fPlayersInstance = pandoraBasesPlugin.javaClass.getMethod("getFPlayers").invoke(pandoraBasesPlugin)
                val fPlayer = pandoraBasesGetFactionMethod!!.invoke(fPlayersInstance, viewer)
                if (fPlayer != null) {
                    val hasFactionMethod = fPlayer.javaClass.getMethod("hasFaction")
                    if (hasFactionMethod.invoke(fPlayer) as Boolean) {
                        pandoraBasesGetTagMethod!!.invoke(fPlayer) as? String ?: "None"
                    } else "None"
                } else "None"
            } else "None"
        } catch (e: Exception) {
            "None"
        }
        
        // Get kills from PandoraBases
        val playerKills = try {
            if (pandoraBasesGetFactionMethod != null && pandoraBasesPlugin != null) {
                val fPlayersInstance = pandoraBasesPlugin.javaClass.getMethod("getFPlayers").invoke(pandoraBasesPlugin)
                val fPlayer = pandoraBasesGetFactionMethod!!.invoke(fPlayersInstance, viewer)
                if (fPlayer != null) {
                    val getKillsMethod = fPlayer.javaClass.getMethod("getKills")
                    (getKillsMethod.invoke(fPlayer) as? Int)?.toString() ?: "0"
                } else "0"
            } else "0"
        } catch (e: Exception) {
            "0"
        }

        result = result.lazyReplace("%player_name%") { viewer.name }
            .lazyReplace("%player_displayname%") { viewer.displayName }
            .lazyReplace("%player_uuid%") { viewer.uniqueId.toString() }
            .lazyReplace("%player_level%") { playerLevel } // Use LevelPlugin level
            .lazyReplace("%player_gamemode%") { viewer.gameMode.name.lowercase().replaceFirstChar { it.titlecase() } }
            .lazyReplace("%player_health%") { viewer.health.roundToInt().toString() }
            .lazyReplace("%player_maxhealth%") { viewer.maxHealth.roundToInt().toString() }
            .lazyReplace("%player_hearts%") {
                val hearts = min(10, max(0, ((viewer.health / viewer.maxHealth) * 10).roundToInt()))
                "&c${"❤".repeat(hearts)}&0${"❤".repeat(10 - hearts)}"
            }
            .lazyReplace("%player_world%") { viewer.world.name }
            .lazyReplace("%player_world_online%") { viewer.world.players.size.toString() }
            .lazyReplace("%server_online%") { viewer.server.onlinePlayers.size.toString() }
            .lazyReplace("%server_maxplayers%") { viewer.server.maxPlayers.toString() }
            .lazyReplace("%player_kills%") { playerKills } // PandoraBases kills
            .lazyReplace("%pandora_base_name%") { baseName } // PandoraBases base name
            .lazyReplace("%player_ping%") { 
                try {
                    val pingMethod = viewer.javaClass.getMethod("getPing")
                    pingMethod.invoke(viewer).toString()
                } catch (e: Exception) {
                    "0"
                }
            } // Player ping
            .lazyReplace("%keyall_timer%") { 
                // Get countdown from manager
                try {
                    val manager = plugin.manager as? com.r4g3baby.simplescore.bukkit.BukkitManager
                    manager?.getKeyAllCountdown() ?: "00:00:00"
                } catch (e: Exception) {
                    "00:00:00"
                }
            } // KeyAll countdown timer

        result = Adventure.parseToString(result)
        return translateColorCodes(result)
    }
}