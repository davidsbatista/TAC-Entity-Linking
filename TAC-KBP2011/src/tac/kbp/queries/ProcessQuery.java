package tac.kbp.queries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import org.ninit.models.bm25.BM25BooleanQuery;
import org.ninit.models.bm25.BM25Parameters;
import org.ninit.models.bm25f.BM25FParameters;

import tac.kbp.kb.utils.BigFile;

public class ProcessQuery {

	static String docs = "documents-locations.txt";
	static String fields_size = "fields_avg_size.txt";
	static HashMap<String, String> docslocations = new HashMap<String, String>();
	static IndexSearcher searcher = null; 
	static List<Query> queries = null;
	
	public static void main(String[] args) throws Exception {
		
		/* Load the queries file */
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(args);
		System.err.println(queries.size() + " queries loaded");
		
		/* Load the text file with location of support documents */
		loadDocsLocations();		
		System.out.println(docslocations.size() + " documents locations loaded");
		
		/* Lucene Index */
		searcher = new IndexSearcher(FSDirectory.getDirectory(args[1]));
		
		System.out.println("Lucene index: ");
		System.out.println(searcher.toString());
		
		//Load fields average length
		BM25Parameters.load(fields_size);
		
		//Set explicit the k1 and B parameter
		BM25FParameters.setK1(0.1f);
		BM25FParameters.setB(1.0f);
		
		for (Iterator<Query> iterator = queries.iterator(); iterator.hasNext();) {
			Query query = (Query) iterator.next();
			System.out.println("Processing query: " + query.query_id);
			processQuery(query);
		}
		
		generateOutput(args[2]);
	
	}

	
	private static void processQuery(Query q) throws Exception {
		File supportDoc = getSupportDocument(q);
		Document doc = queryLucene(q);
		
		if (doc!=null) {		
			String id = doc.getField("id").stringValue();
			q.answer_kb_id = id;
		}
		
		else {
			q.answer_kb_id = "NIL";
		}

	}
	
	private static void generateOutput(String output) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (Iterator<Query> iterator = queries.iterator(); iterator.hasNext();) {
			Query q = (Query) iterator.next();
			out.println(q.query_id.trim() +" "+q.answer_kb_id.trim());
		}
		out.close();		
	}
	
	
	
	private static File getSupportDocument(Query q) {
		File doc = new File(docslocations.get(q.docid));
		return doc;
		
	}
	
	
	private static Document queryLucene(Query q) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		Document doc = null;
		
		//String[] fields = {"wiki_title","type","id","name","infobox_class","wiki_text","facts"};
		String[] fields = {"name","wiki_text","facts"};
		float[] boosts =  {1f,1f,1f};
		float[] bParams =  {1.0f,1.0f,1.0f};
		
		BM25BooleanQuery query = new BM25BooleanQuery(q.name, fields, analyzer, boosts, bParams);
		
		//Using boost and b defaults parameters	
		TopDocs top = searcher.search(query, null, 100);				
		ScoreDoc[] docs = top.scoreDocs;
		
		if (top.scoreDocs.length>0) {
			doc = searcher.doc(docs[0].doc);
		}

		return doc;
		
		/*
		for (int i = 0; i < top.scoreDocs.length; i++) {
			Document doc = searcher.doc(docs[i].doc);
			String id = doc.getField("id").stringValue();
			String wiki_title = doc.getField("wiki_title").stringValue();
			String type = doc.getField("type").stringValue();
			System.out.println(id+": "+wiki_title+" "+type);
		}
		*/
	}
	
	
	
	private static void loadDocsLocations() throws Exception {
		
		BigFile file = new BigFile(docs);
		String[] parts;
		
		for (String line : file) {		
			parts = line.split(".sgm");			
			docslocations.put(parts[0], parts[1]);
		}
		
	}
}
