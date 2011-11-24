package tac.kbp.queries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class KBPQuery {
	
	public String query_id;
	public String name;
	public String docid;
	public String answer_kb_id;
	
	public HashSet<String> alternative_names;
	public HashSet<String> candidates;
	
	public HashSet<String> persons;
	public HashSet<String> places;
	public HashSet<String> organizations;
	
	public HashMap<String,String> query;

	public KBPQuery(String query_id, String name, String docid) {
		super();
		this.query_id = query_id;
		this.name = name;
		this.docid = docid;
		this.answer_kb_id = null;
		this.alternative_names = new HashSet<String>();
		this.candidates = new HashSet<String>();  
		
		this.persons = new HashSet<String>();
		this.places = new HashSet<String>();
		this.organizations = new HashSet<String>();
		
		this.query = new HashMap<String,String>();
	}
}