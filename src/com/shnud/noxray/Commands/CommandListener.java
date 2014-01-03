package com.shnud.noxray.Commands;

import com.shnud.noxray.Hiders.RoomHider;
import com.shnud.noxray.NoXray;
import com.shnud.noxray.Settings.NoXraySettings;
import com.shnud.noxray.Settings.PlayerMetadataEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Created by Andrew on 02/01/2014.
 */
public class CommandListener implements CommandExecutor {

    private static final int MILLISECONDS_BETWEEN_HIDES = 60 * 1000;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(!(commandSender instanceof Player)) {
            NoXray.getInstance().getLogger().log(Level.INFO, "That command can only be used by players");
            return true;
        }

        String name = command.getName();
        Player player = (Player) commandSender;

        if(!NoXraySettings.getRoomHideWorlds().contains(player.getWorld().getName())) {
            player.sendMessage(ChatColor.RED + "Room hiding is not enabled for this world");
            return true;
        }

        if(name.equalsIgnoreCase("auto"))
            handleAuto(player);
        else if(name.equalsIgnoreCase("hide"))
            handleHide(player);
        else if(name.equalsIgnoreCase("unhide"))
            handleUnHide(player);
        else if(name.equalsIgnoreCase("status"))
            handleStatus(player);

        return true;
    }

    private void handleUnHide(Player sender) {
        RoomHider hider = NoXray.getInstance().getRoomHiderForWorld(sender.getWorld());

        if(hider == null) {
            sender.sendMessage("Unable to find room hider for that world");
            return;
        }

        hider.unHideAtPlayerLocation(sender);
    }

    private void handleHide(Player sender) {
        PlayerMetadataEntry metadata = NoXray.getInstance().getMetadataForPlayer(sender);
        long sinceUsed = metadata.getMillisecondsSinceLastHideCommand();

        if(sinceUsed < MILLISECONDS_BETWEEN_HIDES) {
            sender.sendMessage(
                    ChatColor.YELLOW +
                    "Please wait " + (int) ((MILLISECONDS_BETWEEN_HIDES - sinceUsed) / 1000) +
                    " seconds before hiding again");
            return;
        }

        RoomHider hider = NoXray.getInstance().getRoomHiderForWorld(sender.getWorld());

        if(hider == null) {
            sender.sendMessage("Unable to find room hider for that world");
            return;
        }

        metadata.useHideCommand();
        hider.hideAtPlayerLocation(sender);
    }

    private void handleAuto(Player sender) {
        PlayerMetadataEntry metadata = NoXray.getInstance().getMetadataForPlayer(sender.getName());
        metadata.setAutoProtect(!metadata.isAutoProtectOn());
        sender.sendMessage(ChatColor.GREEN + "Autoprotect has been turned " + (metadata.isAutoProtectOn() ? "on" : "off"));
    }

    private void handleStatus(Player sender) {
        RoomHider hider = NoXray.getInstance().getRoomHiderForWorld(sender.getWorld());

        if(hider == null) {
            sender.sendMessage("Unable to find room hider for that world");
            return;
        }

        int roomID = hider.getRoomIDAtPlayerLocation(sender);

        if(roomID == 0)
            sender.sendMessage(ChatColor.YELLOW + "This room is not protected");
        else
            sender.sendMessage(ChatColor.YELLOW + "This room is protected with an ID of " + roomID);
    }
}
