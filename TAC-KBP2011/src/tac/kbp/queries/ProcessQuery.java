package tac.kbp.queries;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import com.google.common.base.Joiner;

import redis.clients.jedis.BinaryJedis;
import tac.kbp.kb.ivo_spellchecker.SpellChecker;
import tac.kbp.kb.ivo_spellchecker.SuggestWord;
import tac.kbp.utils.BigFile;

public class ProcessQuery {
	
	static IndexSearcher searcher = null;
	static SpellChecker spellchecker = null;
	
	static HashMap<String, String> docslocations = new HashMap<String, String>();
	static Set<String> stop_words = new HashSet<String>();
	 
	static List<KBPQuery> queries = null;
	static HashMap<String, GoldStandardQuery> queriesGold = new HashMap<String, GoldStandardQuery>();
	
	//static String named_entities_supportDoc = "/collections/TAC-2011/named-entities";
	static String named_entities_supportDoc = "/collections/TAC-2011/named-entities-Stanford-CRF-XML";
	
	static String serializedClassifier = "/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz";
	
	static int total_n_docs = 0;
	static int n_found = 0;
	static int n_queries_zero_docs = 0;
	static int n_docs_not_found_and_answer_is_NIL = 0;
	static int n_docs_not_found = 0;
	
	public static void main(String[] args) throws Exception {
		
		/* Load the queries file */
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(args[0]);
		System.err.println(queries.size() + " queries loaded");
		
		/* Load the text file with location of support documents */
		//loadDocsLocations(args[1]);
		//System.out.println(docslocations.size() + " documents locations loaded");
		
		/* load english stopwords list */
		loadStopWords(args[2]);
		System.out.println(stop_words.size() + " stopwords loaded");
		
		/* load query-gold standard queries */
		loadGoldStandard(args[3]);
		System.out.println(queriesGold.size() + " queries gold standard loaded");
		
		/* Lucene Index */
		searcher = new IndexSearcher(FSDirectory.open(new File(args[4])));
		
		/* Spellchecker Index */
		FSDirectory spellDirectory = FSDirectory.open(new File(args[5]));
		spellchecker = new SpellChecker(spellDirectory, "name", "id");
 
		int port = 6379;
		String host = "agatha";
		BinaryJedis binaryjedis = new BinaryJedis(host, port);
		
		for (Iterator<KBPQuery> iterator = queries.iterator(); iterator.hasNext();) {
			KBPQuery query = (KBPQuery) iterator.next();
			
			System.out.print(query.query_id + " \"" + query.name + '"');
			getSenses(binaryjedis, query);
			processQuery(query);		
		}
		
		System.out.println("Documents Retrieved: " + Integer.toString(total_n_docs));
		System.out.println("Queries: " + Integer.toString(queries.size()));
		System.out.println("Docs p/ query: " + ( (float) total_n_docs / (float) queries.size()));
		System.out.println("Queries with 0 docs returned: " + Integer.toString(n_queries_zero_docs));
		System.out.println("Queries NIL and not found (NIL): "+ Integer.toString(n_docs_not_found_and_answer_is_NIL));
		System.out.println("Queries not NIL and not found (Misses): "+ Integer.toString(n_docs_not_found));
		System.out.println("Queries not NIL and found (Found)" + n_found);

	}
	
	private static void loadStopWords(String file) { 
		
		try{
			  FileInputStream fstream = new FileInputStream(file);
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  
			  while ((strLine = br.readLine()) != null)   {				  
				  stop_words.add(strLine.trim());
			  }
			  
			  in.close();
			  
			}
		
			catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
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
		//loadNamedEntities(q);
		//loadNamedEntitiesXML(q);
		
		int n_docs = queryKB(q);
		
		System.out.print("  " + n_docs);
		
		total_n_docs += n_docs;
		
		if (n_docs == 0)
			n_queries_zero_docs++;
		
		System.out.print("\t correct answer: "+ queriesGold.get(q.query_id).answer);
		
		findCorrectEntity(q);
	}

	private static void findCorrectEntity(KBPQuery q) throws CorruptIndexException, IOException {
				
		GoldStandardQuery q_gold = queriesGold.get(q.query_id);
		
		boolean found = false;
		
		for (String eid : q.candidates) {
			if (eid.equalsIgnoreCase(q_gold.answer)) {
				System.out.print('\t' + " found");
				n_found++;
				found = true;
				break;
			}
		}
		
		if (!found && q_gold.answer.startsWith("NIL"))
			n_docs_not_found_and_answer_is_NIL++;
		
		if (!found && !q_gold.answer.startsWith("NIL"))
			n_docs_not_found++;
		
		System.out.println();
		}
		
	private static void loadGoldStandard(String filename) throws IOException {
		
		BufferedReader input;
		
		if (filename.contains("2009")) {
			
			try {
				
				input = new BufferedReader(new FileReader(filename));
				String line = null;
		        
				while (( line = input.readLine()) != null){
		          String[] contents = line.split("\t");	          
		          GoldStandardQuery gold = new GoldStandardQuery(contents[0], contents[1], contents[2]);
		          queriesGold.put(contents[0], gold);
		        }
		        
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();    	
			}
			
		}
		
		else {
			
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

	}
	
	private static void generateOutput(String output) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (Iterator<KBPQuery> iterator = queries.iterator(); iterator.hasNext();) {
			KBPQuery q = (KBPQuery) iterator.next();
			out.println(q.query_id.trim()+"\t"+q.answer_kb_id.trim());
		}
		out.close();		
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
	
	private static int queryKB(KBPQuery q) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		HashMap<String, HashSet<String>> query = generateQuery(q);
		
		Set<SuggestWord> suggestedwords = new HashSet<SuggestWord>();
		
		for (String sense : query.get("strings")) {
			List<SuggestWord> l = spellchecker.suggestSimilar(sense, 30);
			suggestedwords.addAll(l);
		}
		
		QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", analyzer);
		ScoreDoc[] scoreDocs = null;
		
		for (SuggestWord suggestWord : suggestedwords) {
			
			//System.out.print("suggestWord: " + suggestWord.string + "  " + suggestWord.eid);
					
			String queryS = "id:" + suggestWord.eid;
			TopDocs docs = searcher.search(queryParser.parse(queryS), 1);
			
			//System.out.println(" num docs:" + docs.totalHits);
			
			if (docs.totalHits == 0) {
				continue;				
			}
			
			else {
				scoreDocs = docs.scoreDocs;
				Document doc = searcher.doc(scoreDocs[0].doc);
				String id = doc.getField("id").stringValue();
				q.candidates.add(id);
			}

		}
		
		/*
		Joiner orJoiner = Joiner.on(" OR ");
		
		HashSet<String> strings = query.get("strings");
		HashSet<String> tokens = query.get("tokens");
		
		// remove stop words
		strings.removeAll(stop_words);
		tokens.removeAll(stop_words);
		
		String qString = orJoiner.join(strings);		
		String qTokens = orJoiner.join(tokens);
		
		String qStringTokens =  qString + " OR " + qTokens;
		
		/*
		String persons = orJoiner.join(q.persons); 
		String organizations = orJoiner.join(q.organizations);
		String places = orJoiner.join(q.places);
		
		String queryEntities = concatenateEntities(persons, organizations);
		queryEntities += concatenateEntities(queryEntities, places);
		
		if (queryEntities.length() > 0) {
			qStringTokens += " OR " + queryEntities;
		}

		//query the name and the wiki_title with the alternative names and tokens made up from the alternative names
		MultiFieldQueryParser multiFieldqueryParser = new MultiFieldQueryParser(org.apache.lucene.util.Version.LUCENE_30, new String[] {"name", "wiki_title","wiki_text"}, analyzer);		
		scoreDocs = null;

		try {
			
			TopDocs docs = searcher.search(multiFieldqueryParser.parse(qStringTokens), 30);
			scoreDocs = docs.scoreDocs;
			
			for (int i = 0; i < scoreDocs.length; i++) {
				Document doc = searcher.doc(scoreDocs[i].doc);
				String id = doc.getField("id").stringValue();
				q.candidates.add(id);
			}
			
		} catch (Exception e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);		
		}
		*/
		
		return q.candidates.size();
	}

	private static HashMap<String, HashSet<String>> generateQuery(KBPQuery q) {
		
		HashSet<String> queryStrings = new HashSet<String>(); 		
		HashSet<String> queryTokens = new HashSet<String>();
			
		HashMap<String, HashSet<String>> query = new HashMap<String,HashSet<String>>();
		
		queryStrings.add('"' + q.name + '"');
		
		String[] tmp = q.name.split("\\s");
		for (int z = 0; z < tmp.length; z++) {
			if (!tmp[z].matches("\\s*-\\s*|.*\\!|\\!.*|.*\\:|\\:.*|\\s*")) {
				queryTokens.add('"' + tmp[z] + '"');
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
					queryTokens.add('"' + tokens[i].trim() + '"');
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
