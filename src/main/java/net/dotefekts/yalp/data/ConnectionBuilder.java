package net.dotefekts.yalp.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionBuilder {
	
	public static Connection getConnection(String path) {
		try {
			String url = "jdbc:sqlite:" + path;
			return DriverManager.getConnection(url);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
}
