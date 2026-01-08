package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.FastMath;
import com.massivecraft.factions.util.PandoraMessage;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class CmdTop extends FCommand {

    /**
     * @author FactionsUUID Team - Modified By CmdrKittens
     * 
     */

    private static final Map<String, Comparator<Faction>> CRITERIA = new HashMap<String, Comparator<Faction>>(){{
        put("members", (f1, f2) -> Integer.compare(f2.getFPlayers().size(), f1.getFPlayers().size()));
        put("start", (f1, f2) -> Long.compare(f2.getFoundedDate(), f1.getFoundedDate()));
        put("power", (f1, f2) -> Integer.compare(f2.getPowerRounded(), f1.getPowerRounded()));
        put("land", (f1, f2) -> Integer.compare(f2.getLandRounded(), f1.getLandRounded()));
        put("online", (f1, f2) -> Integer.compare(f2.getFPlayersWhereOnline(true).size(), f1.getFPlayersWhereOnline(true).size()));
        put("money", (f1, f2) -> {
            double f1Size = f1.getFactionBalance();
            for (FPlayer fp : f1.getFPlayers()) {
                f1Size = f1Size + Econ.getBalance(fp.getAccountId());
            }
            double f2Size = f2.getFactionBalance();
            for (FPlayer fp : f2.getFPlayers()) {
                f2Size = f2Size + Econ.getBalance(fp.getAccountId());
            }
            return Double.compare(f2Size, f1Size);
        });
        put("value", (f1, f2) -> {
            double f1Value = com.massivecraft.factions.util.BaseValueCalculator.getCachedBaseValue(f1);
            double f2Value = com.massivecraft.factions.util.BaseValueCalculator.getCachedBaseValue(f2);
            return Double.compare(f2Value, f1Value);
        });
        put("base", (f1, f2) -> {
            double f1Value = com.massivecraft.factions.util.BaseValueCalculator.getCachedBaseValue(f1);
            double f2Value = com.massivecraft.factions.util.BaseValueCalculator.getCachedBaseValue(f2);
            return Double.compare(f2Value, f1Value);
        });
    }};

    public CmdTop() {
        super();
        this.getAliases().addAll(Aliases.top);
        // Make criteria optional - default to "money"
        this.getOptionalArgs().put("criteria", "money");
        this.getOptionalArgs().put("page", "1");

        this.setRequirements(new CommandRequirements.Builder(Permission.TOP)
                .build());
    }


    @Override
    public void perform(CommandContext context) {
        List<Faction> factionList = Factions.getInstance().getAllNormalFactions();

        // Get criteria, default to "money" if not provided
        String criteria = context.argAsString(0);
        if (criteria == null || criteria.isEmpty()) {
            criteria = "money";
        }

        Comparator<Faction> sorter = CRITERIA.get(criteria.toLowerCase());
        if (sorter == null) {
            // Invalid criteria - default to money
            criteria = "money";
            sorter = CRITERIA.get("money");
        }
        
        factionList.sort(sorter);

        List<String> lines = new ArrayList<>();

        final int pageheight = 9;
        int totalPages = (factionList.size() + pageheight - 1) / pageheight;
        int pagenumber = Math.max(1, Math.min(context.argAsInt(1, 1), totalPages));
        int start = (pagenumber - 1) * pageheight;
        int end = Math.min(start + pageheight, factionList.size());

        // Format header with Pandora color scheme
        String criteriaDisplay = criteria.toUpperCase();
        String header = PandoraMessage.header("Top Factions by " + criteriaDisplay) + 
                        PandoraMessage.text(" (Page " + pagenumber + "/" + totalPages + ")");
        lines.add(header);
        lines.add(""); // Empty line for spacing

        // Format each line
        for (int i = start; i < end; i++) {
            Faction faction = factionList.get(i);
            int rank = i + 1;
            
            // Get faction tag with relation color if player
            String facTag;
            if (context.sender instanceof Player && context.fPlayer != null) {
                ChatColor relationColor = faction.getRelationTo(context.fPlayer).getColor();
                facTag = relationColor + faction.getTag();
            } else {
                facTag = faction.getTag();
            }
            
            // Get formatted value
            String value = getFormattedValue(faction, criteria);
            
            // Format line: # &6[rank] &7- &r[facTag] &7» &6[value]
            String line = PandoraMessage.highlight("#" + rank) + 
                         PandoraMessage.text(" - ") + 
                         facTag + 
                         PandoraMessage.text(" » ") + 
                         PandoraMessage.highlight(value);
            lines.add(line);
        }

        context.sendMessage(lines);
    }

    private String getFormattedValue(Faction faction, String criteria) {
        switch (criteria.toLowerCase()) {
            case "online":
                return Integer.toString(faction.getFPlayersWhereOnline(true).size());
            case "start":
                return TL.sdf.format(faction.getFoundedDate());
            case "members":
                return Integer.toString(faction.getFPlayers().size());
            case "land":
                return Integer.toString(faction.getLandRounded());
            case "power":
                return Integer.toString(faction.getPowerRounded());
            case "value":
            case "base":
                double baseValue = com.massivecraft.factions.util.BaseValueCalculator.getCachedBaseValue(faction);
                return formatCurrency(baseValue);
            default: // money
                double balance = faction.getFactionBalance();
                for (FPlayer fp : faction.getFPlayers()) {
                    balance = FastMath.round(balance + Econ.getBalance(fp.getAccountId()));
                }
                return formatCurrency(balance);
        }
    }
    
    private String formatCurrency(double amount) {
        if (amount >= 1000000) {
            return String.format("$%.2fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("$%.2fK", amount / 1000.0);
        } else {
            return String.format("$%.2f", amount);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_TOP_DESCRIPTION;
    }
}
