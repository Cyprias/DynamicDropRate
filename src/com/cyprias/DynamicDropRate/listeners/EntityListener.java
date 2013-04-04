package com.cyprias.DynamicDropRate.listeners;

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
		if (Config.inStringList("excluded-worlds", event.getEntity().getWorld().getName()))
			return;
		
		LivingEntity entity = event.getEntity();
		
		EntityType eType = entity.getType();
		
		if (!Plugin.mobRates.containsKey(eType))
			return;

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
		
		
		Double rate = Plugin.mobRates.get(eType);
		
		//if ((rate - rateChange) > Config.getDouble("properties.minimum-rate"))
		
		
		
		int exp = event.getDroppedExp();
		
		if (Config.getBoolean("properties.affect-exp")){
			event.setDroppedExp((int) Math.round(exp * rate));
		
			Logger.debug("Modifying " + eType + "'s exp " + exp + " * " + Plugin.Round(rate*100,2) + "% = " + (int) Math.round(exp * rate));
			
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
		if (rate < Config.getDouble("properties.minimum-rate")){
			if (Config.getBoolean("properties.debug-messages"))
				Logger.info(eType + " can't go any lower.");
			return;
		}
		if (Config.getBoolean("properties.debug-messages"))
			Logger.info("- Decreasing " + eType + "'s rate to " + Plugin.Round(rate*100,2));
		
		Plugin.mobRates.put(eType, rate);

		int rad;
		EntityType selectedMob;
		while (true) {
			rad = (int) Math.round(Math.random() * (Plugin.mobTypes.size()-1));
			selectedMob = Plugin.mobTypes.get(rad);
		//	Logger.info("rad: " + rad + ", selectedMob: " + selectedMob);
			if (!selectedMob.equals(eType))
				break;
		}

		Double sRate = Plugin.mobRates.get(selectedMob);
		sRate += rateChange;
		
		if (Config.getBoolean("properties.debug-messages"))
			Logger.info("+ Increasing " + selectedMob + "'s rate to " + Plugin.Round(sRate*100,2) + "%");
		
		Plugin.mobRates.put(selectedMob, sRate);
		

	}
	
}
