package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.cmd.reserve.ReserveObject;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.event.FactionCreateEvent;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.Cooldown;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;

import java.util.ArrayList;


public class CmdCreate extends FCommand {

    /**
     * @author FactionsUUID Team - Modified By CmdrKittens
     */

    public CmdCreate() {
        super();
        this.getAliases().addAll(Aliases.create);

        // No required args - players purchase via GUI then place beacon
        this.setRequirements(new CommandRequirements.Builder(Permission.CREATE)
                .playerOnly()
                .build());
    }

    @Override
    public void perform(CommandContext context) {
        // Redirect to base purchase system
        // Players must purchase a base beacon first, then place it in wilderness
        if (context.fPlayer.hasFaction()) {
            context.msg(TL.COMMAND_CREATE_MUSTLEAVE);
            return;
        }

        // Open purchase GUI
        new CmdBasePurchase().perform(context);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_CREATE_DESCRIPTION;
    }

}