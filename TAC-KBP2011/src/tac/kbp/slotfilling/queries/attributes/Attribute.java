package tac.kbp.slotfilling.queries.attributes;

import java.util.LinkedList;
import java.util.List;

public class Attribute {
	
	public String answer_doc;
	public List<String> answer;
	public List<String> normalized_answer;
	public List<String> sentence_with_answers; 
	
	public Attribute() {
		super();
		this.answer_doc = null;
		this.answer = new LinkedList<String>();
		this.sentence_with_answers = new LinkedList<String>();
		this.normalized_answer = new LinkedList<String>();
	}
}
