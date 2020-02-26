package net.dotefekts.yalp.data;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import net.dotefekts.yalp.LootBox;
import net.dotefekts.yalp.LootBoxManager;
import net.dotefekts.yalp.PlayerOpen;

public class LootBoxSerializer {

	public static LootBox deserialize(String location, String inventory, String title, String overheadTitle, String cost, int cooldown) {
		String[] ls = location.split(",");
		Location worldLocation = new Location(
				Bukkit.getWorld(UUID.fromString(ls[0])), 
				Double.parseDouble(ls[1]),
				Double.parseDouble(ls[2]),
				Double.parseDouble(ls[3]));
		
		ByteArrayInputStream itemsInput = new ByteArrayInputStream(inventory.getBytes());
		XMLDecoder itemsOutput = new XMLDecoder(itemsInput);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) itemsOutput.readObject();
		itemsOutput.close();
		
		ItemStack[] deserializedItems = new ItemStack[items.size()];
		for(int i = 0; i < deserializedItems.length; i++) {
			deserializedItems[i] = items.get(i) != null ? ItemStack.deserialize(items.get(i)) : null;
		}
		
		ByteArrayInputStream itemInput = new ByteArrayInputStream(cost.getBytes());
		XMLDecoder itemOutput = new XMLDecoder(itemInput);
		Object output = itemOutput.readObject();
		@SuppressWarnings("unchecked")
		ItemStack item = output != null ? ItemStack.deserialize((Map<String, Object>) output) : null;
		itemOutput.close();
		
		return new LootBox(worldLocation, deserializedItems, title, overheadTitle, item, cooldown);
	}

	public static PlayerOpen deserializeOpen(LootBoxManager manager, String location, String uuid, String itemsLeft, Timestamp timestamp) {
		String[] ls = location.split(",");
		Location worldLocation = new Location(
				Bukkit.getWorld(UUID.fromString(ls[0])), 
				Double.parseDouble(ls[1]),
				Double.parseDouble(ls[2]),
				Double.parseDouble(ls[3]));
		
		LootBox box = manager.getBox(worldLocation);
		if(box == null)
			return null;
		
		ByteArrayInputStream itemsInput = new ByteArrayInputStream(itemsLeft.getBytes());
		XMLDecoder itemsOutput = new XMLDecoder(itemsInput);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) itemsOutput.readObject();
		itemsOutput.close();
		
		ItemStack[] deserializedItems = new ItemStack[items.size()];
		for(int i = 0; i < deserializedItems.length; i++) {
			deserializedItems[i] = items.get(i) != null ? ItemStack.deserialize(items.get(i)) : null;
		}
				
		return new PlayerOpen(
				box, 
				UUID.fromString(uuid), 
				deserializedItems, 
				OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp.getTime()), ZoneId.of("Z")));
	}
	
	public static String serializeLocation(Location location) {
		return location.getWorld().getUID() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
	}
	
	public static String serializeItems(ItemStack[] items) {
		List<Map<String, Object>> serializedItems = new ArrayList<Map<String, Object>>();
		for(ItemStack item : items) {
			serializedItems.add(item != null ? item.serialize() : null);
		}
		
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		
		XMLEncoder output = new XMLEncoder(byteStream);
		output.writeObject(serializedItems);
		output.close();
		
		return byteStream.toString();
	}
	
	public static String serializeCost(ItemStack item) {
		Map<String, Object> serializedItem = item != null ? item.serialize() : null;
		
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		
		XMLEncoder output = new XMLEncoder(byteStream);
		output.writeObject(serializedItem);
		output.close();
		
		return byteStream.toString();
	}
}
