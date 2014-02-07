package com.cyprias.DynamicDropRate.database;

import java.sql.SQLException;

public interface Database {
	
	Boolean init();
	
	Double getRate(String entityType, String world) throws SQLException;
	
	Boolean setRate(String entityType, Double rate, String world) throws SQLException;
	
	
}
