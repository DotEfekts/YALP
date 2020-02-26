package net.dotefekts.yalp;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class PlayerOpen {
	private LootBox box;
	private UUID playerUUID;
	private ItemStack[] itemsLeft;
	private OffsetDateTime lastOpened;
	
	public PlayerOpen(LootBox box, UUID playerUUID, ItemStack[] itemsLeft, OffsetDateTime lastOpened) {
		this.box = box;
		this.playerUUID = playerUUID;
		this.itemsLeft = itemsLeft;
		this.lastOpened = lastOpened;
	}
	
	public PlayerOpen(LootBox box, UUID playerUUID, ItemStack[] contents) {
		this(box, playerUUID, contents, OffsetDateTime.now(ZoneId.of("Z")));
	}

	public OffsetDateTime getDateOpened() {
		return lastOpened;
	}

	public Duration cooldownRemaining() {
		return Duration.between(OffsetDateTime.now(ZoneId.of("Z")), lastOpened.plusSeconds(box.getCooldown()));
	}

	public void markOpened() {
		this.lastOpened = OffsetDateTime.now(ZoneId.of("Z"));
	}
	
	public LootBox getBox() {
		return box;
	}
	
	public boolean hasItems() {
		boolean empty = true;
		for(ItemStack item : itemsLeft)
			if(item != null)
				empty = false;
		return !empty;
	}

	public ItemStack[] getContents() {
		return itemsLeft;
	}

	public void setContents(ItemStack[] contents) {
		this.itemsLeft = contents;
	}
	
	public UUID getUUID() {
		return playerUUID;
	}
}
