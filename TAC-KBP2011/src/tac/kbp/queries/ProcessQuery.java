package tac.kbp.queries;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import redis.clients.jedis.BinaryJedis;
import tac.kbp.utils.BigFile;

import com.google.common.base.Joiner;

public class ProcessQuery {
	
	static HashMap<String, String> docslocations = new HashMap<String, String>();
	static IndexSearcher searcher = null; 
	
	static List<KBPQuery> queries = null;
	static HashMap<String, GoldStandardQuery> queriesGold = new HashMap<String, GoldStandardQuery>();
	
	static String named_entities_supportDoc = "/collections/TAC-2011/named-entities";
	
	static int total_n_docs = 0;
	static int n_found = 0;
	static int n_queries_zero_docs = 0;
	
	public static void main(String[] args) throws Exception {
		
		/* Load the queries file */
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(args[0]);
		System.err.println(queries.size() + " queries loaded");
		
		/* Load the text file with location of support documents */
		//loadDocsLocations(args[1]);		
		//System.out.println(docslocations.size() + " documents locations loaded");
		
		/* Lucene Index */
		searcher = new IndexSearcher(FSDirectory.getDirectory(args[2]));
		
		/*
		//Load fields average length
		BM25Parameters.load(fields_size);
		
		//Set explicit the k1 and B parameter
		BM25FParameters.setK1(0.1f);
		BM25FParameters.setB(1.0f);
		*/
		
		int port = 6379;
		String host = "agatha";
		BinaryJedis binaryjedis = new BinaryJedis(host, port);
		
		for (Iterator<KBPQuery> iterator = queries.iterator(); iterator.hasNext();) {
			KBPQuery query = (KBPQuery) iterator.next();
			
			System.out.print(query.query_id + " \"" + query.name + '"');
			getSenses(binaryjedis, query);
			loadGoldStandard(args[3]);
			processQuery(query);		
		}
		
		System.out.println("Documents Retrieved: " + Integer.toString(total_n_docs));
		System.out.println("Queries: " + Integer.toString(queries.size()));
		System.out.println("Docs p/ query: " + ( (float) total_n_docs / (float) queries.size()));
		System.out.println("Queries 0 docs returned: " + Integer.toString(n_queries_zero_docs));
		System.out.println("Found: " + n_found);

	}

	private static void loadNamedEntities(KBPQuery q) throws IOException{
		BufferedReader input;
		
		try {
	
			input = new BufferedReader(new FileReader(named_entities_supportDoc+"/"+q.query_id+"-named-entities.txt"));
			String line = null;
			boolean persons = false;
			boolean org = false;
			boolean place = false;
	        
			while (( line = input.readLine()) != null){
				if (line.equalsIgnoreCase("")) {
					continue;
				}
				
				if (line.equalsIgnoreCase("PERSONS:")) {
					persons = true;
					org = false;
					place = false;
					continue;
				}
				
				if (line.equalsIgnoreCase("PLACES:")) {
					org = true;
					persons = false;
					place = false;
					continue;
				}
				
				if (line.equalsIgnoreCase("ORGANIZATIONS:")) {
					place = true;
					persons = false;
					org = false;
					continue;
				}
				
				if (place)
					q.places.add( '"' + line.trim() + '"');
				
				if (org)
					q.organizations.add('"' + line.trim() + '"');
				
				if (persons)
					q.persons.add('"' + line.trim() + '"');

	        }
	        
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();    	
		}
	}

	private static String cleanString(String sense) {
		
		/*
		'Du Wei'
		'Du Wei(footballer)']
		[u'Du_wei'
		*/
		
		String cleaned =  sense.replace("[u'","").replace("']", "").replace("u'", "").replace("[","").
				replace("'","").replace("['", "").trim().replace("_", " ");
		
		return cleaned;
	}

	
	private static void getSenses(BinaryJedis binaryjedis, KBPQuery query) {
	
	try {

		byte[] queryStringbyteArray = query.name.getBytes("UTF-8");
		byte[] queryStringLowbyteArray = query.name.toLowerCase().getBytes("UTF-8");
		
		byte[] acronyms = binaryjedis.get(queryStringLowbyteArray);
		byte[] senses = binaryjedis.get(queryStringbyteArray);
		
		if (acronyms != null) {						
			String acr = new String(acronyms, "UTF8");
			String[] acronymsArray = acr.split(",\\s");
			
			for (int i = 0; i < acronymsArray.length; i++) {
				
				String cleaned = cleanString(acronymsArray[i]);
				
				if (cleaned.compareToIgnoreCase(query.name) != 0) {
					query.alternative_names.add(cleaned);
				}
										
			}
		}
		
		if (senses != null) {
			String ses = new String(senses, "UTF8");
			String[] sensesArray = ses.split(",\\s");
			
			for (int i = 0; i < sensesArray.length; i++) {
				
				String cleaned = cleanString(sensesArray[i]);
				
				if (cleaned.compareToIgnoreCase(query.name) != 0) {
					query.alternative_names.add(cleaned);
				}		
			}
		}
		
	}
	
	catch (Exception e) {
			// Catch exception if any
			System.out.println(e);
			System.err.println("Error: " + e.getMessage());
		}
	}

	private static void processQuery(KBPQuery q) throws Exception {
		
		//String supportDoc = getSupportDocument(q);
		
		//load the recognized named-entities in the support document
		loadNamedEntities(q);
		
		ScoreDoc[] docs = queryKB(q);
		
		/*
		ScoreDoc[] docsTitles = docs.get("titles");
		ScoreDoc[] docsWikiText = docs.get("wikitext");
		*/
				
		total_n_docs += docs.length;
		
		if (docs.length == 0)
			n_queries_zero_docs++;
		
		System.out.print("\t correct answer: "+ queriesGold.get(q.query_id).answer);
		
		findCorrectEntity(q, docs);
	}

	private static void findCorrectEntity(KBPQuery q, ScoreDoc[] docs) throws CorruptIndexException, IOException {
				
		GoldStandardQuery q_gold = queriesGold.get(q.query_id);
		
		for (int i = 0; i < docs.length; i++) {
				
				Document doc = searcher.doc(docs[i].doc);
				String id = doc.getField("id").stringValue();
				//String wiki_title = doc.getField("wiki_title").stringValue();
				//String type = doc.getField("type").stringValue();
				//String wiki_text = doc.getField("wiki_text").stringValue();
				if (id.equalsIgnoreCase(q_gold.answer)) {
					System.out.print('\t' + " found");
					n_found++;
					break;
				}
				q.answers.add(id);
			}
		
		System.out.println();
		
		}
		
	private static void loadGoldStandard(String filename) throws IOException {
		
		BufferedReader input;
		
		try {
		
			input = new BufferedReader(new FileReader(filename));
			String line = null;
	        
			while (( line = input.readLine()) != null){
	          String[] contents = line.split("\t");	          
	          GoldStandardQuery gold = new GoldStandardQuery(contents[0], contents[1], contents[2], contents[3], contents[4]);
	          queriesGold.put(contents[0], gold);
	        }
	        
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();    	
		}
	}
	
	private static void generateOutput(String output) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (Iterator<KBPQuery> iterator = queries.iterator(); iterator.hasNext();) {
			KBPQuery q = (KBPQuery) iterator.next();
			out.println(q.query_id.trim()+"\t"+q.answer_kb_id.trim());
		}
		out.close();		
	}
	
	private static String getSupportDocument(KBPQuery q) {
		
		StringBuilder contents = new StringBuilder();
	    
	    try {
	    	
	    	//use buffering, reading one line at a time
	    	//FileReader always assumes default encoding is OK!
	    	
	    	String file = docslocations.get(q.docid).trim()+"/"+q.docid+".sgm";
	    	
	    	BufferedReader input =  new BufferedReader(new FileReader(file));
	    	
	    	try {
	    		String line = null; //not declared within while loop
		        /*
		        * readLine is a bit quirky :
		        * it returns the content of a line MINUS the newline.
		        * it returns null only for the END of the stream.
		        * it returns an empty String if two newlines appear in a row.
		        */
		        while (( line = input.readLine()) != null){
		          contents.append(line);
		          contents.append(System.getProperty("line.separator"));
		        }
	      }
	      finally {
	        input.close();
	      }
	    }
	    catch (IOException ex){
	      ex.printStackTrace();
	    }
	    
	    return contents.toString();		
	}
	
	private static String concatenateEntities(String str1, String str2) {
		
		String result = new String();
		
		Joiner orJoiner = Joiner.on(" OR ");
		
		if (str1.length() > 0 && str2.length()>0) {
			result = orJoiner.join(str1, str2);
		}
		
		else if (str1.length()>0 && str2.length()==0) {
			result = str1;
		}
		
		else if (str1.length()==0 && str2.length()>0) {
			result = str2;
		}
		
		return result;
	}
	
	private static ScoreDoc[] queryKB(KBPQuery q) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		
		HashMap<String, HashSet<String>> query = generateQuery(q); 

		Joiner orJoiner = Joiner.on(" OR ");
		
		HashSet<String> strings = query.get("strings");
		HashSet<String> tokens = query.get("tokens");
		
		String qString = orJoiner.join(strings);		
		String qTokens = orJoiner.join(tokens);
		
		String queryString =  qString + " OR " + qTokens;
		
		String persons = orJoiner.join(q.persons); 
		String organizations = orJoiner.join(q.organizations);
		String places = orJoiner.join(q.places);
		
		String queryEntities = concatenateEntities(persons, organizations);
		queryEntities += concatenateEntities(queryEntities, places);
		
		//System.out.println("queryString:" + queryString);
		//System.out.println("queryEntities:" + queryEntities);
		
		q.query.put("queryEntities", queryEntities);
		q.query.put("queryString", qString);
		q.query.put("queryTokens", qTokens);
		
		//System.out.println(queryString);
		
		//queryString += " OR " + queryEntities;
		
		//query the name and the wiki_title with the alternative names and tokens made up from the alternative names
		MultiFieldQueryParser queryParserTitle = new MultiFieldQueryParser(new String[] {"name", "wiki_title","wiki_text"}, analyzer);					
		ScoreDoc[] scoreDocs = null;
		
		try {
			TopDocs docs = searcher.search(queryParserTitle.parse(queryString), 30);
			scoreDocs = docs.scoreDocs;
			
		} catch (Exception e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}

		//BM25BooleanQuery query = new BM25BooleanQuery(q.name, fields, analyzer, boosts, bParams);
		
		return scoreDocs;
	}

	private static HashMap<String, HashSet<String>> generateQuery(KBPQuery q) {
		
		HashSet<String> queryStrings = new HashSet<String>(); 		
		HashSet<String> queryTokens = new HashSet<String>();
			
		HashMap<String, HashSet<String>> query = new HashMap<String,HashSet<String>>();
		
		queryStrings.add('"' + q.name + '"');
		
		String[] tmp = q.name.split("\\s");
		for (int z = 0; z < tmp.length; z++) {
			if (!tmp[z].matches("\\s*-\\s*|.*\\!|\\!.*|.*\\:|\\:.*|\\s*")) {
				queryTokens.add(tmp[z]);
			}
		}
		
		for (Iterator<String> iterator = q.alternative_names.iterator(); iterator.hasNext();) {
			String alternative = (String) iterator.next();
			
			String queryParsed = alternative.replaceAll("\\(", "").replaceAll("\\)","").
										replaceAll("\\]", "").replaceAll("\\[", "").replaceAll("\"", "");
			
			queryStrings.add('"' + queryParsed + '"');
			
			String[] tokens = queryParsed.split("\\s");
			
			for (int i = 0; i < tokens.length; i++) {
				if (!tokens[i].matches("\\s*-\\s*|.*\\!|\\!.*|.*\\:|\\:.*|\\s*")) {
					queryTokens.add(tokens[i].trim());
				}
			}
		}
		
		query.put("strings", queryStrings);
		query.put("tokens", queryTokens);
				
		return query;
		
	}
	
	private static void loadDocsLocations(String filename) throws Exception {
		
		BigFile file = new BigFile(filename);
		String[] parts;
		
		for (String line : file) {		
			parts = line.split(".sgm");			
			docslocations.put(parts[0], parts[1]);
		}
		
	}
}
