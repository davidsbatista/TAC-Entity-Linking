package tac.kbp.kb.query;

import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.ninit.models.bm25.BM25BooleanQuery;
import org.ninit.models.bm25.BM25Parameters;
import org.ninit.models.bm25f.BM25FParameters;

public class BM25Query{
	
	/* this uses the BM25 implementation for Lucene from http://nlp.uned.es/~jperezi/Lucene-BM25/
	 * 
	 * K1 is a free parameter usually 2 and B in [0,1] (usually 0.75). 
	 * 
	*/
	
	//same analyzer used for indexing
	WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
	
	// index
	Searcher searcher = null;
	
	//parameters
	float K1;
	float B;
	
		
	public BM25Query(Searcher searcher_, String fields_size) throws NumberFormatException, IOException, ParseException {
					
		//Load fields average length
		BM25Parameters.load(fields_size);
		
		// index
		searcher = searcher_;
		
		//Set explicit k1 parameter
	}
	
	
	public ScoreDoc[] BM25(String query_string) throws IOException, ParseException {

		BM25BooleanQuery query = new BM25BooleanQuery(query_string, "wiki_text", analyzer);
		
		TopDocs top = searcher.search(query, null, 10);
		ScoreDoc[] docs = top.scoreDocs;
		
		return docs;
		
		/*
		//Print results
		System.out.println("Results(BM25): ");
		for (int i = 0; i < top.scoreDocs.length; i++) {
			
			Document doc = searcher.doc(docs[i].doc);
		    String id = doc.getField("id").stringValue();
		    String type = doc.getField("type").stringValue();
		    String wiki_title = doc.getField("wiki_title").stringValue();
		    
		    System.out.println("id: "+id+"  type: "+type+"  wiki_title: "+wiki_title + "\tscore:"+docs[i].score);
		}
		*/
		

	}
	
	public TopDocs BM25F(String query) throws IOException, ParseException {
		
		//String[] fields = {"wiki_title","type","id","name","infobox_class","wiki_text","facts"};
		String[] fields = {"wiki_text","facts"};
		
		//Using boost and b defaults parameters
		BM25BooleanQuery queryF = new BM25BooleanQuery(query, fields, analyzer);
		
		//Retrieving NOT normalized scorer values
		return searcher.search(queryF, null, 100);
		
	}

	public float getK1() {
		return K1;
	}

	public void setK1(float k1) {
		BM25FParameters.setK1(k1);
	}

	public float getB() {
		return B;
	}

	public void setB(float d) {
		BM25FParameters.setB(d);
	}

}