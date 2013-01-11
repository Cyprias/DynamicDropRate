package com.cyprias.DynamicDropRate.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.cyprias.DynamicDropRate.Logger;
import com.cyprias.DynamicDropRate.Plugin;

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
			stat.executeUpdate("CREATE TABLE " + rates_table+ " (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `time` BIGINT NOT NULL, `notify` BOOLEAN NOT NULL DEFAULT '0', `writer` VARCHAR(32) NOT NULL, `player` VARCHAR(32) NOT NULL, `text` TEXT NOT NULL)");
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
	public Double getRate(String entityType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean setRate(String entityType, Double rate) {
		return null;
	}

}
