package com.massivecraft.factions.zcore.persist;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.zcore.MPlugin;

public class SaveTask implements Runnable {

    private static boolean running = false;

    MPlugin p;

    public SaveTask(MPlugin p) {
        this.p = p;
    }

    public void run() {
        if (!p.getAutoSave() || running) return;
        running = true;
        try {
            p.preAutoSave();
            
            // Save core data structures
            Factions.getInstance().forceSave(false);
            FPlayers.getInstance().forceSave(false);
            Board.getInstance().forceSave(false);
            Conf.save();
            
            // Save all additional data structures
            FactionsPlugin plugin = FactionsPlugin.getInstance();
            if (plugin != null) {
                // Save timer data
                if (plugin.getTimerManager() != null) {
                    plugin.getTimerManager().saveTimerData();
                }
                
                // Save faction logs
                if (plugin.getFlogManager() != null) {
                    try {
                        plugin.getFlogManager().saveLogs();
                    } catch (Exception e) {
                        Logger.print("Error saving faction logs during auto-save: " + e.getMessage(), Logger.PrefixType.WARNING);
                    }
                }
                
                // Save all faction data (chests, perks, etc.)
                if (plugin.getFactionDataHelper() != null) {
                    plugin.getFactionDataHelper().getCache().forEach((factionID, factionData) -> {
                        if (factionData != null && factionData.get() != null) {
                            plugin.getFactionDataHelper().saveFactionData(factionData.get());
                        }
                    });
                }
                
                // Save obsidian health data
                if (Conf.obsidianHealthEnabled) {
                    try {
                        com.massivecraft.factions.util.ObsidianHealthManager.getInstance().saveData();
                    } catch (Exception e) {
                        Logger.print("Error saving obsidian health data during auto-save: " + e.getMessage(), Logger.PrefixType.WARNING);
                    }
                }
                
                // Save tripwire alarm data
                if (plugin.getTripwireAlarmManager() != null) {
                    try {
                        plugin.getTripwireAlarmManager().saveAlarmData();
                    } catch (Exception e) {
                        Logger.print("Error saving tripwire alarm data during auto-save: " + e.getMessage(), Logger.PrefixType.WARNING);
                    }
                }
                
                // Save reserves
                if (plugin.reserveObjects != null) {
                    try {
                        com.massivecraft.factions.zcore.util.ShutdownParameter.saveReserves(plugin);
                    } catch (Exception e) {
                        Logger.print("Error saving reserves during auto-save: " + e.getMessage(), Logger.PrefixType.WARNING);
                    }
                }
            }
            
            p.postAutoSave();
            Logger.print("Auto-save completed: All data saved to disk", Logger.PrefixType.DEFAULT);
        } catch (Exception e) {
            Logger.print("Error during auto-save: " + e.getMessage(), Logger.PrefixType.FAILED);
            e.printStackTrace();
        } finally {
            running = false;
        }
    }
}
