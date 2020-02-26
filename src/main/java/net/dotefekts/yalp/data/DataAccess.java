package net.dotefekts.yalp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.dotefekts.yalp.PlayerOpen;
import net.dotefekts.yalp.LootBox;
import net.dotefekts.yalp.LootBoxManager;

public class DataAccess {
	private Connection connection;
	private DatabaseStructure structureProvider;
	
	public DataAccess(Connection connection) throws SQLException {
		this.connection = connection;
		structureProvider = new DatabaseStructure(connection);
		if(!structureProvider.ensureStructure())
			throw new SQLException("Structure could not be validated.");
	}
	
	public LootBox[] loadLootBoxes() throws SQLException {
		try {
			ResultSet lootboxFetch = connection.createStatement().executeQuery("SELECT * FROM LootBoxes");
			ArrayList<LootBox> boxes = new ArrayList<LootBox>();
		
			while(lootboxFetch.next()) {
				boxes.add(LootBoxSerializer.deserialize(
						lootboxFetch.getString("Location"), 
						lootboxFetch.getString("Inventory"), 
						lootboxFetch.getString("Title"), 
						lootboxFetch.getString("OverheadTitle"), 
						lootboxFetch.getString("Cost"), 
						lootboxFetch.getInt("Cooldown")));
			}
			
			return boxes.toArray(new LootBox[boxes.size()]);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException("An SQL error occurred while attempting to fetch a lootbox.");
		}
	}

	public HashMap<UUID, List<PlayerOpen>> loadOpens(LootBoxManager manager, LootBox[] boxes) throws SQLException {
		try {
			ResultSet opensFetch = connection.createStatement().executeQuery("SELECT * FROM PlayerOpens");
			ArrayList<PlayerOpen> opens = new ArrayList<PlayerOpen>();
		
			while(opensFetch.next()) {
				opens.add(LootBoxSerializer.deserializeOpen(
						manager, 
						opensFetch.getString("BoxLocation"), 
						opensFetch.getString("PlayerUUID"), 
						opensFetch.getString("Inventory"), 
						opensFetch.getTimestamp("Opened")));
			}

			HashMap<UUID, List<PlayerOpen>> map = new HashMap<UUID, List<PlayerOpen>>();
			
			for(PlayerOpen open : opens) {
				if(open != null) {
					UUID id = open.getUUID();
					if(!map.containsKey(id)) {
						map.put(id, new ArrayList<PlayerOpen>());
					}
					
					map.get(id).add(open);
				}
			}
			
			return map;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new SQLException("An SQL error occurred while attempting to fetch a lootbox.");
		}
		
		
	}
	
	public void saveLootBoxes(LootBox[] boxes) throws SQLException {
		try {
			connection.setAutoCommit(false);
			
			for(LootBox box : boxes) {
					PreparedStatement statement = connection.prepareStatement("REPLACE INTO LootBoxes ("
							+ "Location,"
							+ "Inventory,"
							+ "Title,"
							+ "OverheadTitle,"
							+ "Cost,"
							+ "Cooldown)"
							+ "VALUES(?, ?, ?, ?, ?, ?)");
					
					statement.setString(1, LootBoxSerializer.serializeLocation(box.getLocation()));
					statement.setString(2, LootBoxSerializer.serializeItems(box.getContents()));
					statement.setString(3, box.getTitle());
					statement.setString(4, box.getOverheadTitle());
					statement.setString(5, LootBoxSerializer.serializeCost(box.getCost()));
					statement.setInt(6, box.getCooldown());
					
					int result = statement.executeUpdate();
					if(result != 1)
						throw new SQLException();
			}
			
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			e.printStackTrace();
			throw new SQLException("An SQL error occurred while attempting to save lootboxes.");
		} finally {
			connection.setAutoCommit(true);
		}
	}

	public void saveOpens(Map<UUID, List<PlayerOpen>> playerOpens) throws SQLException {
		try {
			connection.setAutoCommit(false);
			
			for(Entry<UUID, List<PlayerOpen>> playerEntries : playerOpens.entrySet()) {
				for(PlayerOpen open : playerEntries.getValue()) {
					PreparedStatement statement = connection.prepareStatement("REPLACE INTO PlayerOpens ("
							+ "BoxLocation,"
							+ "PlayerUUID,"
							+ "Inventory,"
							+ "Opened)"
							+ "VALUES(?, ?, ?, ?)");
					
					statement.setString(1, LootBoxSerializer.serializeLocation(open.getBox().getLocation()));
					statement.setString(2, open.getUUID().toString());
					statement.setString(3, LootBoxSerializer.serializeItems(open.getContents()));
					statement.setTimestamp(4, Timestamp.valueOf(open.getDateOpened().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
					
					int result = statement.executeUpdate();
					if(result != 1)
						throw new SQLException();
				}
			}
			
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			e.printStackTrace();
			throw new SQLException("An SQL error occurred while attempting to save lootboxes.");
		} finally {
			connection.setAutoCommit(true);
		}
	}
		
	public void deleteLootBoxes(LootBox[] boxes) throws SQLException {
		try {
			connection.setAutoCommit(false);

			for(LootBox box : boxes) {
				PreparedStatement opensStatement = connection.prepareStatement("DELETE FROM PlayerOpens WHERE BoxLocation = ?");
				opensStatement.setString(1, LootBoxSerializer.serializeLocation(box.getLocation()));
				PreparedStatement boxStatement = connection.prepareStatement("DELETE FROM LootBoxes WHERE Location = ?");
				boxStatement.setString(1, LootBoxSerializer.serializeLocation(box.getLocation()));
				
				opensStatement.executeUpdate();
				boxStatement.executeUpdate();
			}
			
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			e.printStackTrace();
			throw new SQLException("An SQL error occurred while attempting to delete a lootbox.");
		} finally {
			connection.setAutoCommit(true);
		}
	}
}
