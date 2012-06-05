package tac.kbp.slotfilling.relations;

public class ReverbRelation {
	
	public String docid;
	public String arg1;
	public String rel;
	public String arg2;
	public String sentence;
	public Float confidence;
	
	public ReverbRelation(String docid, String arg1, String rel, String arg2, String sentence, Float confidence) {
		super();
		this.docid = docid;
		this.arg1 = arg1;
		this.rel = rel;
		this.arg2 = arg2;
		this.sentence = sentence;
		this.confidence = confidence;
	}
}
