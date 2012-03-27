package tac.kbp.slotfilling.queries.attributes;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import tac.kbp.configuration.Definitions;

public class Attribute {
	
	public String slot_name;
	public String doc_id;
	public String supportDocument;
	public List<String> answer;
	public List<String> normalized_answer;
	public List<String> sentence_with_answers; 
	
	public Attribute() {
		super();
		this.slot_name = null;
		this.doc_id = null;
		this.supportDocument = null;
		this.answer = new LinkedList<String>();
		this.sentence_with_answers = new LinkedList<String>();
		this.normalized_answer = new LinkedList<String>();
	}
	
	public String getAnswerDocument(String docid) throws IOException {
        Term t = new Term("docid", docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.documents.doc(scoredocs[0].doc);        
        return doc.get("text");
	}
}
