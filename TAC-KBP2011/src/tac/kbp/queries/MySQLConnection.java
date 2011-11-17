package tac.kbp.queries;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLConnection {

    public static void main(String[] args) throws SQLException {

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        String url = "jdbc:mysql://agatha:3306/tac_kbp";
        String user = "tac";
        String password = "tackbp";

        try {
            con = DriverManager.getConnection(url, user, password);
            st = con.createStatement();
            rs = st.executeQuery("SELECT VERSION()");

            if (rs.next()) {
                System.out.println(rs.getString(1));
            }
        
	        PreparedStatement pst = con.prepareStatement("SELECT * FROM  entity ");
	        rs = pst.executeQuery();
	
	        while (rs.next()) {
	        	/*
	            System.out.print(rs.getString(2));
	            System.out.print(": ");
	            System.out.print(rs.getString(3));
	            System.out.print(": ");
	            System.out.print(rs.getString(4));
	            System.out.println("\n");
	            */
	        }
	        System.out.println("Done");
        
        } catch (SQLException ex) {
        	System.out.println(ex);
            System.out.println(ex.getMessage()); 
        }
        
        finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (st != null) {
                    st.close();
                }
                if (con != null) {
                    con.close();
                }

            } catch (SQLException ex) {
            	System.out.println(ex);
                System.out.println(ex.getMessage());
            }
        }

    }
}