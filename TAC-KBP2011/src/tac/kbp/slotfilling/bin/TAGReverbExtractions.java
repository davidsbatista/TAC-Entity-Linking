package tac.kbp.slotfilling.bin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.mysql.jdbc.PreparedStatement;

import edu.stanford.nlp.util.Triple;

import tac.kbp.slotfilling.configuration.Definitions;
import tac.kbp.slotfilling.relations.ReverbRelation;
import tac.kbp.utils.SHA1;

public class TAGReverbExtractions {
	
	public static void main(String[] args) throws Exception {
		
		Definitions.getDBConnection();
		/* support doc and named-entities recognizer */
	}
		
	public static void getRelations(String table) throws SQLException {
		
		PreparedStatement stm = (PreparedStatement) Definitions.connection.prepareStatement("SELECT arg1,rel,arg2, sentence FROM ?");
		stm.setString(1, table);
		
		ResultSet resultSet = stm.executeQuery();
		LinkedList<ReverbRelation> relations = new LinkedList<ReverbRelation>();
		
		while (resultSet.next()) {
			String arg1 = resultSet.getString(1);			
			String rel = resultSet.getString(2);
			String arg2 = resultSet.getString(3);
			String sentence = resultSet.getString(3);
		}
	}
}
