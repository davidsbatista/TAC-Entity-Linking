package tac.kbp.kb.query;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class BM25TuneResults {
	
	HashMap<String, Float> results = null;
	
	public BM25TuneResults() {
		results = new HashMap<String, Float>();
	}
	
	
	public void setScore(String query_id, Float score) {
		results.put(query_id, score);
	}
	

	public String getResults(){
		Set<String> keys = results.keySet();
		
		StringBuffer output = new StringBuffer();
		
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String query_id = iterator.next();
					
			output.append(query_id + "\t" + results.get(query_id)+"\n"); 
			
		}
		return output.toString();
	}
}
