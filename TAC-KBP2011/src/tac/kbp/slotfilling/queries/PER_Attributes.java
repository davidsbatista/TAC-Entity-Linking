package tac.kbp.slotfilling.queries;

import java.util.HashMap;

public class PER_Attributes extends Attributes {
	
	public HashMap<String, Attribute> attributes;
	
	public PER_Attributes(){
		super();
		this.attributes = new HashMap<String, Attribute>();
		this.attributes.put("per_alternate_names", new Attribute());		
		this.attributes.put("per_date_of_birth", new Attribute());		
		this.attributes.put("per_age", new Attribute());		
		this.attributes.put("per_country_of_birth", new Attribute());		
		this.attributes.put("per_stateorprovince_of_birth", new Attribute());		
		this.attributes.put("per_city_of_birth", new Attribute());
		this.attributes.put("per_origin", new Attribute());		
		this.attributes.put("per_date_of_death", new Attribute());		
		this.attributes.put("per_country_of_death", new Attribute());		
		this.attributes.put("per_stateorprovince_of_death", new Attribute());		
		this.attributes.put("per_city_of_death", new Attribute());
		this.attributes.put("per_cause_of_death", new Attribute());		
		this.attributes.put("per_countries_of_residence", new Attribute());
		this.attributes.put("per_stateorprovinces_of_residence", new Attribute());
		this.attributes.put("per_cities_of_residence", new Attribute());
		this.attributes.put("per_schools_attended", new Attribute());		
		this.attributes.put("per_title", new Attribute());		
		this.attributes.put("per_member_of", new Attribute());		
		this.attributes.put("per_employee_of", new Attribute());		
		this.attributes.put("per_religion", new Attribute());		
		this.attributes.put("per_spouse", new Attribute());		
		this.attributes.put("per_children", new Attribute());		
		this.attributes.put("per_parents", new Attribute());		
		this.attributes.put("per_siblings", new Attribute());		
		this.attributes.put("per_other_family", new Attribute());
		this.attributes.put("per_charges", new Attribute());
	}		
}
