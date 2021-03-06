package com.cyprias.DynamicDropRate.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import com.cyprias.DynamicDropRate.Logger;
import com.cyprias.DynamicDropRate.Plugin;
import com.cyprias.DynamicDropRate.database.MySQL.queryReturn;

public class SQLite implements Database {
	private static String sqlDB;
	
	static String rates_table = "Rates";
	
	@Override
	public Boolean init() {
		File file = Plugin.getInstance().getDataFolder();
		String pluginPath = file.getPath() + File.separator;

		sqlDB = "jdbc:sqlite:" + pluginPath + "database.sqlite";
		
		try {
			createTables();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(sqlDB);
	}
	
	public static boolean tableExists(String tableName) throws SQLException {
		boolean exists = false;
		Connection con = getConnection();
		ResultSet result = con.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';");
		while (result.next()) {
			exists = true;
			break;
		}
		con.close();
		return exists;
	}
	
	public static void createTables() throws SQLException, ClassNotFoundException {
		// database.plugin.debug("Creating SQLite tables...");
		Class.forName("org.sqlite.JDBC");
		Connection con = getConnection();
		Statement stat = con.createStatement();

		//"CREATE TABLE " + rates_table+ " (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `time` BIGINT NOT NULL, `notify` BOOLEAN NOT NULL DEFAULT '0', `writer` VARCHAR(32) NOT NULL, `player` VARCHAR(32) NOT NULL, `text` TEXT NOT NULL) ENGINE = InnoDB"
		if (tableExists(rates_table) == false) {
			Logger.info("Creating SQLite " + rates_table + " table.");
			stat.executeUpdate("CREATE TABLE `"+rates_table+"` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `type` VARCHAR(16) NOT NULL, `rate` DOUBLE NOT NULL, `world` VARCHAR(16) NOT NULL)");
		//}	else if (tableFieldExists(rates_table, "world") == false) {
			//SQLite doesn't support adding tables. I don't want to recreate the table crap. 
		}
		
		stat.close();
		con.close();

	}
	
	public static int getResultCount(String query, Object... args) throws SQLException {
		queryReturn qReturn = executeQuery(query, args);
		//qReturn.result.first();
		int rows = qReturn.result.getInt(1);
		qReturn.close();
		return rows;
	}
	
	public static int executeUpdate(String query, Object... args) throws SQLException {
		Connection con = getConnection();
		int sucessful = 0;

		PreparedStatement statement = con.prepareStatement(query);
		int i = 0;
		for (Object a : args) {
			i += 1;
			statement.setObject(i, a);
		}
		sucessful = statement.executeUpdate();
		con.close();
		return sucessful;
	}


	public static class queryReturn {
		Connection con;
		PreparedStatement statement;
		public ResultSet result;

		public queryReturn(Connection con, PreparedStatement statement, ResultSet result) {
			this.con = con;
			this.statement = statement;
			this.result = result;
		}

		public void close() throws SQLException {
			this.result.close();
			this.statement.close();
			this.con.close();
		}

	}
	
	public static queryReturn executeQuery(String query, Object... args) throws SQLException {
		Connection con = getConnection();
		queryReturn myreturn = null;// = new queryReturn();
		PreparedStatement statement = con.prepareStatement(query);
		int i = 0;
		for (Object a : args) {
			i += 1;
			// plugin.info("executeQuery "+i+": " + a);
			statement.setObject(i, a);
		}
		ResultSet result = statement.executeQuery();
		myreturn = new queryReturn(con, statement, result);
		return myreturn;
	}
	
	@Override
	public Double getRate(String entityType, String world) throws SQLException {
		queryReturn results = executeQuery("SELECT * FROM `"+rates_table+"` WHERE `type` LIKE ? AND `world` LIKE ? LIMIT 0 , 1", entityType, world);
		ResultSet r = results.result;
		Double rate = -1.0; //Oh oh.
		while (r.next()) {
			rate = r.getDouble("rate");
		}
		results.close();
		return rate;
	}

	@Override
	public Boolean setRate(String entityType, Double rate, String world) throws SQLException {
		int succsess = executeUpdate("UPDATE `"+rates_table+"` SET `rate` = ? WHERE `type` LIKE ? AND `world` LIKE ?;", rate, entityType, world);
		if (succsess > 0)
			return true;

		succsess = executeUpdate("INSERT INTO `"+rates_table+"` (`type` ,`rate`, `world`) VALUES (?, ?, ?);", entityType, rate, world);
		return (succsess > 0) ? true : false;
	}

}
