package net.dotefekts.yalp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class LootBox {
	private Location location;
	private ItemStack[] contents;
	private String title;
	private String overheadTitle;
	private ItemStack cost;
	private ArmorStand worldTitle;
	private int cooldown;
	
	public LootBox(Location location, ItemStack[] contents, String title, String overheadTitle, ItemStack cost, int cooldown) {
		this.location = location; 
		this.contents = contents; 
		this.title = title; 
		this.overheadTitle = overheadTitle; 
		this.cost = cost; 
		this.cooldown = cooldown; 
		
		if(this.overheadTitle != null && !this.overheadTitle.trim().isEmpty()) {
			createWorldTitle();
		}
	}
	
	private void createWorldTitle() {
		if(this.worldTitle == null) {
			this.worldTitle = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(0.5, 1, 0.5), EntityType.ARMOR_STAND);
			
			this.worldTitle.setVisible(false);
			this.worldTitle.setGravity(false);
			this.worldTitle.setCanMove(false);
			this.worldTitle.setCanPickupItems(false);
			this.worldTitle.setRemoveWhenFarAway(false);
			
			this.worldTitle.setSmall(true);
			this.worldTitle.setMarker(true);
			this.worldTitle.setInvulnerable(true);
			this.worldTitle.setCustomNameVisible(true);
			
			this.worldTitle.setCustomName(this.overheadTitle);
		}
	}

	public void destroyWorldTitle() {
		if(this.worldTitle != null) {
			this.worldTitle.remove();
			this.worldTitle = null;
		}
	}

	public Location getLocation() {
		return location;
	}
	
	public ItemStack[] getContents() {
		return contents;
	}
	
	public void setContents(ItemStack[] contents) {
		this.contents = contents;
	}
	
	public Inventory createInventory(Player player) {
		Inventory playerInv = Bukkit.createInventory(player, contents.length, title);
		playerInv.setContents(contents);
		return playerInv;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
		
	public String getOverheadTitle() {
		return overheadTitle;
	}
		
	public void setOverheadTitle(String overheadTitle) {
		this.overheadTitle = overheadTitle;
		if(this.overheadTitle != null && !this.overheadTitle.trim().isEmpty()) {
			if(this.worldTitle != null)
				this.worldTitle.setCustomName(overheadTitle);
			else
				this.createWorldTitle();
		} else {
			this.destroyWorldTitle();
		}
	}
	
	public ItemStack getCost() {
		return cost;
	}
	
	public void setCost(ItemStack cost) {
		this.cost = cost;
	}
	
	public int getCooldown() {
		return cooldown;
	}

	public void setCooldown(int cooldown) {
		this.cooldown = cooldown;
	}
}
