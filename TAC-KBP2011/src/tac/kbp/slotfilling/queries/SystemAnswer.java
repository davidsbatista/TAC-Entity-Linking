package tac.kbp.slotfilling.queries;

import tac.kbp.slotfilling.relations.ReverbRelation;

public class SystemAnswer {
	
	public String slot_name;
	public String slot_filler;
	public String justify;											
	public int start_offset_filler;
	public int end_offset_filler;
	public int start_offset_justification;
	public int end_offset_justification;
	public Float confidence_score;	
	
	public ReverbRelation relation;
}
