package tac.kbp.slotfilling.queries;

import java.util.List;

public class ORG_Attributes implements Attributes {
	
	List<String> org_alternate_names;					// name-list							
	List<String> org_political_religious_affiliation;	// name-list
	List<String> org_top_members_employees;				// name-list
	String org_number_of_employees_members;				// value-single
	List<String> org_members;							// name-list
	List<String> org_member_of;							// name-list
	List<String> org_subsidiaries;						// name-list
	List<String> org_parents;							// name-list
	List<String> org_founded_by;						// name-list
	String org_founded;									// value-single
	String org_dissolved;								// value-single
	String org_country_of_headquarters;					// name-single
	String org_stateorprovince_of_headquarters;			// name-single
	String org_city_of_headquarters;					// name-single
	List<String> org_shareholders;						// name-list
	String org_website;									// string-single

	public ORG_Attributes(){
		super();
	}
}
