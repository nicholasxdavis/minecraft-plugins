package com.massivecraft.factions.tasks;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.BaseRoofValidator;
import com.massivecraft.factions.util.Logger;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class BaseValidationTask extends BukkitRunnable {

    @Override
    public void run() {
        // Roof validation disabled - players can build roofs
        return;
        /*
        if (!Conf.baseEnforceNoRoof || Conf.baseValidationIntervalTicks <= 0) {
            return;
        }

        // Validate all bases with beacons
        for (Faction faction : Factions.getInstance().getAllFactions()) {
            if (!faction.isNormal() || !faction.hasBeacon()) {
                continue;
            }

            Location beaconLoc = faction.getBeaconLocation();
            if (beaconLoc == null || beaconLoc.getWorld() == null) {
                continue;
            }

            // Validate this base
            validateBase(faction, beaconLoc);
        }
        */
    }
    
    /*
    private void validateBase(Faction faction, Location beaconLoc) {
        FLocation centerChunk = com.massivecraft.factions.FLocation.wrap(beaconLoc);
        int floorY = beaconLoc.getBlockY();
        int radiusChunks = 4; // Same as claim radius

        // Validate roof
        List<Location> violations = BaseRoofValidator.validateRoof(
            faction, centerChunk, radiusChunks, floorY);

        if (!violations.isEmpty()) {
            if (Conf.baseAutoRemoveRoofViolations) {
                int removed = BaseRoofValidator.removeRoofViolations(violations);
                if (removed > 0 && Conf.logFactionCreate) {
                    Logger.print("Periodic validation: Removed " + removed + 
                        " roof violations from base " + faction.getTag(), Logger.PrefixType.DEFAULT);
                }
            } else {
                // Just log
                if (Conf.logFactionCreate) {
                    Logger.print("Periodic validation: Base " + faction.getTag() + 
                        " has " + violations.size() + " roof violations", Logger.PrefixType.DEFAULT);
                }
            }
        }

        // Validate beacon sky access
        if (!BaseRoofValidator.validateBeaconSkyAccess(beaconLoc)) {
            if (Conf.logFactionCreate) {
                Logger.print("Periodic validation: Base " + faction.getTag() + 
                    " beacon sky access is blocked", Logger.PrefixType.DEFAULT);
            }
        }
    }
    */
}



