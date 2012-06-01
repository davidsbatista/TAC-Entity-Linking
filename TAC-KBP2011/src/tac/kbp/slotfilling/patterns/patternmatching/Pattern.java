package tac.kbp.slotfilling.patterns.patternmatching;

public class Pattern {
	
	public String slot_name;
	public String query_name;
	public String response_value;
	public String sentence;
	
	public Pattern(String slot_name, String query_name, String response_value, String sentence) {
		
		super();
		this.slot_name = slot_name;
		this.query_name = query_name;
		this.response_value = response_value;
		this.sentence = sentence;
	}
	
}
