package com.cyprias.DynamicDropRate.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import com.cyprias.DynamicDropRate.ChatUtils;
import com.cyprias.DynamicDropRate.Logger;
import com.cyprias.DynamicDropRate.Perm;
import com.cyprias.DynamicDropRate.Plugin;
import com.cyprias.DynamicDropRate.command.ListCommand.MobRate;
import com.cyprias.DynamicDropRate.command.ListCommand.compareRates;
import com.cyprias.DynamicDropRate.configuration.Config;

public class ResetCommand implements Command {
	
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.RESET))
			list.add("/%s resetrates - Reset droprates for all mobs.");
	}

	static public class MobRate{
		private EntityType type;
		private Double rate;
		public MobRate(EntityType type, Double rate){
			this.type = type;
			this.rate= rate;
		}
		
	}
	
	public class compareRates implements Comparator<MobRate> {

		@Override
		public int compare(MobRate o1, MobRate o2) {
			
			if (o1.rate != o2.rate)
				return o2.rate.compareTo(o1.rate);
			return o1.type.compareTo(o2.type);
		}


	}
	
	public boolean execute(final CommandSender sender, org.bukkit.command.Command cmd, String[] args) {
		if (!Plugin.checkPermission(sender, Perm.RESET)) {
			return false;
		}
		/*if (args.length == 0){
			getCommands(sender, cmd);
			return true;
		}*/
		
		List<String> mobs = Config.getStringList("mobs");
		// http://jd.bukkit.org/doxygen/d6/d7b/EntityType_8java_source.html
		EntityType eType;

		Plugin.mobTypes.clear();
		
		for (String mob : mobs) {
			eType = EntityType.fromName(mob);
			if (eType != null) {
				Plugin.mobRates.put(eType, 1.0);
				Plugin.mobTypes.add(eType);
			} else {
				Logger.warning(mob + " is not a valid mob.");
			}
		}

		
		
		Plugin instance;
		if (Config.getBoolean("properties.async-db-queries")){
			instance = Plugin.getInstance();
			instance.getServer().getScheduler().runTaskAsynchronously(instance, new Runnable() {
				public void run() {
					if (Config.getBoolean("properties.debug-messages"))
						Logger.info("Saving rates to DB.");
					try {
						Plugin.saveMobRates();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			
			
		}else{
			if (Config.getBoolean("properties.debug-messages"))
				Logger.info("Saving rates to DB.");
			try {
				Plugin.saveMobRates();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		ChatUtils.send(sender, "Rates reset.");
		
		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.RESET, "/%s resetrates -Reset droprates for all mobs.", cmd);
	}

	public boolean hasValues() {
		return false;
	}

}