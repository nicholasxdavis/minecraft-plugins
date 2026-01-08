package com.massivecraft.factions.cmd.chest;

import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.Aliases;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;

public class CmdChest extends FCommand {

    /**
     * @author Illyria Team
     */
    public CmdChest() {
        this.getAliases().addAll(Aliases.chest);

        // Allow all members to access chest (not just mods)
        // Only require member status, not CHEST permission action
        this.setRequirements(new CommandRequirements.Builder(Permission.CHEST)
                .playerOnly()
                .memberOnly()
                .build());
    }

    @Override
    public void perform(CommandContext context) {
        // Check if player has a base with beacon - use base chest if available
        if (context.fPlayer.hasFaction() && context.fPlayer.getFaction().isNormal() && context.fPlayer.getFaction().hasBeacon()) {
            // All members can access base chest (not just mods)
            com.massivecraft.factions.util.BaseChestManager.openChest(context.player, context.fPlayer.getFaction());
            return;
        }

        // Fallback to old faction chest system
        if (!FactionsPlugin.getInstance().getConfig().getBoolean("fchest.Enabled")) {
            context.msg(TL.GENERIC_DISABLED, "Faction Chests");
            return;
        }
        // This permission check is way too explicit, but it's clean
        context.fPlayer.setInFactionsChest(true);
        context.player.openInventory(context.faction.getChestInventory());
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_VAULT_DESCRIPTION;
    }
}
