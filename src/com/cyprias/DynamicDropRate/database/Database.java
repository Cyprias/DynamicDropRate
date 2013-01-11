package com.cyprias.DynamicDropRate.database;

import java.sql.SQLException;
import java.util.List;

import org.bukkit.command.CommandSender;

public interface Database {
	
	Boolean init();
	
	/*
	List<Note> list(CommandSender sender, int page) throws SQLException;
	
	Boolean init();
	
	Boolean create(CommandSender sender, Boolean notify, String player, String text) throws SQLException;
	
	Note info(int id) throws SQLException;
	
	List<Note> getPlayerNotifications(String playerName) throws SQLException;
	
	Boolean notify(int id) throws SQLException;
	
	
	List<Note> search(SearchParser parser) throws SQLException;
	
	Boolean remove(int id) throws SQLException;
	
	Note last() throws SQLException;
	*/
	
	Double getRate(String entityType) throws SQLException;
	
	Boolean setRate(String entityType, Double rate) throws SQLException;
	
	
}
