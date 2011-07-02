package tac.kbp.kb.query;

import java.io.IOException;
import java.util.Date;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

/**
 * @author dsbatista
 *
 */

public class Main {
	
	public static void main(String[] args) throws IOException, ParseException{
		
		long start = new Date().getTime();
		
		IndexSearcher searcher = new IndexSearcher(FSDirectory.getDirectory(args[0]));
		
		// to find the best k1 and B parameters, batch queries processing
		TuneBM25 tune = new TuneBM25(searcher, args[1], args[2]);
		tune.processBatch(args[1]);
		tune.findBestParameters();
		
		// test with a query from command line
		
		/*
		BM25Query query = new BM25Query(searcher, args[1], 0.75f, 2f);
		TopDocs top = query.query(args[2]);
				
		System.out.println("Total hits: " + top.totalHits);
		System.out.println("Top : " + top.scoreDocs.length + "\n");
		
		ScoreDoc[] docs = top.scoreDocs;
		
		for (int i = 0; i < top.scoreDocs.length; i++) {			
			
			Document doc = searcher.doc(docs[i].doc);
			//String id = doc.getField("id").stringValue();
			System.out.println(docs[i].doc + ":"+docs[i].score);
		}
		*/
		
		long end = new Date().getTime();	
		float secs = (end - start) / 1000;	
		
		System.out.println("\nProcessing time " + Float.toString(secs) + " secs");
		
	}		
}

