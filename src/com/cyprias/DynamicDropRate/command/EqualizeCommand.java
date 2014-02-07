package com.cyprias.DynamicDropRate.command;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import com.cyprias.DynamicDropRate.ChatUtils;
import com.cyprias.DynamicDropRate.Logger;
import com.cyprias.DynamicDropRate.Perm;
import com.cyprias.DynamicDropRate.Plugin;
import com.cyprias.DynamicDropRate.configuration.Config;

public class EqualizeCommand  implements Command {
	
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.EQUALIZE))
			list.add("/%s equalize - Equalize the sum droprate to equal 100%%.");
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
		if (!Plugin.checkPermission(sender, Perm.EQUALIZE)) {
			return false;
		}
		
		if (args.length > 0){
			getCommands(sender, cmd);
			return true;
		}
		
		EntityType mob;
		Double rate;
		List<MobRate> rates;
		
		Double sum;
		
		
		
		Set<String> groups = Config.getConfigurationSection("world-groups").getKeys(false);

		for (String group : groups) {
			sum = 0.0;
			rates = new ArrayList<MobRate>();
			
			
			for (int i=0; i<Plugin.mobTypes.size(); i++){
				mob = Plugin.mobTypes.get(i);
				rate = Plugin.getMobRate(mob, group);

				rates.add(new MobRate(mob, rate));
				sum+= rate;

			}
			
			ChatUtils.send(sender, group + ": " + rates.size() + " mobs summing " + Plugin.Round((sum/rates.size())*100,2) + "%");
			
			double perMob = (1-sum / rates.size());
			
			/*
			Double sRate;
			if (perMob > 0){
				ChatUtils.send(sender, "Increasing all mobs by " + Plugin.Round(perMob*100,2) + "%" );
				
				for (int i=0; i<Plugin.mobTypes.size(); i++){
					mob = Plugin.mobTypes.get(i);
					sRate = Plugin.mobRates.get(mob);
					Plugin.mobRates.put(mob, sRate + perMob);
				}
				
			}else{
				ChatUtils.send(sender, "Decreasing all mobs by " + Plugin.Round(perMob*100,2) + "%" );
				for (int i=0; i<Plugin.mobTypes.size(); i++){
					mob = Plugin.mobTypes.get(i);
					sRate = Plugin.mobRates.get(mob);
					
					Plugin.mobRates.put(mob, sRate - Math.abs(perMob));
				}
			}
			*/
			
			
			Double sRate;
			if (perMob > 0){
				ChatUtils.send(sender, "  Increasing all mobs by " + Plugin.Round(perMob*100,2) + "%" );
				
				for (int i=0; i<Plugin.mobTypes.size(); i++){
					mob = Plugin.mobTypes.get(i);
					sRate = Plugin.getMobRate(mob, group);
					
					Plugin.setMobRate(mob, group, sRate + perMob);
					
				}
				
			}else{
				ChatUtils.send(sender, "  Decreasing all mobs by " + Plugin.Round(perMob*100,2) + "%" );
				for (int i=0; i<Plugin.mobTypes.size(); i++){
					mob = Plugin.mobTypes.get(i);
					sRate = Plugin.getMobRate(mob, group);
				
					Plugin.setMobRate(mob, group, sRate - Math.abs(perMob));
				}
			}
			
			
		}
		


		
		
		
		
		try {
			Plugin.saveMobRates();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return true;
	}


	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.EQUALIZE, "/%s equalize - Equalize the sum droprate to equal 100%.", cmd);
	}

	public boolean hasValues() {
		return false;
	}
	
}
