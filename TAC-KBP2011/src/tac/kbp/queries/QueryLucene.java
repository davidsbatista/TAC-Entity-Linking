package tac.kbp.queries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.ninit.models.bm25.BM25BooleanQuery;
import org.ninit.models.bm25.BM25Parameters;
import org.ninit.models.bm25f.BM25FParameters;


public class QueryLucene {
	
	static IndexSearcher searcher = null;
	static List<DocumentScore> documents = null;
	
	static String fields_size = "fields_avg_size.txt";
	static String index = "index/";
	
	/*
	 * args[0] - output dir of documents from Lucence
	 * 
	 * String[] args[1...n] - list of strings representing the query
	 */
	
	public static void main(String[] args) throws Exception {
		
		/* Lucene Index */
		searcher = new IndexSearcher(FSDirectory.getDirectory(index));
		
		//Load fields average length
		BM25Parameters.load(fields_size);
		
		//Set explicit the k1 and B parameter
		BM25FParameters.setK1(0.1f);
		BM25FParameters.setB(1.0f);
		
		documents = new LinkedList<DocumentScore>();
		
		StringBuffer query_string = new StringBuffer();
		
		for (int i = 1; i < args.length; i++) {
			query_string.append(" "+args[i]); 
		}
		
		queryLucene(query_string.toString());
		System.out.println( documents.size() + " documents found");
		dumpDocs(args[0]);
	}
	
	public static void dumpFile(String outdir, String filename, String contents) {
		
		try {
			//Create dir
			new File(outdir).mkdir();
			//Create file 
			FileWriter fstream = new FileWriter(outdir+"/lucene_"+filename+".txt");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(contents);
			//Close the output stream
			out.close();
		} 
		
		catch (Exception e)
		{
			//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public static void dumpDocs(String outdir) {
		
		for (Iterator<DocumentScore> iterator = documents.iterator(); iterator.hasNext();) {
			DocumentScore doc = (DocumentScore) iterator.next();
			
			String id = doc.luceneDoc.getField("id").stringValue();
			String wiki_title = doc.luceneDoc.getField("wiki_title").stringValue();
			String type = doc.luceneDoc.getField("type").stringValue();
			String name = doc.luceneDoc.getField("name").stringValue();
			String infobox = doc.luceneDoc.getField("infobox").stringValue();
			String facts = doc.luceneDoc.getField("facts").stringValue();
			String wiki_text = doc.luceneDoc.getField("wiki_text").stringValue();
			
			StringBuffer contents = new StringBuffer();
			
			contents.append(doc.score+"\n");
			contents.append(wiki_title+"\n");
			contents.append(type+"\n");
			contents.append(name+"\n");
			contents.append(infobox+"\n");
			contents.append(facts+"\n");
			contents.append(wiki_text+"\n");
			
			dumpFile(outdir,id,contents.toString());
		}
		
	}
	
	private static void queryLucene(String query_string) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		
		String[] fields = {"name","wiki_text","facts"};
		float[] boosts =  {1f,1f,1f};
		float[] bParams =  {1.0f,1.0f,1.0f};
		
		BM25BooleanQuery query = new BM25BooleanQuery(query_string, fields, analyzer, boosts, bParams);
		
		//Using boost and b defaults parameters	
		TopDocs top = searcher.search(query, null, 100);				
		ScoreDoc[] docs = top.scoreDocs;
		
		/*
		only outputs the document with the highest score
		DocumentScore doc = new DocumentScore();
		doc.score = docs[0].score;
		doc.luceneDoc = searcher.doc(docs[0].doc);
		documents.add(doc);
		*/
		
		for (int i = 0; i < docs.length; i++) {
			DocumentScore doc = new DocumentScore();
			doc.score = docs[i].score;
			doc.luceneDoc = searcher.doc(docs[i].doc);
			documents.add(doc);
		}
	}
}




