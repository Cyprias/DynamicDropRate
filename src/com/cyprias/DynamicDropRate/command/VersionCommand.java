package com.cyprias.DynamicDropRate.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import com.cyprias.DynamicDropRate.ChatUtils;
import com.cyprias.DynamicDropRate.Perm;
import com.cyprias.DynamicDropRate.Plugin;

public class VersionCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.VERSION))
			list.add("/%s version - Get the plugin version.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.VERSION)) {
			return false;
		}
		/*
		 * if (args.length == 0){ getCommands(sender, cmd); return true; }
		 */

		final Plugin instance = Plugin.getInstance();

		ChatUtils.send(sender, "We're running version v" + instance.getDescription().getVersion());

		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.VERSION, "/%s version - Get the plugin version.", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
