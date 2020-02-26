package net.dotefekts.yalp;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.dotefekts.dotutils.DotUtilities;
import net.dotefekts.yalp.data.ConnectionBuilder;
import net.dotefekts.yalp.data.DataAccess;

public class LootBoxes extends JavaPlugin {
	private Connection sqlConnection;
	private DataAccess dataAccess;
	private LootBoxManager boxManager;
	private LootBoxEventHandler eventHandler;
	
    @Override
    public void onEnable() {
    	String path = this.getDataFolder().getAbsolutePath();
    	File file = new File(path);
    	file.mkdir();
    	
    	sqlConnection = ConnectionBuilder.getConnection(path + File.separator + "lootboxes.db");
    	if(sqlConnection != null) {
    		try {
    			dataAccess = new DataAccess(sqlConnection);
    			boxManager = new LootBoxManager(this, dataAccess);
            	eventHandler = new LootBoxEventHandler(this, boxManager);
            	
            	DotUtilities.getCommandHelper().registerCommands(eventHandler, this);
            	Bukkit.getPluginManager().registerEvents(eventHandler, this);
    		} catch(SQLException e) {
    			e.printStackTrace();
            	getLogger().severe("Failed to load existing lootboxes. Disabling...");
        		this.getPluginLoader().disablePlugin(this);
    		}
    	} else {
        	getLogger().severe("Failed to establish a database connection. Disabling...");
    		this.getPluginLoader().disablePlugin(this);
    	}
    	
    	getLogger().info("YALP has finished loading.");
    }

    @Override
    public void onDisable() {
		if(boxManager != null)
			boxManager.removeWorldTitles();
			boxManager.flushBoxUpdates();
		
		try {
			sqlConnection.close();
		} catch (SQLException e) {
			e.printStackTrace();
			getLogger().severe("Error attempting to close database connection.");
		}
    	
    	getLogger().info("YALP has finished disabling.");
    }
}
