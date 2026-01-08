package com.massivecraft.factions.util;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;

public class WarmUpUtil {

    /**
     * @param player         The player to notify.
     * @param translationKey The translation key used for notifying.
     * @param action         The action, inserted into the notification message.
     * @param runnable       The task to run after the delay. If the delay is 0, the task is instantly ran.
     * @param delay          The time used, in seconds, for the delay.
     *                       <p>
     *                       note: for translations: %s = action, %d = delay
     */
    public static void process(final FPlayer player, Warmup warmup, TL translationKey, String action, final Runnable runnable, long delay) {
        if (delay > 0) {
            if (player.isWarmingUp()) {
                player.msg(TL.WARMUPS_ALREADY);
                return;
            }

            player.msg(translationKey.format(action, delay));
            
            // For HOME warmup, show countdown with hook notifications
            if (warmup == Warmup.HOME && delay > 0) {
                startCountdownNotifications(player, action, delay, runnable, warmup);
            } else {
                int id = Bukkit.getScheduler().runTaskLater(FactionsPlugin.getInstance(), () -> {
                    player.stopWarmup();
                    runnable.run();
                }, delay * 20).getTaskId();
                player.addWarmup(warmup, id);
            }
        } else {
            runnable.run();
        }
    }
    
    /**
     * Start countdown notifications for home teleport using Hook plugin
     */
    private static void startCountdownNotifications(final FPlayer fPlayer, String action, long delaySeconds, final Runnable runnable, Warmup warmup) {
        final org.bukkit.entity.Player player = fPlayer.getPlayer();
        if (player == null) return;
        
        final long[] remainingSeconds = {delaySeconds};
        
        // Initial message is already sent by player.msg() above, so we just start countdown
        
        // Start countdown task
        final int[] countdownTaskId = new int[1];
        countdownTaskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(FactionsPlugin.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !fPlayer.isWarmingUp()) {
                    Bukkit.getScheduler().cancelTask(countdownTaskId[0]);
                    return;
                }
                
                remainingSeconds[0]--;
                
                // Send hook notification with countdown using reflection
                if (Bukkit.getPluginManager().getPlugin("Hook") != null) {
                    try {
                        org.bukkit.plugin.Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
                        if (hookPlugin != null) {
                            // Get Hook instance using reflection
                            java.lang.reflect.Method getInstanceMethod = hookPlugin.getClass().getMethod("getInstance");
                            Object hookInstance = getInstanceMethod.invoke(null);
                            
                            if (hookInstance != null) {
                                // Get API using reflection
                                java.lang.reflect.Method getAPIMethod = hookInstance.getClass().getMethod("getAPI");
                                Object hookAPI = getAPIMethod.invoke(hookInstance);
                                
                                if (hookAPI != null) {
                                    // Use title notification for countdown
                                    String countdownText = remainingSeconds[0] > 0 ? 
                                        com.massivecraft.factions.util.PandoraMessage.text("Teleporting in ") +
                                        com.massivecraft.factions.util.PandoraMessage.highlight(remainingSeconds[0] + " seconds") :
                                        com.massivecraft.factions.util.PandoraMessage.text("Teleporting...");
                                    
                                    // Call sendTitle using reflection
                                    java.lang.reflect.Method sendTitleMethod = hookAPI.getClass().getMethod("sendTitle", 
                                        org.bukkit.entity.Player.class, String.class, String.class);
                                    sendTitleMethod.invoke(hookAPI, player, 
                                        com.massivecraft.factions.util.PandoraMessage.highlight("Home Teleport"),
                                        countdownText);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Hook not available or reflection failed
                    }
                }
                
                // When countdown reaches 0, execute teleport
                if (remainingSeconds[0] <= 0) {
                    Bukkit.getScheduler().cancelTask(countdownTaskId[0]);
                    fPlayer.stopWarmup();
                    runnable.run();
                }
            }
        }, 20L, 20L); // Every second
        
        fPlayer.addWarmup(warmup, countdownTaskId[0]);
    }

    public enum Warmup {
        HOME, WARP, FLIGHT, BANNER, CHECKPOINT, WILD
    }

}
