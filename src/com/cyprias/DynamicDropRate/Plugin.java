package com.cyprias.DynamicDropRate;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.xml.sax.SAXException;

import com.cyprias.DynamicDropRate.VersionChecker.versionInfo;
import com.cyprias.DynamicDropRate.command.Command;
import com.cyprias.DynamicDropRate.command.CommandManager;
import com.cyprias.DynamicDropRate.command.ListCommand;
import com.cyprias.DynamicDropRate.configuration.Config;
import com.cyprias.DynamicDropRate.listeners.EntityListener;

public class Plugin extends JavaPlugin {
	// static PluginDescriptionFile description;
	private static Plugin instance = null;

	public void onLoad() {
		// description = getDescription();
	}

	public void onEnable() {
		instance = this;

		// File dataFolder = getDataFolder();
		// configFile = new File(dataFolder, "config.yml");
		getConfig().options().copyDefaults(true);
		saveConfig();


		loadPermissions();
		registerListeners(new EntityListener());

		
		CommandManager cm = new CommandManager().registerCommand("list", new ListCommand());

		this.getCommand("ddr").setExecutor(cm);
		
		/*
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
		}*/
		loadMobRates();
		
		//if (Config.getBoolean("properties.check-new-version"))
		//	checkVersion();

		Logger.info("enabled.");
	}

	
	public static HashMap<EntityType, Double> mobRates = new HashMap<EntityType, Double>();
	public static List<EntityType> mobTypes = new ArrayList<EntityType>();
	
	private void loadMobRates(){
		
		List<String> mobs = Config.getStringList("mobs");
		//http://jd.bukkit.org/doxygen/d6/d7b/EntityType_8java_source.html
		EntityType eType;
		
		for (String mob : mobs) {
			eType = EntityType.fromName(mob);
			if (eType != null){
				mobRates.put(eType, 1.0);
				mobTypes.add(eType);
			}else{
				Logger.warning(mob + " is not a valid mob.");
			}
		}
		
	}
	
	private void loadPermissions() {
		PluginManager pm = Bukkit.getPluginManager();
		for (Perm permission : Perm.values()) {
			permission.loadPermission(pm);
		}
	}

	private void checkVersion() {
		getServer().getScheduler().runTaskAsynchronously(instance, new Runnable() {
			public void run() {
				try {
					VersionChecker version = new VersionChecker("http://dev.bukkit.org/server-mods/adminnotes/files.rss");
					versionInfo info = (version.versions.size() > 0) ? version.versions.get(0) : null;
					if (info != null) {
						String curVersion = getDescription().getVersion();
						if (VersionChecker.compareVersions(curVersion, info.getTitle()) < 0) {
							Logger.warning("We're running v" + curVersion + ", v" + info.getTitle() + " is available");
							Logger.warning(info.getLink());
						}
					}
				} catch (SAXException e) {e.printStackTrace();
				} catch (IOException e) {e.printStackTrace();
				} catch (ParserConfigurationException e) {e.printStackTrace();
				}

			}
		});
	}

	private void registerListeners(Listener... listeners) {
		PluginManager manager = getServer().getPluginManager();

		for (Listener listener : listeners) {
			manager.registerEvents(listener, this);
		}
	}

	public void onDisable() {
		Logger.info("disabled.");
		instance = null;
	}

	public static void reload() {
		instance.reloadConfig();
	}

	public static void disable() {
		instance.getServer().getPluginManager().disablePlugin(instance);
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
		for (int i=1; i<=pl; i++)
			format += "#";
		
		DecimalFormat df = new DecimalFormat(format);
		return df.format(val);
	}
	public static String Round(double val) {
		return Round(val,0);
	}
	
}
