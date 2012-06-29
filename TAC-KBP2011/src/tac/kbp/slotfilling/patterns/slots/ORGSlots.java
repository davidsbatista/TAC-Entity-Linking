package tac.kbp.slotfilling.patterns.slots;

import java.util.HashMap;
import java.util.LinkedList;

public class ORGSlots {
	
	public static HashMap<String, LinkedList<String>> slots_patterns = new HashMap<String, LinkedList<String>>();
	
	public static void load_patterns() {
		
		/* org:alternate_names*/		
		LinkedList<String> patterns = new LinkedList<String>();
		ORGSlots.slots_patterns.put("org:alternate_names", patterns);
		
		
		/* org:political_religious_affiliation */
		patterns = new LinkedList<String>();
		ORGSlots.slots_patterns.put("org:political_religious_affiliation", patterns);
		
		
		/* org:top_members_employees */
		patterns = new LinkedList<String>();
		patterns.add("heads the");
		patterns.add("secretary general");
		patterns.add("vice president of");
		patterns.add("leader of");
		patterns.add("director of");
		patterns.add("general of");
		patterns.add("chairman of");
		patterns.add("chief of");
		patterns.add("chief executive of");
		patterns.add("archbishop of");
		patterns.add("president of");
		patterns.add("manager of");
		patterns.add("chief commissioner of");
		patterns.add("head of");
		patterns.add("president for");
		patterns.add("managing director");
		patterns.add("chief executive");
		ORGSlots.slots_patterns.put("org:top_members_employees", patterns);
		
		
		/* org:number_of_employees_members */
		patterns = new LinkedList<String>();
		patterns.add("employs");
		ORGSlots.slots_patterns.put("org:number_of_employees_members", patterns);
		
		
		/* org:members */
		patterns = new LinkedList<String>();		
		patterns.add("the parent of");
		patterns.add("owns");
		patterns.add("acquired by");
		ORGSlots.slots_patterns.put("org:members", patterns);
		
		
		/* org:member_of */
		patterns = new LinkedList<String>();						
		ORGSlots.slots_patterns.put("org:member_of", patterns);
		
		
		/* org:subsidiaries */
		patterns = new LinkedList<String>();
		ORGSlots.slots_patterns.put("org:subsidiaries", patterns);
		

		/* org:parents */
		patterns = new LinkedList<String>();		
		ORGSlots.slots_patterns.put("org:parents", patterns);
		
		
		/* org:founded_by */
		patterns = new LinkedList<String>();		
		ORGSlots.slots_patterns.put("org:founded_by", patterns);
		
		
		/* org:founded */
		patterns = new LinkedList<String>();
		ORGSlots.slots_patterns.put("org:founded", patterns);

		
		/* org:dissolved */ 
		patterns = new LinkedList<String>();		
		ORGSlots.slots_patterns.put("org:dissolved", patterns);

		
		/* org:country_of_headquarters */
		patterns = new LinkedList<String>();
		ORGSlots.slots_patterns.put("org:country_of_headquarters", patterns);
		
		
		/* org:city_of_headquarters */
		patterns = new LinkedList<String>();		
		ORGSlots.slots_patterns.put("org:city_of_headquarters", patterns);
		
		
		/* org:shareholders */
		patterns = new LinkedList<String>();		
		ORGSlots.slots_patterns.put("org:shareholders", patterns);
				
		/* org:website */
		patterns = new LinkedList<String>();
		ORGSlots.slots_patterns.put("org:website", patterns);
	}
}