package tac.kbp.kb;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.ninit.models.bm25.BM25BooleanQuery;
import org.ninit.models.bm25.BM25Parameters;
import org.ninit.models.bm25f.BM25FParameters;


public class QueryTest{
	
	
	
	public void BM25() {
	
		Directory dir = new SimpleFSDirectory(new File("/tmp/KB"));
		
		IndexSearcher searcher = new IndexSearcher(dir,true);

		//Load average length
		BM25Parameters.load(avgLengthPath);
		BM25BooleanQuery query = new BM25BooleanQuery("This is my Query", "Search-Field", new StandardAnalyzer());
		
		TopDocs top = searcher.search(query, null, 10);
		ScoreDoc[] docs = top.scoreDocs;
		
		//Print results
		for (int i = 0; i < top.scoreDocs.length; i++) {
		      System.out.println(docs[i].doc + ":"+docs[i].score);
		}
	}
	
	public void BM25F() throws IOException, ParseException {

		Directory dir = new SimpleFSDirectory(new File("/tmp/KB"));
		
		String[] fields ={"FIELD1","FIELD2"};
		IndexSearcher searcher = new IndexSearcher(dir,true);
	
		//Set explicit average Length for each field
		BM25FParameters.setAverageLength("FIELD1", 123.5f);
		BM25FParameters.setAverageLength("FIELD2", 42.2f);
		
		//Set explicit k1 parameter
		BM25FParameters.setK1(1.2f);
		
		//Using boost and b defaults parameters
		BM25BooleanQuery queryF = new BM25BooleanQuery("This is my query", fields, new StandardAnalyzer());
		
		//Retrieving NOT normalized scorer values
		TopDocs top = searcher.search(queryF, null, 10);
		ScoreDoc[] docs = top.scoreDocs;
		
		//Print results
		for (int i = 0; i < top.scoreDocs.length; i++) {
		      System.out.println(docs[i].doc + ":"+docs[i].score);
		}
		
	}
}
	