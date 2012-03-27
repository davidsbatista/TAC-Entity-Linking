package tac.kbp.slotfilling.patternmatching;

import java.util.List;
import java.util.Set;

import tac.kbp.slotfilling.queries.SFQuery;
import tac.kbp.slotfilling.queries.attributes.PER_Attributes;


public class PatternMatching {	
		
	public static void qaPairs(List<SFQuery> trainQueries) {
		//TODO: collect query-answers pairs from the training queries
		
		for (SFQuery q : trainQueries) {
			if (q.etype.equalsIgnoreCase("PER")) {
				Set<String> keys = ((PER_Attributes) q.attributes).attributes.keySet();
				
				for (String k : keys) {
					((PER_Attributes) q.attributes).attributes.get(k);
				}
				
				
			}
				
		}
	}
	
	public static void extractPatterns() {
		//TODO: retrieve from the document collection sentences where query and answer co-occur
	}

}
