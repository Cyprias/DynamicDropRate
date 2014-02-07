package com.cyprias.DynamicDropRate.listeners;

import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.cyprias.DynamicDropRate.Logger;
import com.cyprias.DynamicDropRate.Plugin;
import com.cyprias.DynamicDropRate.configuration.Config;


public class EntityListener implements Listener {

	
	static public void unregisterEvents(JavaPlugin instance){
		EntityDeathEvent.getHandlerList().unregister(instance);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDeath(EntityDeathEvent event)  {
		LivingEntity entity = event.getEntity();
		
		String world = entity.getWorld().getName();
		
		EntityType eType = entity.getType();
		
		
		
		if (!Plugin.watchingMob(eType, world)){
			Logger.debug("Not watching " + eType + " in " + world);
			return;
		}
		
		
		EntityDamageEvent dEvent = event.getEntity().getLastDamageCause();
		if (dEvent instanceof EntityDamageByEntityEvent) {

			EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) dEvent;
			if (!(damageEvent.getDamager() instanceof Player) && Config.getBoolean("properties.only-affect-player-kills") == true) {
				Logger.debug("Exiting death due to attacker being " + damageEvent.getDamager().getType());
				return;
			}

		}else if (Config.getBoolean("properties.only-affect-player-kills") == true){
			Logger.debug("Exiting death due to death having no attacker.");
			
			return;
		}
		
		double rateChange = Config.getDouble("properties.rate-change")/100;
		
		
		//String world = entity.getWorld().getName();

		Double rate = Plugin.getMobRate(eType, world);
		
		//if ((rate - rateChange) > Config.getDouble("properties.minimum-rate"))
		
		
		
		int exp = event.getDroppedExp();
		
		if (Config.getBoolean("properties.affect-exp")){
			int modExp = (int) Math.round(exp * rate);
			event.setDroppedExp(Math.abs(modExp));

			Logger.debug("Modifying " + eType + "'s exp " + exp + " * " + Plugin.Round(rate*100,2) + "% = " + Math.abs(modExp));
		}
		
		if (Config.getBoolean("properties.affect-drops")){
			List<ItemStack> drops = event.getDrops();
			int iAmount;
			String sid;
			for (int i=drops.size()-1;i>=0;i--){
				sid = String.valueOf(drops.get(i).getTypeId());
				if (drops.get(i).getDurability() > 0)
					sid+=":" + drops.get(i).getDurability();
					
				if (Config.inStringList("excluded-items", sid))
					continue;

				
				
				iAmount = (int) Math.round(drops.get(i).getAmount() * rate);
				
				Logger.debug("Modifying " + eType + "'s " + drops.get(i).getType() + "x" + drops.get(i).getAmount() + " * " + Plugin.Round(rate*100,2) + "% = x" + Math.round(drops.get(i).getAmount() * rate));
				
				if (iAmount>0){
					drops.get(i).setAmount(iAmount);
				}else
					drops.remove(i);
			}
		}
		
		rate -= rateChange;
		
		//Don't set rate below min rate.
		if (rate <= Config.getDouble("properties.minimum-rate")){
			Logger.debug(eType + " can't go any lower.");
			return;
		}

		
		
		List<String> mobs = Plugin.getMobs();
		EntityType selectedMob;
		
		//while (mobs.size() > 0){
		// Remove mobs we don't want to modify.
		//for (String mob : mobs) {
		for (int i=mobs.size()-1;i>=0;i--){
			
			
			
			String mob = mobs.get(i);
			
			selectedMob = EntityType.fromName(mob.toUpperCase());
			// Skip the mob type we're currently killing.
			if (mob.equalsIgnoreCase(entity.getType().toString())){
//				Logger.debug("Removing " + mob + " due to match.");
				mobs.remove(i);
				continue;
			}
			// Skip mobs if they're above the max rate.
			if (Plugin.getMobRate(selectedMob, world) >= Config.getDouble("properties.maximum-rate")){
				mobs.remove(i);
				continue;
			}

		}
		
		// Select a random mob.
		int rad = (int) Math.round(Math.random() * (mobs.size()-1));
	
		//selectedMob = Plugin.mobTypes.get(mobs.get(rad));
		
		selectedMob = EntityType.fromName(mobs.get(rad));
		
		// Get their rate.
		Double sRate = Plugin.getMobRate(selectedMob, world);
		sRate += rateChange;

		Logger.debug("- Decreasing " + eType + "'s rate to " + Plugin.Round(rate*100,2) + "% in " + world);
		Plugin.setMobRate(eType, world, Plugin.dRound(rate, 8));

		
		Logger.debug("+ Increasing " + selectedMob + "'s rate to " + Plugin.Round(sRate*100,2) + "% in " + world);
		Plugin.setMobRate(selectedMob, world, Plugin.dRound(sRate, 8));
		
		
		
		
	}
	
}
