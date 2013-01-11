package com.cyprias.DynamicDropRate.database;

import java.sql.SQLException;

public interface Database {
	
	Boolean init();
	
	Double getRate(String entityType) throws SQLException;
	
	Boolean setRate(String entityType, Double rate) throws SQLException;
	
	
}
