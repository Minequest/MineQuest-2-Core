package com.theminequest.MineQuest;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.alta189.simplesave.Database;
import com.alta189.simplesave.DatabaseFactory;
import com.alta189.simplesave.exceptions.ConnectionException;
import com.alta189.simplesave.exceptions.TableRegistrationException;
import com.alta189.simplesave.h2.H2Configuration;
import com.alta189.simplesave.mysql.MySQLConfiguration;
import com.alta189.simplesave.query.QueryResult;
import com.alta189.simplesave.sqlite.SQLiteConfiguration;
import com.theminequest.MineQuest.API.CompleteStatus;
import com.theminequest.MineQuest.API.Managers;
import com.theminequest.MineQuest.API.Quest.Quest;
import com.theminequest.MineQuest.API.Tracker.QuestStatistic;
import com.theminequest.MineQuest.API.Tracker.StatisticManager;
import com.theminequest.MineQuest.API.Tracker.StatisticManager.Statistic;
import com.theminequest.MineQuest.API.Utils.PropertiesFile;

public class Statistics implements StatisticManager, Listener {
	
	private enum Mode{
		MySQL, SQlite, H2;
	}
	private Mode databasetype;
	private Database backend;
	
	public Statistics() throws ConnectionException{
		Managers.log("[SQL] Loading and connecting to SQL...");
		PropertiesFile config = MineQuest.configuration.databaseConfig;
		String dbtype = config.getString("db_type","h2");
		if (dbtype.equalsIgnoreCase("mysql"))
			databasetype = Mode.MySQL;
		else if (dbtype.equalsIgnoreCase("sqlite"))
			databasetype = Mode.SQlite;
		else
			databasetype = Mode.H2;
		Managers.log("[SQL] Using "+databasetype.name()+" as database.");
		String hostname = config.getString("db_hostname","localhost");
		String port = config.getString("db_port","3306");
		String databasename = config.getString("db_name","minequest");
		String username = config.getString("db_username","root");
		String password = config.getString("db_password","toor");
		if (databasetype == Mode.MySQL)
			backend = DatabaseFactory.createNewDatabase(new MySQLConfiguration().setHost(hostname).setDatabase(databasename).setPort(Integer.parseInt(port)).setUser(username).setPassword(password));
		else if (databasetype == Mode.SQlite) {
			SQLiteConfiguration s = new SQLiteConfiguration();
			s.setPath(Managers.getActivePlugin().getDataFolder().getAbsolutePath() + File.separator + "minequest_sqlite");
			backend = DatabaseFactory.createNewDatabase(s);
		} else
			backend = DatabaseFactory.createNewDatabase(new H2Configuration().setDatabase(Managers.getActivePlugin().getDataFolder().getAbsolutePath() + File.separator + "minequest_h2"));
	}

	@Override
	public Database getStorageBackend() {
		return backend;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Statistic> List<T> getStatistics(String playerName, Class<? extends Statistic> tableClazz) {
		List<? extends Statistic> result = backend.select(tableClazz).where().equal("playerName", playerName.toLowerCase()).execute().find();
		for (Statistic s : result)
			s.setup();
		return (List<T>) result;
	}

	@Override
	public <T extends Statistic> void saveStatistic(T statistic,	Class<? extends Statistic> tableClazz) {
		backend.save(tableClazz,statistic);
	}

	@Override
	public void registerStatistic(Class<? extends Statistic> tableClazz) {
		try {
			backend.registerTable(tableClazz);
		} catch (TableRegistrationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void connect(boolean connect) throws ConnectionException {
		if (connect)
			backend.connect();
		else
			backend.close();
	}

	@Override
	public <T extends Statistic> List<T> getStatisticList(
			Class<? extends Statistic> tableClazz) {
		QueryResult<? extends Statistic> r = backend.select(tableClazz).execute();
		return (List<T>) r.find();
	}

	@Override
	public <T extends Statistic> T createStatistic(String playerName,
			Class<? extends Statistic> tableClazz) {
		try {
			T s = (T) tableClazz.newInstance();
			s.setPlayerName(playerName);
			s.setup();
			return s;
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends Statistic> void removeStatistic(T statistic,
			Class<? extends Statistic> tableClazz) {
		backend.remove(tableClazz, statistic);
	}
	
}
