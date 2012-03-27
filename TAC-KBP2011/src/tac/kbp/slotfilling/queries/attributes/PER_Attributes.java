package tac.kbp.slotfilling.queries.attributes;

import java.util.HashMap;

public class PER_Attributes extends Attributes {
	
	public HashMap<String, Attribute> attributes;
	
	public PER_Attributes(){
		super();
		this.attributes = new HashMap<String, Attribute>();
		this.attributes.put("per:alternate_names", new Attribute());		
		this.attributes.put("per:date_of_birth", new Attribute());		
		this.attributes.put("per:age", new Attribute());		
		this.attributes.put("per:country_of_birth", new Attribute());		
		this.attributes.put("per:stateorprovince_of_birth", new Attribute());		
		this.attributes.put("per:city_of_birth", new Attribute());
		this.attributes.put("per:origin", new Attribute());		
		this.attributes.put("per:date_of_death", new Attribute());		
		this.attributes.put("per:country_of_death", new Attribute());		
		this.attributes.put("per:stateorprovince_of_death", new Attribute());		
		this.attributes.put("per:city_of_death", new Attribute());
		this.attributes.put("per:cause_of_death", new Attribute());		
		this.attributes.put("per:countries_of_residence", new Attribute());
		this.attributes.put("per:stateorprovinces_of_residence", new Attribute());
		this.attributes.put("per:cities_of_residence", new Attribute());
		this.attributes.put("per:schools_attended", new Attribute());		
		this.attributes.put("per:title", new Attribute());		
		this.attributes.put("per:member_of", new Attribute());		
		this.attributes.put("per:employee_of", new Attribute());		
		this.attributes.put("per:religion", new Attribute());		
		this.attributes.put("per:spouse", new Attribute());		
		this.attributes.put("per:children", new Attribute());		
		this.attributes.put("per:parents", new Attribute());		
		this.attributes.put("per:siblings", new Attribute());		
		this.attributes.put("per:other_family", new Attribute());
		this.attributes.put("per:charges", new Attribute());
	}		
}
