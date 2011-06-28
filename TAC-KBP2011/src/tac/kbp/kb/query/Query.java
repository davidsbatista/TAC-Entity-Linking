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

public class Query{

	//index location
	Searcher searcher = null;
	
	//same analyzer used for indexing
	WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		
	public Query(String index, String fields_size) throws NumberFormatException, IOException, ParseException {
		
		searcher = new IndexSearcher(FSDirectory.getDirectory(index));
					
		//Load fields average length
		BM25Parameters.load(fields_size);
			
	}
	
	
	public void BM25(String query_string) throws IOException, ParseException {

		BM25BooleanQuery query = new BM25BooleanQuery(query_string, "wiki_text", analyzer);
		
		TopDocs top = searcher.search(query, null, 10);
		ScoreDoc[] docs = top.scoreDocs;
		
		//Print results
		System.out.println("Results(BM25): ");
		for (int i = 0; i < top.scoreDocs.length; i++) {
		      System.out.println(docs[i].doc + ":"+docs[i].score);
		}

	}
	
	public void BM25F(String query) throws IOException, ParseException {
		
		String[] fields ={"wiki_title","type","id","name","infobox_class","wiki_text","facts"};
		
		//Set explicit k1 parameter
		BM25FParameters.setK1(1.2f);
		
		//Using boost and b defaults parameters
		BM25BooleanQuery queryF = new BM25BooleanQuery(query, fields, analyzer);
		
		//Retrieving NOT normalized scorer values
		TopDocs top = searcher.search(queryF, null, 10);
		ScoreDoc[] docs = top.scoreDocs;
		
		//Print results
		System.out.println("Results(BM25F): ");
		for (int i = 0; i < top.scoreDocs.length; i++) {
		      System.out.println(docs[i].doc + ":"+docs[i].score);
		}
		
	}
}