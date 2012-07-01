package tac.kbp.slotfilling.queries.attributes;

import java.util.HashMap;

public class ORG_Attributes extends Attributes {
	
	public HashMap<String, Attribute> attributes;
	
	public ORG_Attributes(){
		super();
		this.attributes = new HashMap<String, Attribute>();
		this.attributes.put("org:alternate_names", new Attribute());
		this.attributes.put("org:political_religious_affiliation", new Attribute());
		this.attributes.put("org:top_members/employees", new Attribute());
		this.attributes.put("org:number_of_employees/members", new Attribute());		
		this.attributes.put("org:members", new Attribute());
		this.attributes.put("org:member_of", new Attribute());
		this.attributes.put("org:subsidiaries", new Attribute());
		this.attributes.put("org:parents", new Attribute());
		this.attributes.put("org:founded_by", new Attribute());
		this.attributes.put("org:dissolved", new Attribute());
		this.attributes.put("org:country_of_headquarters", new Attribute());
		this.attributes.put("org:stateorprovince_of_headquarters", new Attribute());
		this.attributes.put("org:city_of_headquarters", new Attribute());
		this.attributes.put("org:shareholders", new Attribute());
		this.attributes.put("org:website", new Attribute());
		this.attributes.put("org:founded", new Attribute());
	}
}
