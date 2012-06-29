package tac.kbp.slotfilling.relations;

import java.util.LinkedList;

public class DocumentRelations {
	
	public LinkedList<ReverbRelation> relations;
	public String docid;
	
	public DocumentRelations(LinkedList<ReverbRelation> relations, String docid) {
		super();
		this.relations = relations;
		this.docid = docid;
	}
}