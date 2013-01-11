package com.cyprias.DynamicDropRate.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import com.cyprias.DynamicDropRate.ChatUtils;
import com.cyprias.DynamicDropRate.Perm;
import com.cyprias.DynamicDropRate.Plugin;

public class ListCommand implements Command {
	
	public void listCommands(CommandSender sender, List<String> list) {
		if (Plugin.hasPermission(sender, Perm.LIST))
			list.add("/%s list - List mobs and their drop rate.");
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
		if (!Plugin.checkPermission(sender, Perm.LIST)) {
			return false;
		}
		/*if (args.length == 0){
			getCommands(sender, cmd);
			return true;
		}*/
		
		EntityType mob;
		Double rate;
		List<MobRate> rates = new ArrayList<MobRate>();
		
		Double sum = 0.0;
		
		for (int i=0; i<Plugin.mobTypes.size(); i++){
			mob = Plugin.mobTypes.get(i);
			rate = Plugin.mobRates.get(mob);
			
			rates.add(new MobRate(mob, rate));
			sum+= rate;
			
			//.send(sender, mob.getName() + ": " + Plugin.Round(rate*100,2) + "%");
			
			
			
		}
		
		compareRates comparator = new compareRates();
		Collections.sort(rates, comparator);
		
		ChatUtils.send(sender, rates.size() + " mobs summing " + Plugin.Round((sum/rates.size())*100,2) + "%");
		
		for (int i=0; i<rates.size(); i++)
			ChatUtils.send(sender, rates.get(i).type.getName() + ": " + Plugin.Round(rates.get(i).rate*100,2) + "%");
		
		
		
		

		return true;
	}

	public CommandAccess getAccess() {
		return CommandAccess.BOTH;
	}

	public void getCommands(CommandSender sender, org.bukkit.command.Command cmd) {
		ChatUtils.sendCommandHelp(sender, Perm.LIST, "/%s list - List mobs and their drop rate.", cmd);
	}

	public boolean hasValues() {
		return false;
	}

}
