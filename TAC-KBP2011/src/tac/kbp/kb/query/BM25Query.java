package tac.kbp.kb.query;

import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
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
		
	public BM25Query(Searcher searcher_, String fields_size, float k1, float b) throws NumberFormatException, IOException, ParseException {
		
		// index searcher
		searcher = searcher_;
		
		//Load fields average length
		BM25Parameters.load(fields_size);
		
		//Set explicit the k1 and B parameter
		BM25FParameters.setK1(k1);
		BM25FParameters.setB(b);
	}
	
	public TopDocs query(String query_string) throws IOException, ParseException {

		//String[] fields = {"wiki_title","type","id","name","infobox_class","wiki_text","facts"};
		String[] fields = {"wiki_text","facts"};
		
		BM25BooleanQuery query = new BM25BooleanQuery(query_string, fields, analyzer);
		
		//Using boost and b defaults parameters				
		return searcher.search(query, null, 100);
		
	}
}