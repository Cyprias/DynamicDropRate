package com.cyprias.DynamicDropRate.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import com.cyprias.DynamicDropRate.ChatUtils;
import com.cyprias.DynamicDropRate.Perm;
import com.cyprias.DynamicDropRate.Plugin;

public class ReloadCommand implements Command {
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.RELOAD))
			list.add("/%s reload - Reload the plugin.");
	}

	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.RELOAD)) {
			return false;
		}
		/*if (args.length == 0){
			getCommands(sender, cmd);
			return true;
		}*/
		
		Plugin instance = Plugin.getInstance();
		
		
		instance.getPluginLoader().disablePlugin(instance);
		instance.getPluginLoader().enablePlugin(instance);
		
		
		ChatUtils.send(sender, "Plugin reloaded.");
		

		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.RELOAD, "/%s reload - Reload the plugin.", cmd);
	}

	public boolean hasValues() {
		return false;
	}
}
