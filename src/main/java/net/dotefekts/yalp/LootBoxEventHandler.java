package net.dotefekts.yalp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;

import net.dotefekts.dotutils.DotUtilities;
import net.dotefekts.dotutils.commandhelper.CommandEvent;
import net.dotefekts.dotutils.commandhelper.CommandHandler;
import net.dotefekts.dotutils.commandhelper.PermissionHandler;
import net.dotefekts.dotutils.menuapi.ButtonListener;
import net.dotefekts.dotutils.menuapi.InternalMenuException;
import net.dotefekts.dotutils.menuapi.Menu;
import net.dotefekts.dotutils.menuapi.MenuButton;
import net.dotefekts.dotutils.menuapi.MenuManager;

public class LootBoxEventHandler implements ButtonListener, Listener {
	private LootBoxes plugin;
	private Logger logger;
	private LootBoxManager boxManager;
	private MenuManager menuManager;
	private ArrayList<UUID> editMode;
	private HashMap<Player, Menu> playerMenus;
	private HashMap<Menu, LootBox> menuBoxes;
	private HashMap<Player, EditType> activeEdits;
	
	public LootBoxEventHandler(LootBoxes plugin, LootBoxManager boxManager) {
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.boxManager = boxManager;
		
		editMode = new ArrayList<UUID>();
		menuManager = DotUtilities.getMenuManager();
		
		menuBoxes = new HashMap<Menu, LootBox>();
		playerMenus = new HashMap<Player, Menu>();
		activeEdits = new HashMap<Player, EditType>();
	}
	
	public void setMenuBox(Menu menu, LootBox box) {
		menuBoxes.put(menu, box);
		playerMenus.put(menu.getPlayer(), menu);
	}
	
	@PermissionHandler(description = Permissions.MANAGE_BOXES.Description, node = Permissions.MANAGE_BOXES.Name, permissionDefault = PermissionDefault.OP)
	@CommandHandler(command = "lootbox", description = "Enables lootbox management mode.", format = "n", serverCommand = false)
	public boolean lootBoxCommand(CommandEvent event) {
		Player player = (Player) event.getSender();
		if(editMode.contains(player.getUniqueId())) {
			editMode.remove(player.getUniqueId());
			event.getSender().sendMessage(ChatColor.YELLOW + "You are no longer editing lootboxes.");
		} else {
			editMode.add(player.getUniqueId());
			event.getSender().sendMessage(ChatColor.YELLOW + "Right click an inventory to turn it into a lootbox or edit an existing lootbox.");
		}
		
		return true;
	}
	
	@EventHandler
	public void playerLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		editMode.remove(player.getUniqueId());
		activeEdits.remove(player);
		
		Menu menu = playerMenus.get(player);
		if(menu != null) {
			playerMenus.remove(player);
			menuBoxes.remove(menu);
		}
	}
	
	@Override
	public boolean buttonClicked(Menu menu, MenuButton buttonClicked) {
		Player player = menu.getPlayer();
		LootBox box = menuBoxes.get(menu);
		if(box != null) {
			int buttonPos = buttonClicked.getPos();
			if(buttonPos == 0) {
				Inventory inv = Bukkit.createInventory(menu.getPlayer(), box.getContents().length);
				inv.setContents(box.getContents());
				activeEdits.put(player, EditType.CONTENTS);
				player.openInventory(inv);
				return false;
			} else if(buttonPos == 1) {
				player.sendMessage(ChatColor.YELLOW + "Type the new title in chat. Type q to cancel. Use \\s to insert a section sign.");
				activeEdits.put(player, EditType.TITLE);
			} else if(buttonPos == 2) {
				player.sendMessage(ChatColor.YELLOW + "Type the new world title in chat. Type q to cancel. Use \\s to insert a section sign.");
				activeEdits.put(player, EditType.WORLD_TITLE);
			} else if(buttonPos == 3) {
				player.sendMessage(ChatColor.YELLOW + "Right click the lootbox with an item stack to set the new cost (includes NBT data). Right click with nothing to remove the cost.");
				activeEdits.put(player, EditType.COST);
			} else if(buttonPos == 4) {
				player.sendMessage(ChatColor.YELLOW + "Type the new cooldown in chat in seconds. Type q to cancel. Cooldown must be between 0 and 31,556,952. You can also set the cooldown to -1 to restrict each player to a single use.");
				activeEdits.put(player, EditType.COOLDOWN);
			} else if(buttonPos == 8) {
				boxManager.remove(box);
				
				if(box.getLocation().getBlock().getType() == Material.ENDER_CHEST)
					player.sendMessage(ChatColor.RED + "Lootbox removed. Contents have been destroyed as container is an ender chest.");
				else
					player.sendMessage(ChatColor.RED + "Lootbox removed. Contents have been returned to block.");
				
				menuBoxes.remove(menu);
				playerMenus.remove(player);
				menu.markDestruction();
			}
		} else {
			player.sendMessage(ChatColor.RED + "An error occurred. You are not currently editing a lootbox.");
		}
		
		return true;
	}
	
	@EventHandler
	public void playerInteract(PlayerInteractEvent event) {
		if(!event.getPlayer().isSneaking() && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
			Player player = event.getPlayer();
			Location location = event.getClickedBlock().getLocation();
			EditType editType = activeEdits.get(player);
			
			if(editType == EditType.COST) {
				Menu playerMenu = playerMenus.get(player);
				LootBox box = menuBoxes.get(playerMenu);
				if(box.getLocation().equals(location)) {
					event.setCancelled(true);

					box.setCost(event.getItem());
					player.sendMessage(ChatColor.YELLOW + "Cost updated.");
					activeEdits.remove(player);
					
					try {
						playerMenus.get(player).showMenu();
					} catch (InternalMenuException e) {
						plugin.getLogger().warning("Failed when trying to reopen lootbox edit menu.");
					}
				}
			} else if(event.getClickedBlock().getState() instanceof InventoryHolder) {
				InventoryHolder ih = (InventoryHolder) event.getClickedBlock().getState();
				Inventory inv = ih.getInventory();
				if(isValidInventory(inv)) {
					if(inv.getLocation() != null) {
						LootBox box = boxManager.getBox(location);
						boolean isEditing = editMode.contains(player.getUniqueId());
	
						if(isEditing) {
							event.setCancelled(true);
							
							if(box == null) {
								if(ih.getInventory() instanceof DoubleChestInventory) {
									player.sendMessage(ChatColor.RED + "Sorry, you cannot make a double chest into a lootbox.");
									return;
								}
								
								box = boxManager.createBox(inv, location);
								inv.setContents(new ItemStack[inv.getSize()]);
								player.sendMessage(ChatColor.YELLOW + "Created new lootbox.");
							}
							
							try {
								Menu menu = boxManager.createEditMenu(player, box, this);
								setMenuBox(menu, box);
								menu.showMenu();
							} catch (InternalMenuException e) {
								e.printStackTrace();
								logger.severe("An error occurred while attempting to open a lootbox edit menu.");
								player.sendMessage(ChatColor.RED + "An error occurred while attempting to open the edit menu.");
							}
						} else if(box != null) {
							boxManager.openBox(player, box);
							event.setCancelled(true);
						}
					}
				}
			} else if(event.getClickedBlock().getType() == Material.ENDER_CHEST) {
				LootBox box = boxManager.getBox(location);
				boolean isEditing = editMode.contains(player.getUniqueId());

				if(isEditing) {
					if(box == null) {
						box = boxManager.createBox(
								new ItemStack[InventoryType.ENDER_CHEST.getDefaultSize()], 
								InventoryType.ENDER_CHEST.getDefaultTitle(), 
								location);
						
						player.sendMessage(ChatColor.YELLOW + "Created new lootbox.");
					}
					
					try {
						Menu menu = boxManager.createEditMenu(player, box, this);
						setMenuBox(menu, box);
						menu.showMenu();
					} catch (InternalMenuException e) {
						e.printStackTrace();
						logger.severe("An error occurred while attempting to open a lootbox edit menu.");
						player.sendMessage(ChatColor.RED + "An error occurred while attempting to open the edit menu.");
					}
					
					
					event.setCancelled(true);
				} else if(box != null) {
					boxManager.openBox(player, box);
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void blockPlaced(BlockPlaceEvent event) {
		if(!event.getPlayer().isSneaking()
				&& (event.getBlockPlaced().getType() == Material.CHEST || event.getBlockPlaced().getType() == Material.TRAPPED_CHEST)) {
			Material material = event.getBlockPlaced().getType();
			Chest placed = (Chest) event.getBlockPlaced().getBlockData();
			boolean cancel = false;
			
			if(placed.getFacing() == BlockFace.NORTH || placed.getFacing() == BlockFace.SOUTH) {
				Block westBlock = event.getBlock().getRelative(BlockFace.WEST);
				LootBox westBox = boxManager.getBox(westBlock.getLocation());
				if(westBox != null && westBlock.getType() == material && ((Chest) westBlock.getBlockData()).getFacing() == placed.getFacing()) {
					cancel = true;
				} else {
					Block eastBlock = event.getBlock().getRelative(BlockFace.EAST);
					LootBox eastBox = boxManager.getBox(eastBlock.getLocation());
					if(eastBox != null && eastBlock.getType() == material && ((Chest) eastBlock.getBlockData()).getFacing() == placed.getFacing())
						cancel = true;
				}
			} else {
				Block northBlock = event.getBlock().getRelative(BlockFace.NORTH);
				LootBox northBox = boxManager.getBox(northBlock.getLocation());
				if(northBox != null && northBlock.getType() == material && ((Chest) northBlock.getBlockData()).getFacing() == placed.getFacing()) {
					cancel = true;
				} else {
					Block southBlock = event.getBlock().getRelative(BlockFace.SOUTH);
					LootBox southBox = boxManager.getBox(southBlock.getLocation());
					if(southBox != null && southBlock.getType() == material && ((Chest) southBlock.getBlockData()).getFacing() == placed.getFacing())
						cancel = true;
				}
			}
			
			if(cancel) {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "Sorry, you cannot place this next to a lootbox.");
			}
		}
			
	}
	
	@EventHandler
	public void blockBroken(BlockBreakEvent event) {
		LootBox box = boxManager.getBox(event.getBlock().getLocation());
		if(box != null) {
			if(event.getPlayer().hasPermission(Permissions.MANAGE_BOXES.Permission))
				event.getPlayer().sendMessage(ChatColor.RED + "You must remove this lootbox via the edit menu.");
			else
				event.getPlayer().sendMessage(ChatColor.RED + "You cannot break that.");
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void inventoryClosed(InventoryCloseEvent event) {
		boxManager.notifyClose(event.getView());
		
		if(event.getPlayer() instanceof Player) {
			Menu menu = menuManager.findMenu(event.getInventory());
			Player player = (Player) event.getPlayer();
			if(activeEdits.containsKey(player)) {
				if(menu == null && activeEdits.get(player) == EditType.CONTENTS) {
					Menu playerMenu = playerMenus.get(player);
					menuBoxes.get(playerMenu).setContents(event.getInventory().getContents());
					player.sendMessage(ChatColor.YELLOW + "Contents updated.");
					activeEdits.remove(player);
					try {
						playerMenu.showMenu();
					} catch (InternalMenuException e) {
						plugin.getLogger().warning("Failed when trying to reopen lootbox edit menu.");
					}
				}
			} else if(menu != null && menuBoxes.containsKey(menu)) {
				menuBoxes.remove(menu);
				playerMenus.remove(player);
				activeEdits.remove(player);
				menu.markDestruction();
			}
		}
	}
	
	private InventoryAction[] blockedActions = {
		InventoryAction.HOTBAR_SWAP, 
		InventoryAction.PLACE_ALL, 
		InventoryAction.PLACE_ONE,
		InventoryAction.PLACE_SOME,
		InventoryAction.SWAP_WITH_CURSOR
	};
	
	@EventHandler
	public void inventoryClick(InventoryClickEvent event) {
		if(Arrays.asList(blockedActions).contains(event.getAction())
				&& boxManager.getBoxByInv(event.getClickedInventory()) != null)
			event.setCancelled(true);
		else if(event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
				&& event.getInventory() != event.getClickedInventory()
				&& boxManager.getBoxByInv(event.getInventory()) != null)
			event.setCancelled(true);
			
	}
	
	@EventHandler
	public void inventoryDrag(InventoryDragEvent event) {
		LootBox box = boxManager.getBoxByInv(event.getInventory());
		if(box != null) {
			boolean isInBox = false;
			for(int slot : event.getRawSlots())
				if(slot < box.getContents().length)
					isInBox = true;
			if(isInBox)
				event.setCancelled(true);
		}
			
	}
	
	@EventHandler
	public void playerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		EditType editType = activeEdits.get(player);
		if(editType != null && (editType == EditType.COOLDOWN || editType == EditType.TITLE || editType == EditType.WORLD_TITLE)) {
			event.setCancelled(true);
			
			if(event.getMessage().equalsIgnoreCase("q")) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					activeEdits.remove(player);
					
					try {
						playerMenus.get(player).showMenu();
					} catch (InternalMenuException e) {
						plugin.getLogger().warning("Failed when trying to reopen lootbox edit menu.");
					}
				});
			} else if(editType == EditType.COOLDOWN) {
				try {
					int cooldown = Integer.parseInt(event.getMessage().replace(",", ""));
					if(cooldown < -1 || cooldown > 31556952) {
						event.getPlayer().sendMessage(ChatColor.RED + "Cooldown must be between 0 and 31,556,952. You can also set the cooldown to -1 to restrict each player to a single use.");
					} else {
						Bukkit.getScheduler().runTask(plugin, () -> {
							Menu playerMenu = playerMenus.get(player);
							menuBoxes.get(playerMenu).setCooldown(cooldown);
							player.sendMessage(ChatColor.YELLOW + "Cooldown updated.");
							activeEdits.remove(player);
							
							try {
								playerMenus.get(player).showMenu();
							} catch (InternalMenuException e) {
								plugin.getLogger().warning("Failed when trying to reopen lootbox edit menu.");
							}
						});
					}
				} catch(NumberFormatException e) {
					event.getPlayer().sendMessage(ChatColor.RED + "Cooldown must be between 0 and 31,556,952. You can also set the cooldown to -1 to restrict each player to a single use.");
				}
			} else {
				Bukkit.getScheduler().runTask(plugin, () -> {
					Menu playerMenu = playerMenus.get(player);
					
					if(editType == EditType.TITLE) {
						menuBoxes.get(playerMenu).setTitle(event.getMessage().replaceAll("\\\\s", "§"));
						playerMenus.get(player).setTitle("Editing " + event.getMessage().replaceAll("\\\\s", "§"));
						player.sendMessage(ChatColor.YELLOW + "Title updated.");
					} else {
						menuBoxes.get(playerMenu).setOverheadTitle(event.getMessage().replaceAll("\\\\s", "§"));
						player.sendMessage(ChatColor.YELLOW + "World title updated.");
					}

					activeEdits.remove(player);
					
					try {
						playerMenus.get(player).showMenu();
					} catch (InternalMenuException e) {
						plugin.getLogger().warning("Failed when trying to reopen lootbox edit menu.");
					}
				});
			}
		}
	}
	
	@EventHandler
	public void worldSaved(WorldSaveEvent event) {
		boxManager.flushBoxUpdates();
	}
	
	private static boolean isValidInventory(Inventory inventory) {
		switch(inventory.getType()) {
			case BARREL:
			case CHEST:
			case SHULKER_BOX:
				return true;
			default:
				return false;
		}
	}
	
	private enum EditType {
		CONTENTS,
		TITLE,
		WORLD_TITLE,
		COST,
		COOLDOWN
	}
}
