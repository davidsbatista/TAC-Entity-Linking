package tac.kbp.queries;

import java.util.HashSet;
import java.util.List;

public class KBPQuery {
	
	public String query_id;
	public String name;
	public String docid;
	public String answer_kb_id;
	public HashSet<String> alternative_names;
	public HashSet<String> answers;

	public KBPQuery(String query_id, String name, String docid) {
		super();
		this.query_id = query_id;
		this.name = name;
		this.docid = docid;
		this.answer_kb_id = null;
		this.alternative_names = new HashSet<String>();
		this.answers = new HashSet<String>();  
	}
}