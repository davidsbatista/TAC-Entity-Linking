package tac.kbp.slotfilling.queries;

import java.util.List;

public class PER_Attributes implements Attributes {
	
	List<String> per_alternate_names; 				// name-list
	String per_date_of_birth;  						// value-single
	String per_age;  								// value-single
	String per_country_of_birth;					// name-single
	String per_stateorprovince_of_birth; 			// name-single
	String per_city_of_birth; 						// name-single
	List<String> per_origin;						// name-list
	String per_date_of_death;						// value-single
	String per_country_of_death;					// name-single
	String per_stateorprovince_of_death; 			// name-single
	String per_city_of_death;						// name-single
	String per_cause_of_death;						// string-single
	List<String> per_countries_of_residence;		// name-list
	List<String> per_stateorprovinces_of_residence; // name-list
	List<String> per_cities_of_residence;  			// name-list
	List<String> per_schools_attended;  			// name-list
	List<String> per_title;  						// string-list
	List<String> per_member_of;  					// name-list
	List<String> per_employee_of;  					// name-list
	String per_religion;  							// string-single
	List<String> per_spouse;  						// name-list
	List<String> per_children;  					// name-list
	List<String> per_parents; 						// name-list
	List<String> per_siblings; 						// name-list
	List<String> per_other_family; 					// name-list
	List<String> per_charges;						// string-list

	public PER_Attributes(){
		super();
	}
}
