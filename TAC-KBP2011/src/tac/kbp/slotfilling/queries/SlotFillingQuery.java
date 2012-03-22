package tac.kbp.slotfilling.queries;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import tac.kbp.entitylinking.bin.Definitions;
import tac.kbp.entitylinking.queries.KBPQuery;

public class SlotFillingQuery {
	
	public String query_id;
	public String name;
	public String docid;
	public String etype;
	public String nodeid;
	public String ignore;
	public String supportDocument;

	public SlotFillingQuery() {
		super();
	}
	
	public SlotFillingQuery(String query_id, String name, String docid, String etype, String nodeid) {
		
		super();
		this.query_id = query_id;
		this.name = name;
		this.docid = docid;
		this.etype = etype;
		this.nodeid = nodeid;
	}
	
	public void getSupportDocument() throws IOException {
        Term t = new Term("docid", this.docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.documents.doc(scoredocs[0].doc);        
        this.supportDocument = doc.get("text");
	}

}
