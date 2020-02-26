package net.dotefekts.yalp;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class Permissions {
	public static class MANAGE_BOXES {
		public static final String Name = "yalp.manage";
		public static final String Description = "Allows for managment of lootboxes.";
		public static final PermissionDefault Default = PermissionDefault.OP;
		
		public static final Permission Permission = new Permission(
				Name,
				Description,
				Default
		);
	}
	
	public static class BYPASS_COST {
		public static final String Name = "yalp.bypass.cost";
		public static final String Description = "Allows a user to ignore the cost of opening a lootbox.";
		public static final PermissionDefault Default = PermissionDefault.OP;
		
		public static final Permission Permission = new Permission(
				Name,
				Description,
				Default
		);
	}
	
	public static class BYPASS_COOLDOWN {
		public static final String Name = "yalp.bypass.cooldown";
		public static final String Description = "Allows a user to ignore the cooldown for opening a lootbox.";
		public static final PermissionDefault Default = PermissionDefault.OP;
		
		public static final Permission Permission = new Permission(
				Name,
				Description,
				Default
		);
	}
}
