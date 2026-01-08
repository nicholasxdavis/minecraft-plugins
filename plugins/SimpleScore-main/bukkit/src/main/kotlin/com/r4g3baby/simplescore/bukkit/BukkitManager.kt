package com.r4g3baby.simplescore.bukkit

import com.r4g3baby.simplescore.BukkitPlugin
import com.r4g3baby.simplescore.api.scoreboard.Scoreboard as ScoreboardInterface
import com.r4g3baby.simplescore.api.scoreboard.data.Priority
import com.r4g3baby.simplescore.bukkit.command.MainCmd
import com.r4g3baby.simplescore.bukkit.config.MainConfig
import com.r4g3baby.simplescore.bukkit.listener.PlayerListener
import com.r4g3baby.simplescore.bukkit.protocol.legacy.LegacyProtocolHandler
import com.r4g3baby.simplescore.bukkit.protocol.modern.ModernProtocolHandler
import com.r4g3baby.simplescore.bukkit.protocol.modern.TeamsProtocolHandler
import com.r4g3baby.simplescore.bukkit.protocol.util.ServerVersion
import com.r4g3baby.simplescore.bukkit.scoreboard.ScoreboardTask
import com.r4g3baby.simplescore.bukkit.scoreboard.VarReplacer
import com.r4g3baby.simplescore.bukkit.scoreboard.data.Viewer
import com.r4g3baby.simplescore.bukkit.worldguard.WorldGuardAPI
import com.r4g3baby.simplescore.core.BaseManager
import com.r4g3baby.simplescore.core.scoreboard.Scoreboard
import com.r4g3baby.simplescore.core.scoreboard.ScoreboardScore
import com.r4g3baby.simplescore.core.scoreboard.line.BlankLine
import com.r4g3baby.simplescore.core.scoreboard.line.StaticLine
import com.r4g3baby.simplescore.core.util.translateColorCodes
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BukkitManager(private val plugin: BukkitPlugin) : BaseManager<Player, YamlConfiguration>(plugin) {
    override val varReplacer = VarReplacer(plugin)

    private val trailsAndTailsUpdate3 = ServerVersion.trailsAndTailsUpdate.copy(build = 3)
    private val protocolHandler = if (ServerVersion.isBellow(trailsAndTailsUpdate3)) {
        if (ServerVersion.isBellow(ServerVersion.aquaticUpdate)) {
            LegacyProtocolHandler()
        } else TeamsProtocolHandler()
    } else ModernProtocolHandler()

    override fun onEnable() {
        super.onEnable()

        // Create hardcoded scoreboard on enable
        createHardcodedScoreboard()

        plugin.getCommand(plugin.name).executor = MainCmd(plugin)
        plugin.server.pluginManager.registerEvents(PlayerListener(this), plugin)

        if (config.scoreboardTaskAsync) {
            plugin.scheduler.runTaskTimerAsync(20L, config.taskUpdateTime, ScoreboardTask(this, protocolHandler))
        } else plugin.scheduler.runTaskTimer(20L, config.taskUpdateTime, ScoreboardTask(this, protocolHandler))
        
        // Start hourly key give task (1 hour = 72000 ticks)
        startHourlyKeyTask()
    }
    
    private fun startHourlyKeyTask() {
        // Run countdown timer every second (20 ticks)
        plugin.scheduler.runTaskTimer(20L, 20L) {
            if (keyAllCountdown > 0) {
                keyAllCountdown--
            } else {
                // Reset to 1 hour when countdown reaches 0
                keyAllCountdown = 3600L
                giveHourlyKey()
            }
        }
        
        // Also run the key give task every hour (72000 ticks = 3600 seconds)
        plugin.scheduler.runTaskTimer(72000L, 72000L) {
            giveHourlyKey()
            // Reset countdown when key is given
            keyAllCountdown = 3600L
        }
    }
    
    private fun giveHourlyKey() {
        // Execute command: /crate key give nicholas4x default
        plugin.server.dispatchCommand(plugin.server.consoleSender, "crate key give nicholas4x default")
        
        // Send chat messages to all players with proper color formatting
        val prefix = "&e&lPandora &8» &r"
        val normalText = "&7"
        val specialText = "&e"
        val valueText = "&6"
        
        plugin.server.onlinePlayers.forEach { player ->
            val message1 = translateColorCodes("${prefix}${normalText}Hourly key reward has been given to ${valueText}nicholas4x${normalText}!")
            val message2 = translateColorCodes("${prefix}${specialText}You received a ${valueText}default${specialText} key!")
            player.sendMessage(message1)
            player.sendMessage(message2)
        }
        
        // Also log to console
        plugin.logger.info("Hourly key given to nicholas4x")
    }
    
    fun getKeyAllCountdown(): String {
        val hours = keyAllCountdown / 3600
        val minutes = (keyAllCountdown % 3600) / 60
        val seconds = keyAllCountdown % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDisable() {
        HandlerList.unregisterAll(plugin)

        plugin.server.onlinePlayers.forEach { player ->
            removeViewer(player.uniqueId)
        }

        super.onDisable()
    }

    override lateinit var config: MainConfig
        private set

    override fun loadConfiguration() {
        config = MainConfig(plugin)
        super.loadConfiguration()

        // Clear world scoreboard cache
        worldScoreboardsCache.clear()

        // Clear all scoreboards and create hardcoded one only
        scoreboardsMap.clear()

        // Create hardcoded PandoraBases scoreboard
        createHardcodedScoreboard()

        // Refresh player scoreboards - always use hardcoded PandoraBases scoreboard
        plugin.server.onlinePlayers.forEach { player ->
            val viewer = getOrCreateViewer(player)

            // Always set the hardcoded PandoraBases scoreboard with highest priority
            val pandoraScoreboard = getScoreboard("pandora")
            if (pandoraScoreboard != null) {
                viewer.setScoreboard(pandoraScoreboard, worldProvider, Priority.Highest)
            }

            onViewerChangeWorld(viewer, player.world)
            onViewerChangeLocation(viewer, player.location)
        }
    }
    
    private fun createHardcodedScoreboard() {
        // Title
        val title = StaticLine<Player>("<gradient:#FFE066:#FF8C00><bold>PandoraBases</bold></gradient>", 20)
        
        val scores = mutableListOf<ScoreboardScore<Player>>().apply {
            add(ScoreboardScore(8, BlankLine(), true))
            
            // ✦ Level with fancy text and invisible space
            add(ScoreboardScore(7, StaticLine<Player>("&6◆&e  ʟᴇᴠᴇʟ: &7%player_level%", 20), true))
            
            // ⬢ Base with invisible space
            add(ScoreboardScore(6, StaticLine<Player>("&6⬢&e  ʙᴀꜱᴇ: &7%pandora_base_name%", 20), true))
            
            // ⚔ Kills with invisible space
            add(ScoreboardScore(5, StaticLine<Player>("&6⚙&e  ᴋɪʟʟꜱ: &7%player_kills%", 20), true))
            
            // ⛁ Balance with invisible space
            add(ScoreboardScore(4, StaticLine<Player>("&6⸎&e  ʙᴀʟᴀɴᴄᴇ: &7%vault_eco_balance_formatted%", 20), true))
            
            // ❖ Key All with invisible space
            add(ScoreboardScore(3, StaticLine<Player>("&6❖&e  ᴋᴇʏ ᴀʟʟ: &7%keyall_timer%", 20), true))
            
            add(ScoreboardScore(2, BlankLine(), true))
            
            // ⌁ Ping with invisible space and fancy text
            add(ScoreboardScore(1, StaticLine<Player>("&6✶&e  ᴘɪɴɢ: &7%player_ping%ms", 20), true))
        }
        
        // Create and register the hardcoded scoreboard
        val pandoraScoreboard = Scoreboard("pandora", listOf(title), scores, emptyList())
        scoreboardsMap["pandora"] = pandoraScoreboard
    }

    private val scoreboardsMap: MutableMap<String, Scoreboard<Player>> = ConcurrentHashMap()
    private val viewersMap: MutableMap<UUID, Viewer> = ConcurrentHashMap()
    private val worldProvider = plugin.provider.withContext("world")
    private val regionProvider = plugin.provider.withContext("region")
    
    // KeyAll countdown timer (in seconds, starts at 3600 = 1 hour)
    private var keyAllCountdown: Long = 3600L

    override val scoreboards: List<ScoreboardInterface<Player>>
        get() = scoreboardsMap.values.toList()

    override fun getScoreboard(name: String): ScoreboardInterface<Player>? {
        // Always return hardcoded PandoraBases scoreboard
        return scoreboardsMap["pandora"]
    }

    override fun addScoreboard(scoreboard: ScoreboardInterface<Player>): ScoreboardInterface<Player>? {
        val sb = scoreboard as? Scoreboard<Player> ?: return null
        return scoreboardsMap.put(sb.name, sb)
    }

    override fun removeScoreboard(scoreboard: ScoreboardInterface<Player>): ScoreboardInterface<Player>? {
        val sb = scoreboard as? Scoreboard<Player> ?: return null
        return scoreboardsMap.remove(sb.name)
    }

    override val viewers: List<Viewer>
        get() = viewersMap.values.toList()

    override fun getViewer(uniqueID: UUID): Viewer? {
        return viewersMap[uniqueID]
    }

    internal fun getOrCreateViewer(player: Player): Viewer {
        return getViewer(player.uniqueId) ?: createViewer(player)
    }

    internal fun createViewer(player: Player): Viewer {
        return Viewer(player).also { viewer ->
            viewersMap[player.uniqueId] = viewer

            // Immediately set the hardcoded PandoraBases scoreboard
            val pandoraScoreboard = getScoreboard("pandora")
            if (pandoraScoreboard != null) {
                viewer.setScoreboard(pandoraScoreboard, worldProvider, Priority.Highest)
            }

            plugin.scheduler.runTaskAsync {
                // todo: fetch viewer information from storage
            }
        }
    }

    internal fun removeViewer(uniqueID: UUID): Viewer? {
        return viewersMap.remove(uniqueID).also { viewer ->
            val player = viewer?.reference?.get()
            if (player != null && player.isOnline) {
                protocolHandler.removeObjective(player)
            }
        }
    }

    internal fun onViewerChangeWorld(viewer: Viewer, world: World) {
        val player = viewer.reference.get() ?: return
        // Always show hardcoded PandoraBases scoreboard with highest priority
        val pandoraScoreboard = getScoreboard("pandora")
        if (pandoraScoreboard != null) {
            viewer.setScoreboard(pandoraScoreboard, worldProvider, Priority.Highest)
        }
    }

    internal fun onViewerChangeLocation(viewer: Viewer, location: Location) {
        // Don't override with region scoreboards - keep hardcoded one
        // Region scoreboards are disabled for hardcoded scoreboard
    }

    private val worldScoreboardsCache = mutableMapOf<String, List<Scoreboard<Player>>>()
    private fun getForWorld(world: World): List<Scoreboard<Player>> {
        // Always return empty list - we use hardcoded scoreboard only
        return emptyList()
    }
}