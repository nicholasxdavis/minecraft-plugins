package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.PandoraMessage;
import com.massivecraft.factions.zcore.util.TL;

public class CmdBase extends FCommand {

    public CmdBase() {
        super();
        this.getAliases().add("base");
        this.getAliases().add("b");

        this.setRequirements(new CommandRequirements.Builder(Permission.HOME)
                .playerOnly()
                .build());
    }

    @Override
    public void perform(CommandContext context) {
        FPlayer fPlayer = context.fPlayer;
        Faction faction = context.faction;

        // Check if player has a base
        if (faction != null && faction.isNormal() && faction.hasBeacon()) {
            // Player has a base - open base menu GUI
            com.massivecraft.factions.util.BeaconMenuGUI.openMenu(context.player, faction);
        } else {
            // Player doesn't have a base - open base creation GUI
            com.massivecraft.factions.cmd.CmdBasePurchase cmdBasePurchase = new com.massivecraft.factions.cmd.CmdBasePurchase();
            cmdBasePurchase.openPurchaseGUI(context.player);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_HOME_DESCRIPTION; // Reuse home description as it's similar
    }
}

