package tac.kbp.slotfilling.patterns.slots;

import java.util.HashMap;
import java.util.LinkedList;

public class PERSlots {
	
	public static HashMap<String, LinkedList<String>> slots_patterns = new HashMap<String, LinkedList<String>>();
	
	public static void load_patterns() {
		
		/* per:country_of_birth
		 * per:stateorprovince_of_birth
		 * per:city_of_birth
		 */	
		LinkedList<String> patterns = new LinkedList<String>();
		patterns.add("born in");
		patterns.add("originally from");
		patterns.add("grow up in");
		patterns.add("grew up in");
		PERSlots.slots_patterns.put("per:place_of_birth", patterns);
		
		
		/* per:siblings */
		patterns = new LinkedList<String>();
		patterns.add("brother of");
		patterns.add("sister of");
		PERSlots.slots_patterns.put("per:siblings", patterns);
		
		
		/* per:schools_attended */
		patterns = new LinkedList<String>();		
		patterns.add("graduated from");
		patterns.add("attended");
		patterns.add("studying at");
		patterns.add("studies at");
		patterns.add("studied at");
		PERSlots.slots_patterns.put("per:schools_attended", patterns);
		
		
		/* per:member_of */
		patterns = new LinkedList<String>();		
		patterns.add("elected to");
		patterns.add("member of");
		patterns.add("joined");
		patterns.add("has a contract with");
		patterns.add("contract with");
		patterns.add("had signed with");
		patterns.add("worked for");
		PERSlots.slots_patterns.put("per:member_of", patterns);
		
		
		/* per:marriage */
		patterns = new LinkedList<String>();	
		patterns.add("married to");
		patterns.add("marrying");
		patterns.add("a divorce from");
		slots_patterns.put("per:marriage", patterns);
		
		
		/* per:employ_of */
		patterns = new LinkedList<String>();		
		patterns.add("rehires");
		patterns.add("appointed");
		patterns.add("hire");
		patterns.add("employ");
		patterns.add("employed");
		patterns.add("signed by");
		patterns.add("signed");
		patterns.add("signed with");
		patterns.add("paid by");
		slots_patterns.put("per:employee_of", patterns);
		
		
		/* per:date_of_death */
		patterns = new LinkedList<String>();
		patterns.add("died in");
		patterns.add("killed in");
		patterns.add("was buried in");
		patterns.add("passed away in");		
		slots_patterns.put("per:date_of_death", patterns);
		

		/* per:children */
		patterns = new LinkedList<String>();
		patterns.add("has \\% children");
		patterns.add("have \\% children");
		patterns.add("filled for custody of");
		slots_patterns.put("per:children", patterns);
		
		
		/* per:charges */
		patterns = new LinkedList<String>();
		patterns.add("convicted of");		
		patterns.add("killed");
		patterns.add("shot");
		patterns.add("murdered");
		patterns.add("accused of");
		patterns.add("charged with");
		patterns.add("sentenced for");
		patterns.add("accused of");
		patterns.add("alleges");
		patterns.add("guilty of");
		patterns.add("caught");
		patterns.add("sued for");
		patterns.add("in prison for");
		slots_patterns.put("per:charges", patterns);
		
		
		/* per:cause_of_death */
		patterns = new LinkedList<String>();
		patterns.add("died from");
		patterns.add("died of");
		slots_patterns.put("per:cause_of_death", patterns);

		
		/* 
		 * per:country_of_death
		 * per:stateorprovince_of_death 
		 * per:city_of_death 
		*/
		patterns = new LinkedList<String>();
		patterns.add("died in");		
		patterns.add("killed \\% in");
		patterns.add("killed \\% at");
		patterns.add("fatally \\% shot");
		patterns.add("murdered \\% at");
		patterns.add("murdered \\% in");
		patterns.add("assinated \\% in");
		patterns.add("assinated \\% at");
		patterns.add("died \\% in");
		patterns.add("died \\% at");
		slots_patterns.put("per:place_of_death", patterns);
		
		
		/* per:alternate_names */
		patterns = new LinkedList<String>();
		patterns.add("nicknamed");
		patterns.add("also called");
		slots_patterns.put("per:alternate_names", patterns);
		
		
		/* per:age */
		patterns = new LinkedList<String>();
		patterns.add("turns in");
		slots_patterns.put("per:age", patterns);
		
		
		/* per:date_of_birth */
		patterns = new LinkedList<String>();
		patterns.add("born in");
		slots_patterns.put("per:date_of_birth", patterns);
		
		
		/* per:origin */
		patterns = new LinkedList<String>();
		patterns.add("from");
		patterns.add("have \\% nationality");
		patterns.add("has % nationality");
		slots_patterns.put("per:origin", patterns);
		
		
		/*
		 * per:countries_of_residence
		 * per:stateorprovinces_of_residence
		 * per:cities_of_residence  
		 * */
		patterns = new LinkedList<String>();
		slots_patterns.put("per:place_of_residence", patterns);
				
		/* per:religion */
		patterns = new LinkedList<String>();
		slots_patterns.put("per:religion", patterns);
		
		/* per:title */
		patterns = new LinkedList<String>();
		slots_patterns.put("per:title", patterns);
		
		/* per:alternate_names */
		patterns = new LinkedList<String>();
		slots_patterns.put("per:alternate_names", patterns);
		
		/* per:spouse*/
		patterns = new LinkedList<String>();
		slots_patterns.put("per:spouse", patterns);
		
		/* per:parents*/
		patterns = new LinkedList<String>();
		slots_patterns.put("per:parents", patterns);
		
		/* per:other_family*/
		patterns = new LinkedList<String>();
		slots_patterns.put("per:other_family", patterns);

	}
}
