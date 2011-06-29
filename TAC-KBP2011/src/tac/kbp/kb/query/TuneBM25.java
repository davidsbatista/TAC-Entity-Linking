package tac.kbp.kb.query;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import tac.kbp.kb.query.GoldenStandardQuery;

public class TuneBM25 {
	
	List<GoldenStandardQuery> queries = null;
	BM25Query bm25query = null;
	Searcher searcher = null;
	
	
	public TuneBM25(String index, String fields_size, String queries_file) throws NumberFormatException, IOException, ParseException {
		
		queries = new LinkedList<GoldenStandardQuery>();
		loadQueries(queries_file);
		
		System.out.println( queries.size() + " queries loaded");
		
		searcher = new IndexSearcher(FSDirectory.getDirectory(index));
		bm25query = new BM25Query(searcher, fields_size);
		
		bm25query.setB(0.75f);
		bm25query.setK1(2f);
		
		processQueries();
	}
	
	public void processQueries() throws IOException, ParseException {		
		for (GoldenStandardQuery test_query : this.queries) {
			System.out.println("Correct Answer: "+ test_query.entity_kb_id);
			query(test_query);
		}

	}
	
	public void query(GoldenStandardQuery query) throws IOException, ParseException {
		
		TopDocs top = bm25query.BM25F(query.name);
		ScoreDoc[] docs = top.scoreDocs;
				
		for (int i = 0; i < top.scoreDocs.length; i++) {			
			Document doc = searcher.doc(docs[i].doc);
			String id = doc.getField("id").stringValue();
			String type = doc.getField("type").stringValue();
			String wiki_title = doc.getField("wiki_title").stringValue();
		    
			//System.out.println("Query answer: " + id);
			
			if (id==query.entity_kb_id) {
				System.out.println("id: "+id+"  type: "+type+"  wiki_title: "+wiki_title + "\tscore:"+docs[i].score);
			}

		}
	}

	
	public void loadQueries(String file) {
		
		try {
			
			//Open the file that is the first
			
			System.out.println("opening queries file: " + file);
			
			FileInputStream fstream = new FileInputStream(file);
			
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			String delims = "\\t";
			
			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				String[] tokens = strLine.split(delims);
				
				GoldenStandardQuery query = new GoldenStandardQuery(tokens[0], tokens[1], tokens[2], tokens[3], tokens[4]); 
				this.queries.add(query);				
				
			}
			// Close the input stream
			in.close();
			
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

}