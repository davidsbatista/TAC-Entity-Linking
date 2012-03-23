package tac.kbp.slotfilling.queries;

import java.util.List;

public class PER_Attributes implements Attributes {
	
	public List<String> per_alternate_names; 				// name-list
	public String per_date_of_birth;  						// value-single
	public String per_age;  								// value-single
	public String per_country_of_birth;					// name-single
	public String per_stateorprovince_of_birth; 			// name-single
	public String per_city_of_birth; 						// name-single
	public List<String> per_origin;						// name-list
	public String per_date_of_death;						// value-single
	public String per_country_of_death;					// name-single
	public String per_stateorprovince_of_death; 			// name-single
	public String per_city_of_death;						// name-single
	public String per_cause_of_death;						// string-single
	public List<String> per_countries_of_residence;		// name-list
	public List<String> per_stateorprovinces_of_residence; // name-list
	public List<String> per_cities_of_residence;  			// name-list
	public List<String> per_schools_attended;  			// name-list
	public List<String> per_title;  						// string-list
	public List<String> per_member_of;  					// name-list
	public List<String> per_employee_of;  					// name-list
	public String per_religion;  							// string-single
	public List<String> per_spouse;  						// name-list
	public List<String> per_children;  					// name-list
	public List<String> per_parents; 						// name-list
	public List<String> per_siblings; 						// name-list
	public List<String> per_other_family; 					// name-list
	public List<String> per_charges;						// string-list

	public PER_Attributes(){
		super();
	}
}
