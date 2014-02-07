package com.cyprias.DynamicDropRate;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import com.cyprias.DynamicDropRate.command.CommandManager;
import com.cyprias.DynamicDropRate.command.EqualizeCommand;
import com.cyprias.DynamicDropRate.command.ListCommand;
import com.cyprias.DynamicDropRate.command.ReloadCommand;
import com.cyprias.DynamicDropRate.command.ResetCommand;
import com.cyprias.DynamicDropRate.command.VersionCommand;
import com.cyprias.DynamicDropRate.configuration.Config;
import com.cyprias.DynamicDropRate.database.Database;
import com.cyprias.DynamicDropRate.database.MySQL;
import com.cyprias.DynamicDropRate.database.SQLite;
import com.cyprias.DynamicDropRate.listeners.EntityListener;

public class Plugin extends JavaPlugin {
	// static PluginDescriptionFile description;
	private static Plugin instance = null;

	//public void onLoad() {}

	public static Database database;

	public void onEnable() {
		instance = this;

		YamlConfiguration dConfig = YamlConfiguration.loadConfiguration(getResource("config.yml"));
		File config = new File(getDataFolder(), "config.yml");
		if (!config.exists() || getConfig().getInt("config-version") < dConfig.getInt("config-version")) { 
			Logger.info("Copying defaults to config file.");
			getConfig().options().copyDefaults(true);
			getConfig().set("config-version", dConfig.getInt("config-version"));
			saveConfig();
		}
		
		if (Config.getString("properties.db-type").equalsIgnoreCase("mysql")) {
			database = new MySQL();
		} else if (Config.getString("properties.db-type").equalsIgnoreCase("sqlite")) {
			database = new SQLite();
		} else {
			Logger.severe("No database selected (" + Config.getString("properties.db-type") + "), unloading plugin...");
			instance.getPluginLoader().disablePlugin(instance);
			return;
		}

		if (!database.init()) {
			Logger.severe("Failed to initilize database, unloading plugin...");
			instance.getPluginLoader().disablePlugin(instance);
			return;
		}
		
		CommandManager cm = new CommandManager().registerCommand("list", new ListCommand()).registerCommand("reload", new ReloadCommand()).registerCommand("version", new VersionCommand()).registerCommand("resetrates", new ResetCommand()).registerCommand("equalize", new EqualizeCommand());
		this.getCommand("ddr").setExecutor(cm);
		
		if (Config.getBoolean("properties.async-db-queries")){
			instance.getServer().getScheduler().runTaskTimerAsynchronously(instance, new Runnable() {
				public void run() {
					if (Config.getBoolean("properties.debug-messages"))
						Logger.info("Saving rates to DB.");
					try {
						saveMobRates();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, Config.getInt("properties.save-frequency") * 20L, Config.getInt("properties.save-frequency") * 20L);

			instance.getServer().getScheduler().runTaskAsynchronously(instance, new Runnable() {
				public void run() {
					try {
						if (Config.getBoolean("properties.debug-messages"))
							Logger.info("loading rate from DB.");
						
						loadMobRates();
					} catch (SQLException e) {e.printStackTrace();}
				}
			});
			
			
		}else{
			instance.getServer().getScheduler().runTaskTimer(instance, new Runnable() {
				public void run() {
					if (Config.getBoolean("properties.debug-messages"))
						Logger.info("Saving rates to DB.");
					try {
						saveMobRates();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, Config.getInt("properties.save-frequency") * 20L, Config.getInt("properties.save-frequency") * 20L);
			
			try {
				if (Config.getBoolean("properties.debug-messages"))
					Logger.info("loading rate from DB.");
				loadMobRates();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

		
		
		loadPermissions();

		registerListeners(new EntityListener());


		
		
		
		if (Config.getBoolean("properties.use-metrics"))
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
			} catch (IOException e) {
			}
		
		Logger.info("enabled.");
	}

	//private static HashMap<String, HashMap<EntityType, Double>> mobRates = new HashMap<String, HashMap<EntityType, Double>>();
	private static HashMap<EntityType, HashMap<String, Double>> mobRates = new HashMap<EntityType, HashMap<String, Double>>();
	
	public static String getWorldGroup(String worldName){
		
		Set<String> groups = Config.getConfigurationSection("world-groups").getKeys(false);
		
		//Logger.debug("Checking which group " + worldName + " is in.");
		
		for (String group : groups) {
			//Logger.debug("group: " + group);
			
			if (group.equalsIgnoreCase(worldName))
				return group;

			List<String> worlds = Config.getConfigurationSection("world-groups").getStringList(group);

			for (String world : worlds) {
			//	Logger.debug("group: " + group + " world " + world);
				
				if (world.equalsIgnoreCase(worldName)){
					
				//	Logger.debug(worldName + " is in " + group);
					
					return group;
				}
			}
			
			
		}
		
		return null;
	}
	
	
	public static double getMobRate(EntityType eType, String world){
		String worldGroup = getWorldGroup(world);
		
		if (worldGroup != null)
			if (mobRates.containsKey(eType))
				if (mobRates.get(eType).containsKey(worldGroup))
					return mobRates.get(eType).get(worldGroup);

		return 1.0;
	}
	
	public static void setMobRate(EntityType eType, String world, Double rate){
		String worldGroup = getWorldGroup(world);
		
		if (worldGroup != null){
			if (!mobRates.containsKey(eType))
				mobRates.put(eType, new HashMap<String, Double>());

			mobRates.get(eType).put(worldGroup, rate);
		}
	}
	
	public static boolean watchingMob(EntityType eType, String world){
		
		if (mobRates.containsKey(eType)){
			//if (mobRates.get(eType).containsKey(world))
				return true;
			
		}
		return false;
	}
	
	public static HashMap<String, Double> getMobWorlds(EntityType eType){
		if (mobRates.containsKey(eType))
			return mobRates.get(eType);
				
		return null;
	}
	
	public static List<String> getMobs(){
		return Config.getStringList("mobs");
	}
	
	
	public static List<EntityType> mobTypes = new ArrayList<EntityType>();

	private static void loadMobRates() throws SQLException {
		Set<String> worlds = Config.getConfigurationSection("world-groups").getKeys(false);
		

		List<String> mobs = Config.getStringList("mobs");
		// http://jd.bukkit.org/doxygen/d6/d7b/EntityType_8java_source.html
		EntityType eType;

		mobRates.clear();
		mobTypes.clear();
		
		

		for (String world : worlds) {
			for (String mob : mobs) {
				eType = EntityType.fromName(mob);
				
				// Value Of gives me errors claiming CAVESPIDER doesn't exist, when I do CAVE_SPIDER I get no error but it doesn't return anything. =/
				//eType = EntityType.valueOf(mob.toUpperCase());

				if (eType != null) {

					Double rate = database.getRate(eType.getName(), world);

					setMobRate(eType, world, rate);
					
					if (!mobTypes.contains(eType))
						mobTypes.add(eType);
					
				
					Logger.debug("Loading " + eType + " " + rate + "% " + world + " from db");
				} else {
					Logger.warning(mob + " is not a valid mob.");
				}
			}
		}
	}

	public static void saveMobRates() throws SQLException {
		for (String world : Config.getConfigurationSection("world-groups").getKeys(false)) {
			for (EntityType type : mobTypes) {

				double rate = getMobRate(type, world);

				Logger.debug("Saving " + type.getName() + " " + rate + "% " + world + " to db");
				
				database.setRate(type.getName(),rate, world);

			}
		}
	}

	private void loadPermissions() {
		PluginManager pm = Bukkit.getPluginManager();
		for (Perm permission : Perm.values()) {
			permission.loadPermission(pm);
		}
	}


	Listener[] listenerList;

	private void registerListeners(Listener... listeners) {
		PluginManager manager = getServer().getPluginManager();

		listenerList = listeners;

		for (Listener listener : listeners) {
			manager.registerEvents(listener, this);
		}
	}

	public void onDisable() {
		try {
			saveMobRates();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		PluginManager pm = Bukkit.getPluginManager();
		for (Perm permission : Perm.values()) {
			// permission.loadPermission(pm);
			permission.unloadPermission(pm);
		}

		CommandManager.unregisterCommands();
		this.getCommand("ddr").setExecutor(null);
		
		
		instance.getServer().getScheduler().cancelTasks(instance);
		
		EntityListener.unregisterEvents(instance);

		instance = null;
		Logger.info("disabled.");
	}

	public static void reload() throws SQLException {
		saveMobRates();
		instance.reloadConfig();
		loadMobRates();
	}

	static public boolean hasPermission(CommandSender sender, Perm permission) {
		if (sender != null) {
			if (sender instanceof ConsoleCommandSender)
				return true;

			if (sender.hasPermission(permission.getPermission())) {
				return true;
			} else {
				Perm parent = permission.getParent();
				return (parent != null) ? hasPermission(sender, parent) : false;
			}
		}
		return false;
	}

	public static boolean checkPermission(CommandSender sender, Perm permission) {
		if (!hasPermission(sender, permission)) {
			String mess = permission.getErrorMessage();
			if (mess == null)
				mess = Perm.DEFAULT_ERROR_MESSAGE;
			ChatUtils.error(sender, mess);
			return false;
		}
		return true;
	}

	public static final Plugin getInstance() {
		return instance;
	}

	public static double getUnixTime() {
		return (System.currentTimeMillis() / 1000D);
	}

	public static String getFinalArg(final String[] args, final int start) {
		final StringBuilder bldr = new StringBuilder();
		for (int i = start; i < args.length; i++) {
			if (i != start) {
				bldr.append(" ");
			}
			bldr.append(args[i]);
		}
		return bldr.toString();
	}

	public static boolean isInt(final String sInt) {
		try {
			Integer.parseInt(sInt);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static boolean isDouble(final String sDouble) {
		try {
			Double.parseDouble(sDouble);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static String Round(double val, int pl) {
		String format = "#.";
		for (int i = 1; i <= pl; i++)
			format += "#";

		DecimalFormat df = new DecimalFormat(format);
		return df.format(val);
	}

	public static String Round(double val) {
		return Round(val, 0);
	}
	
	public static double dRound(double Rval, int Rpl) {
		double p = (double) Math.pow(10, Rpl);
		Rval = Rval * p;
		double tmp = Math.round(Rval);
		return (double) tmp / p;
	}

}
