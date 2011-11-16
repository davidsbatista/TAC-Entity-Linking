package tac.kbp.queries;

public class QueryGoldStandard {
	
	String 	query_id;
	String	answer;
	String  type;
	String  web_search; //indicates if the annotator made use of web searches to make the linking judgment
	String  genre; 		//source genre of the document for the query (WL for web data, or NW for newswire data)
	
	public QueryGoldStandard(String query_id, String answer, String type, String web_search, String genre) {
		super();
		this.query_id = query_id;
		this.answer = answer;
		this.type = type;
		this.web_search = web_search;
		this.genre = genre;
	}
	
	@Override
	public String toString() {
		return "QueryGoldStandard [query_id=" + query_id + ", answer=" + answer
				+ ", type=" + type + ", web_search=" + web_search + ", genre="
				+ genre + "]";
	}

}
