package com.cyprias.DynamicDropRate;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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

	public static HashMap<EntityType, Double> mobRates = new HashMap<EntityType, Double>();
	public static List<EntityType> mobTypes = new ArrayList<EntityType>();

	private void loadMobRates() throws SQLException {

		List<String> mobs = Config.getStringList("mobs");
		// http://jd.bukkit.org/doxygen/d6/d7b/EntityType_8java_source.html
		EntityType eType;

		mobRates.clear();
		mobTypes.clear();
		
		for (String mob : mobs) {
			eType = EntityType.fromName(mob);
			if (eType != null) {
				mobRates.put(eType, database.getRate(eType.getName()));
				mobTypes.add(eType);
				if (Config.getBoolean("properties.debug-messages"))
					Logger.info("Added " + eType + " to tracking.");
			} else {
				Logger.warning(mob + " is not a valid mob.");
			}
		}

	}

	public static void saveMobRates() throws SQLException {
		for (EntityType type : mobTypes) {
			database.setRate(type.toString(), mobRates.get(type));
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

	public static void reload() {
		instance.reloadConfig();
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

}
