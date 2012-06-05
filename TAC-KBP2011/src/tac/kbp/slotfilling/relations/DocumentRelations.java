package tac.kbp.slotfilling.relations;

import java.util.LinkedList;

public class DocumentRelations {
	
	public DocumentRelations(LinkedList<ReverbRelation> relations, String docid) {
		super();
		this.relations = relations;
		this.docid = docid;
	}
	
	public LinkedList<ReverbRelation> relations;
	public String docid;

}
