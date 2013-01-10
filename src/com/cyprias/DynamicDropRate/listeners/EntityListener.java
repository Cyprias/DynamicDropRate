package com.cyprias.DynamicDropRate.listeners;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.cyprias.DynamicDropRate.Logger;
import com.cyprias.DynamicDropRate.Plugin;
import com.cyprias.DynamicDropRate.configuration.Config;


public class EntityListener implements Listener {

	
	
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDeath(EntityDeathEvent event)  {
		
		
		
		
		
		
		LivingEntity entity = event.getEntity();
		
		EntityType eType = entity.getType();
		
		if (!Plugin.mobRates.containsKey(eType))
			return;
		
		double rateChange = Config.getDouble("properties.rate-change")/100;
		
		
		Double rate = Plugin.mobRates.get(eType);
		
		rate -= rateChange;

		
		int exp = event.getDroppedExp();
		
		if (Config.getBoolean("properties.affect-exp"))
			event.setDroppedExp((int) Math.round(exp * rate));
		
		
		if (Config.getBoolean("properties.affect-drops")){
			List<ItemStack> drops = event.getDrops();
			int iAmount;
			for (int i=drops.size()-1;i>=0;i--){
				iAmount = (int) Math.round(drops.get(i).getAmount() * rate);
				
				if (iAmount>0){
					drops.get(i).setAmount(iAmount);
				}else
					drops.remove(i);
			}
		}
		
		//Can't set rate below zero, don't increase other mob's rate.
		if (rate < 0)
			return;

		if (Config.getBoolean("properties.debug-messages"))
			Logger.info("- Decreasing " + eType + " to " + Plugin.Round(rate*100,2));
		
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
			Logger.info("+ Increasing " + selectedMob + " to " + Plugin.Round(sRate*100,2) + "%");
		
		Plugin.mobRates.put(selectedMob, sRate);
		

	}
	
}
