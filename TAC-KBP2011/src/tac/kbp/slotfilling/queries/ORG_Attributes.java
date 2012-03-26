package tac.kbp.slotfilling.queries;

import java.util.HashMap;

public class ORG_Attributes extends Attributes {
	
	public HashMap<String, Attribute> attributes;
	
	public ORG_Attributes(){
		super();
		this.attributes = new HashMap<String, Attribute>();
		this.attributes.put("org_alternate_names", new Attribute());
		this.attributes.put("org_political_religious_affiliation", new Attribute());
		this.attributes.put("org_top_members_employees", new Attribute());
		this.attributes.put("org_number_of_employees_members;", new Attribute());		
		this.attributes.put("org_members", new Attribute());
		this.attributes.put("org_member_of", new Attribute());
		this.attributes.put("org_subsidiaries", new Attribute());
		this.attributes.put("org_parents", new Attribute());
		this.attributes.put("org_founded_by", new Attribute());
		this.attributes.put("org_founded", new Attribute());		
		this.attributes.put("org_dissolved", new Attribute());
		this.attributes.put("org_country_of_headquarters", new Attribute());
		this.attributes.put("org_stateorprovince_of_headquarters", new Attribute());
		this.attributes.put("org_city_of_headquarters", new Attribute());
		this.attributes.put("org_shareholders", new Attribute());
		this.attributes.put("org_website", new Attribute());
	}
}
