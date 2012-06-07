package tac.kbp.slotfilling.relations;

import tac.kbp.slotfilling.configuration.Definitions;

public class ReverbRelation {
	
	public String docid;
	public String arg1;
	public String arg1_tagged;
	public String rel;
	public String arg2;
	public String arg2_tagged;
	public String sentence;
	public String sentence_tagged;
	public Float confidence;
	
	public ReverbRelation(String docid, String arg1, String rel, String arg2, String sentence, Float confidence) {
		super();
		this.docid = docid;
		this.arg1 = arg1;
		this.rel = rel;
		this.arg2 = arg2;
		this.sentence = sentence;
		this.sentence_tagged = null;
		this.confidence = confidence;
	}
	
	public void tag() {
		
		sentence_tagged = Definitions.classifier.classifyWithInlineXML(sentence);
		arg1_tagged = Definitions.classifier.classifyWithInlineXML(arg1);
		arg2_tagged = Definitions.classifier.classifyWithInlineXML(arg2);		
	}
}