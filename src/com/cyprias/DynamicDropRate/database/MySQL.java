package com.cyprias.DynamicDropRate.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.cyprias.DynamicDropRate.Logger;
import com.cyprias.DynamicDropRate.configuration.Config;

public class MySQL implements Database {




	static String prefix;
	static String rates_table;
	public Boolean init() {
		if (!canConnect()){
			Logger.info("Failed to connect to MySQL!");
			return false;
		}
		prefix = Config.getString("mysql.prefix");
		rates_table = prefix+ "Rates";
		
		
		try {
			createTables();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	public void createTables() throws SQLException{
		Connection con = getConnection();
		
		if (tableExists(rates_table) == false) {
			Logger.info("Creating "+rates_table+" table.");
			con.prepareStatement("CREATE TABLE `"+rates_table+"` (`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, `type` VARCHAR(16) NOT NULL, `rate` DOUBLE NOT NULL, `world` VARCHAR(16) NOT NULL) ENGINE = InnoDB").executeUpdate();
		}
		
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
	
	public static int getResultCount(String query, Object... args) throws SQLException {
		queryReturn qReturn = executeQuery(query, args);
		qReturn.result.first();
		int rows = qReturn.result.getInt(1);
		qReturn.close();
		return rows;
	}
	
	public static boolean tableExists(String tableName) throws SQLException {
		boolean exists = false;
		Connection con = getConnection();
		ResultSet result = con.prepareStatement("show tables like '" + tableName + "'").executeQuery();
		result.last();
		if (result.getRow() != 0) 
			exists = true;
		con.close();
		return exists;
	}

	
	private static String getURL(){
		return "jdbc:mysql://" + Config.getString("mysql.hostname") + ":" + Config.getInt("mysql.port") + "/" + Config.getString("mysql.database");
	}
	
	public static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(getURL(), Config.getString("mysql.username"), Config.getString("mysql.password"));
	}
	
	private Boolean canConnect(){
		try {
			@SuppressWarnings("unused")
			Connection con = getConnection();
		} catch (SQLException e) {
			return false;
		}
		return true;
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


	@Override
	public Double getRate(String entityType, String world) throws SQLException {
		queryReturn results = executeQuery("SELECT * FROM `"+rates_table+"` WHERE `type` LIKE ? AND `world` LIKE ? LIMIT 0 , 1", entityType, world);
		ResultSet r = results.result;
		Double rate = 1.0; //1 = 100%
		while (r.next()) {
			rate = r.getDouble("rate");
		}
		results.close();
		return rate;
	}


	@Override
	public Boolean setRate(String entityType, Double rate, String world) throws SQLException {
		int succsess = executeUpdate("UPDATE `"+rates_table+"` SET `rate` = ? WHERE `type` = ? AND `world` = ?;", rate, entityType, world);
		if (succsess > 0)
			return true;

		succsess = executeUpdate("INSERT INTO `"+rates_table+"` (`type` ,`rate`, `world`) VALUES (?, ?, ?);", entityType, rate, world);
		return (succsess > 0) ? true : false;
	}

	
}
