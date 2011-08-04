package tac.kbp.queries;

public class Query {
	
	String query_id;
	String name;
	String docid;
	String answer_kb_id;
	
	public Query(String query_id, String name, String docid) {
		super();
		this.query_id = query_id;
		this.name = name;
		this.docid = docid;
		this.answer_kb_id = null;
	}
	
}