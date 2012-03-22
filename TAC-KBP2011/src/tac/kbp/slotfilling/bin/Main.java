package tac.kbp.slotfilling.bin;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;

import tac.kbp.entitylinking.bin.Definitions;
import tac.kbp.slotfilling.queries.LoadQueries;
import tac.kbp.slotfilling.queries.SlotFillingQuery;

public class Main {
	
	public static void main(String[] args) throws CorruptIndexException, IOException {
		
		Definitions.loadDocumentCollecion();
		
		List<SlotFillingQuery> queries = LoadQueries.loadXML(args[0]);
		parseQueries(queries);
	}
	
	public static void parseQueries(List<SlotFillingQuery> queries) throws IOException {
			 
		for (SlotFillingQuery q : queries) {
			
			//fetch doc_id where entity occurs
			q.getSupportDocument();
			
			// get occurrences + context of entity occurrence		
			// query alternative senses for entity name		
			// query document collection
			
						
		}
	}

}
