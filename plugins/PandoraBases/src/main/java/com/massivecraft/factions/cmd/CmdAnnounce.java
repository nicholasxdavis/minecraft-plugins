package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class CmdAnnounce extends FCommand {

    /**
     * @author FactionsUUID Team - Modified By CmdrKittens
     */

    public CmdAnnounce() {
        super();
        this.getAliases().addAll(Aliases.announce);

        this.getRequiredArgs().add("message");

        this.setRequirements(new CommandRequirements.Builder(Permission.ANNOUNCE)
                .playerOnly()
                .memberOnly()
                .brigadier(AnnounceBrigadier.class)
                .noErrorOnManyArgs()
                .build());
    }

    @Override
    public void perform(CommandContext context) {
        // Use proper color scheme: &e for special, &7 for normal, &6 for values
        String prefix = com.massivecraft.factions.util.PandoraMessage.highlight(context.faction.getTag()) + 
                       com.massivecraft.factions.util.PandoraMessage.text(" [") +
                       com.massivecraft.factions.util.PandoraMessage.highlight(context.player.getName()) +
                       com.massivecraft.factions.util.PandoraMessage.text("] ");
        String message = String.join(" ", context.args);

        for (Player player : context.faction.getOnlinePlayers()) {
            player.sendMessage(prefix + com.massivecraft.factions.util.PandoraMessage.text(message));
        }

        // Add for offline players.
        String announcement = prefix + message;
        for (FPlayer fp : context.faction.getFPlayersWhereOnline(false)) {
            context.faction.addAnnouncement(fp, announcement);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_ANNOUNCE_DESCRIPTION;
    }

    public static class AnnounceBrigadier implements BrigadierProvider {

        @Override
        public ArgumentBuilder<Object, ?> get(ArgumentBuilder<Object, ?> parent) {
            return parent.then(RequiredArgumentBuilder.argument("message", StringArgumentType.greedyString()));
        }
    }
}