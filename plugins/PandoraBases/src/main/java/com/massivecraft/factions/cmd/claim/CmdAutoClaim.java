package com.massivecraft.factions.cmd.claim;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.Aliases;
import com.massivecraft.factions.cmd.CommandContext;
import com.massivecraft.factions.cmd.CommandRequirements;
import com.massivecraft.factions.cmd.FCommand;
import com.massivecraft.factions.cmd.audit.FLogType;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.CC;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;

public class CmdAutoClaim extends FCommand {

    /**
     * @author FactionsUUID Team - Modified By CmdrKittens
     */

    public CmdAutoClaim() {
        super();
        this.getAliases().addAll(Aliases.claim_auto);

        //this.requiredArgs.add("");
        this.getOptionalArgs().put("faction", "your");

        this.setRequirements(new CommandRequirements.Builder(Permission.AUTOCLAIM)
                .playerOnly()
                .withAction(PermissableAction.TERRITORY)
                .build());
    }

    @Override
    public void perform(CommandContext context) {
        // AUTOCLAIM IS DISABLED - Players only get the 100x100 base cube, no autoclaim
        context.msg(com.massivecraft.factions.util.PandoraMessage.formatWithPrefix(
            com.massivecraft.factions.util.PandoraMessage.error("Autoclaim is disabled. Bases are limited to the 100x100 cube area.")));
        return;
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_AUTOCLAIM_DESCRIPTION;
    }

}