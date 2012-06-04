package tac.kbp.slotfilling.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class MySQLConnection {
		
	public static Connection getConnection(String username, String password, String url) throws Exception {	
		
		Connection connect = null;
		
		try {
			
			// This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.jdbc.Driver");
			
			// Setup the connection with the DB
			connect = DriverManager.getConnection(url, username, password);			
		}
		
		catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
			e.printStackTrace();
		}
		
		return connect;
	}
}
