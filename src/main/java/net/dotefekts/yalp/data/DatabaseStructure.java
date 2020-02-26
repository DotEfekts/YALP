package net.dotefekts.yalp.data;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseStructure {
	private Connection connection;
	
	public DatabaseStructure(Connection connection) {
		this.connection = connection;
	}
	
	public boolean ensureStructure() {
		try {
			if(!connection.isValid(10))
				return false;
			
			connection.prepareStatement("CREATE TABLE IF NOT EXISTS LootBoxes"
					+ "(Location		VARCHAR(65535) PRIMARY KEY,"
					+ " Inventory		VARCHAR(65535),"
					+ " Title			VARCHAR(65535),"
					+ " OverheadTitle	VARCHAR(65535),"
					+ " Cost			VARCHAR(65535),"
					+ "	Cooldown		INTEGER)").execute();
			
			connection.prepareStatement("CREATE TABLE IF NOT EXISTS PlayerOpens"
					+ "(BoxLocation		INTEGER NOT NULL REFERENCES LootBoxes(Location),"
					+ "	PlayerUUID		VARCHAR(36) NOT NULL,"
					+ " Inventory		VARCHAR(65535),"
					+ " Opened			TIMESTAMP,"
					+ " PRIMARY KEY (BoxLocation, PlayerUUID))").execute();
			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}
