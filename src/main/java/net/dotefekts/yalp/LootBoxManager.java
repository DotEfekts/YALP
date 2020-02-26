package net.dotefekts.yalp;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;

import net.dotefekts.dotutils.DotUtilities;
import net.dotefekts.dotutils.menuapi.ButtonListener;
import net.dotefekts.dotutils.menuapi.Menu;
import net.dotefekts.dotutils.menuapi.MenuManager;
import net.dotefekts.yalp.data.DataAccess;
import net.md_5.bungee.api.ChatColor;

public class LootBoxManager {
	private Logger logger;
	private DataAccess dataAccess;
	private ArrayList<LootBox> boxes;
	private ArrayList<LootBox> removedBoxes;
	private HashMap<String, LootBox> boxLocations;
	private HashMap<InventoryView, LootBox> openBoxes;
	private HashMap<String, Integer> interactionCount;
	private Map<UUID, List<PlayerOpen>> playerOpens;
	private ProtocolManager protocolManager;
	private MenuManager menuManager;
	
	public LootBoxManager(LootBoxes plugin, DataAccess dataAccess) throws SQLException {
		this.logger = plugin.getLogger();
		this.dataAccess = dataAccess;
		
		this.boxes = new ArrayList<LootBox>(Arrays.asList(dataAccess.loadLootBoxes()));
		
		this.removedBoxes = new ArrayList<LootBox>();
		this.boxLocations = new HashMap<String, LootBox>();
		
		for(LootBox box : boxes) {
			Location loc = box.getLocation();
			boxLocations.put(getLocationString(loc), box);
		}
		
		this.logger.info("LootBoxes loaded from the database.");

		this.playerOpens = dataAccess.loadOpens(this, getBoxes());
		
		this.openBoxes = new HashMap<InventoryView, LootBox>();
		this.interactionCount = new HashMap<String, Integer>();
		
		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.menuManager = DotUtilities.getMenuManager();
	}
	
	public LootBox[] getBoxes() {
		return boxes.toArray(new LootBox[boxes.size()]);
	}
	
	private LootBox[] getRemovedBoxes() {
		return removedBoxes.toArray(new LootBox[removedBoxes.size()]);
	}

	public LootBox getBox(Location loc) {
		return boxLocations.get(getLocationString(loc));
	}
	
	public LootBox createBox(Inventory inventory, Location loc) {
		return createBox(inventory.getContents(), inventory.getType().getDefaultTitle(), loc);
	}

	public LootBox createBox(ItemStack[] contents, String title, Location loc) {
		LootBox newBox = new LootBox(loc, contents, title, null, null, 0);
		
		boxes.add(newBox);
		boxLocations.put(getLocationString(loc), newBox);
	
		return newBox;
	}

	public Menu createEditMenu(Player player, LootBox box, ButtonListener listener) {
		Menu menu = menuManager.createMenu(player, 9, "Editing " + box.getTitle());
				
		menu.setButton(getNamedItem("Edit Contents", Material.CHEST), 0, 0, listener);
		menu.setButton(getNamedItem("Change Title", Material.NAME_TAG), 1, 0, listener);
		menu.setButton(getNamedItem("Change World Title", Material.ITEM_FRAME), 2, 0, listener);
		menu.setButton(getNamedItem("Change Cost", Material.GOLD_INGOT), 3, 0, listener);
		menu.setButton(getNamedItem("Change Cooldown", Material.CLOCK), 4, 0, listener);
		menu.setButton(getNamedItem("Remove Lootbox", Material.REDSTONE_BLOCK), 8, 0, listener);
		
		return menu;
	}
	
	private ItemStack getNamedItem(String itemName, Material material) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(itemName);
		item.setItemMeta(meta);
		
		return item;
	}

	public void openBox(Player player, LootBox box) {
		ItemStack[] contents = box.getContents();
		PlayerOpen boxOpen = null;
		
		if(playerOpens.containsKey(player.getUniqueId())) {
			for(PlayerOpen open : playerOpens.get(player.getUniqueId())) {
				if(open.getBox() == box) {
					boxOpen = open;
					
					if(open.hasItems()) {
						contents = open.getContents();
					} else if(!player.hasPermission(Permissions.BYPASS_COOLDOWN.Permission)) {
						if(box.getCooldown() == -1) {
							player.sendMessage(ChatColor.RED + "You've already opened this lootbox. You cannot open it again.");
							return;
						} else if(!open.cooldownRemaining().isNegative()) {
							Duration cooldown = open.cooldownRemaining();
							if(cooldown.toDays() / 7.0 > 1) {
								player.sendMessage(ChatColor.RED + 
										"You've recently opened this lootbox. You cannot open it again for " + 
										(int) Math.floor(cooldown.toDays() / 7.0) + " weeks.");
								return;
							} else if(cooldown.toDays() > 1) {
								player.sendMessage(ChatColor.RED + 
										"You've recently opened this lootbox. You cannot open it again for " + 
										(int) Math.floor(cooldown.toDays()) + " days.");
								return;
							} else if(cooldown.toHours() > 1) {
								player.sendMessage(ChatColor.RED + 
										"You've recently opened this lootbox. You cannot open it again for " + 
										(int) Math.floor(cooldown.toHours()) + " hours.");
								return;
							} else if(cooldown.toMinutes() > 1) {
								player.sendMessage(ChatColor.RED + 
										"You've recently opened this lootbox. You cannot open it again for " + 
										(int) Math.floor(cooldown.toMinutes()) + " minutes.");
								return;
							} else if(cooldown.getSeconds() > 1) {
								player.sendMessage(ChatColor.RED + 
										"You've recently opened this lootbox. You cannot open it again for " + 
										(int) Math.floor(cooldown.getSeconds()) + " seconds.");
								return;
							}
						}
					}
				}
			}
		}

		ItemStack cost = box.getCost();
		if(!player.hasPermission(Permissions.BYPASS_COST.Permission)) {
			if((boxOpen == null || !boxOpen.hasItems()) && cost != null) {
				String costName = cost.getItemMeta().hasDisplayName() ? cost.getItemMeta().getDisplayName() : parseItemName(cost) ;
				ItemStack heldItem = player.getInventory().getItemInMainHand();
				if(heldItem != null && heldItem.isSimilar(cost)) {
					if(heldItem.getAmount() >= cost.getAmount()) {
						heldItem.setAmount(heldItem.getAmount() - cost.getAmount());
					} else {
						player.sendMessage(ChatColor.RED + "This lootbox requires " + cost.getAmount() + " " + costName + ". " + 
								"You currently have " + heldItem.getAmount() + ".");
						return;
					}
				} else {
					player.sendMessage(ChatColor.RED + "This lootbox requires " + cost.getAmount() + " " + costName + (cost.getAmount() > 1 ? "s." : "."));
					return;
				}
			}
		}
		
		Inventory inv = Bukkit.createInventory(player, contents.length, box.getTitle());
		inv.setContents(contents);
		
		String locString = getLocationString(box.getLocation());
		
		if(interactionCount.containsKey(locString)) {
			int count = interactionCount.get(locString);
			interactionCount.put(locString, count++);
		} else {
			interactionCount.put(locString, 1);
			
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_ACTION);
            BlockPosition pos = new BlockPosition(box.getLocation().getBlockX(), box.getLocation().getBlockY(), box.getLocation().getBlockZ());
            
    		packet.getBlockPositionModifier().write(0, pos);
            packet.getIntegers().
            	write(0, 1).
            	write(1, 1);
            
            packet.getBlocks().
            	write(0, box.getLocation().getBlock().getType());
            
            protocolManager.broadcastServerPacket(packet);
            box.getLocation().getWorld().playSound(box.getLocation(), getSoundOpenType(box), 0.75f, 1f);
		}
		
		this.openBoxes.put(player.openInventory(inv), box);
		
		if(boxOpen != null) {
			boxOpen.markOpened();
		} else {
			if(!playerOpens.containsKey(player.getUniqueId())) {
				playerOpens.put(player.getUniqueId(), new ArrayList<PlayerOpen>());
			}
			
			playerOpens.get(player.getUniqueId()).add(new PlayerOpen(box, player.getUniqueId(), contents));
		}
	}
	
	private String parseItemName(ItemStack item) {
		return WordUtils.capitalizeFully(item.getType().toString().replaceAll("_", " "));
	}

	private Sound getSoundOpenType(LootBox box) {
		if(box.getLocation().getBlock().getState() instanceof InventoryHolder) {
			InventoryHolder holder = (InventoryHolder) box.getLocation().getBlock().getState();
			
			switch(holder.getInventory().getType()) {
				case BARREL:
					return Sound.BLOCK_BARREL_OPEN;
				case ENDER_CHEST:
					return Sound.BLOCK_ENDER_CHEST_OPEN;
				case SHULKER_BOX:
					return Sound.BLOCK_SHULKER_BOX_OPEN;
				case CHEST:
				default:
					return Sound.BLOCK_CHEST_OPEN;
			}
		} else {
			return Sound.BLOCK_CHEST_OPEN;
		}
	}

	public void notifyClose(InventoryView view) {
		if(this.openBoxes.containsKey(view)) {
			LootBox box = this.openBoxes.get(view);
			String locString = getLocationString(box.getLocation());
			
			this.openBoxes.remove(view);

			Integer count = interactionCount.get(locString);
			if(count == null || count == 1) {
				interactionCount.remove(locString);
				
	            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_ACTION);
	            BlockPosition pos = new BlockPosition(box.getLocation().getBlockX(), box.getLocation().getBlockY(), box.getLocation().getBlockZ());
	            
        		packet.getBlockPositionModifier().write(0, pos);
	            packet.getIntegers().
	            	write(0, 1).
	            	write(1, 0);
	            
	            packet.getBlocks().
	            	write(0, box.getLocation().getBlock().getType());
	            
	            protocolManager.broadcastServerPacket(packet);
	            box.getLocation().getWorld().playSound(box.getLocation(), getSoundCloseType(box), 0.75f, 1f);
			} else {
				interactionCount.put(locString, count--);
			}
			
			Player player = (Player) view.getPlayer();
			if(playerOpens.containsKey(player.getUniqueId())) {
				for(PlayerOpen open : playerOpens.get(player.getUniqueId())) {
					if(open.getBox() == box) {
						open.setContents(view.getTopInventory().getContents());
					}
				}
			}
		}
	}
	
	private Sound getSoundCloseType(LootBox box) {
		if(box.getLocation().getBlock().getState() instanceof InventoryHolder) {
			InventoryHolder holder = (InventoryHolder) box.getLocation().getBlock().getState();
			
			switch(holder.getInventory().getType()) {
				case BARREL:
					return Sound.BLOCK_BARREL_CLOSE;
				case ENDER_CHEST:
					return Sound.BLOCK_ENDER_CHEST_CLOSE;
				case SHULKER_BOX:
					return Sound.BLOCK_SHULKER_BOX_CLOSE;
				case CHEST:
				default:
					return Sound.BLOCK_CHEST_CLOSE;
			}
		} else {
			return Sound.BLOCK_CHEST_CLOSE;
		}
	}
	
	private String getLocationString(Location loc) {
		return 	loc.getWorld().getUID() + "," + 
				loc.getBlockX() + "," + 
				loc.getBlockY() + "," + 
				loc.getBlockZ();
	}

	public void remove(LootBox box) {
		box.destroyWorldTitle();
		
		if(box.getLocation().getBlock().getState() instanceof InventoryHolder) {
			InventoryHolder inv = (InventoryHolder) box.getLocation().getBlock().getState();
			inv.getInventory().setContents(box.getContents());
		}
		
		boxLocations.remove(getLocationString(box.getLocation()));
		
		Set<Entry<InventoryView, LootBox>> entries = openBoxes.entrySet();
		for(Entry<InventoryView, LootBox> pair : entries) {
			if(pair.getValue() == box) {
				pair.getKey().close();
				pair.getKey().getPlayer().sendMessage(ChatColor.YELLOW + "Sorry, the lootbox you were looking at has been removed.");
			}
		}

		removedBoxes.add(box);
		boxes.remove(box);
	}

	public void flushBoxUpdates() {
		try {
			// Run delete before save so that new boxes won't get deleted for having the same key.
			dataAccess.deleteLootBoxes(getRemovedBoxes());
			dataAccess.saveLootBoxes(getBoxes());
			dataAccess.saveOpens(playerOpens);
			
			logger.info("Lootboxes flushed to the database.");
		} catch (SQLException e) {
			e.printStackTrace();
			logger.severe("Error attempting to flush lootbox information to the database.");
		}
	}

	public void removeWorldTitles() {
		for(LootBox box : boxes)
			box.destroyWorldTitle();
	}
}
