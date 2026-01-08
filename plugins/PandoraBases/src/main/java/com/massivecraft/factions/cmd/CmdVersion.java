package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import com.massivecraft.factions.zcore.util.TextUtil;


public class CmdVersion extends FCommand {

    /**
     * @author FactionsUUID Team - Modified By CmdrKittens
     */

    public CmdVersion() {
        this.getAliases().add("version");
        this.getAliases().add("ver");

        this.setRequirements(new CommandRequirements.Builder(Permission.VERSION)
                .build());
    }

    @Override
    public void perform(CommandContext context) {
        context.msg(TextUtil.parse("&e&lPandora &8» &r &7&e&lPandoraBases &7- By Driftay"));
        context.msg(TL.COMMAND_VERSION_VERSION, FactionsPlugin.getInstance().getDescription().getFullName());
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_VERSION_DESCRIPTION;
    }
}