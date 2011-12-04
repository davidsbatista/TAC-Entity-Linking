package tac.kbp.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import redis.clients.jedis.BinaryJedis;
import tac.kbp.kb.ivo_spellchecker.SpellChecker;
import tac.kbp.queries.GoldStandardQuery;
import tac.kbp.queries.KBPQuery;
import tac.kbp.utils.misc.BigFile;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

public class Definitions {
	
	/* named-entities type */
	public enum NERType {
	    PERSON, ORGANIZATION, PLACE, UNK
	}
	
	/* queries */
	public static List<KBPQuery> queries = null;
	public static HashMap<String, GoldStandardQuery> queriesGold = new HashMap<String, GoldStandardQuery>();
	public static String queries_set = new String();
	
	/* resources locations */
	public static String named_entities_supportDoc = "/collections/TAC-2011/named-entities-Stanford-CRF-XML";
	public static String serializedClassifier = "/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz";
	public static Set<String> stop_words = new HashSet<String>();
	
	public static HashMap<Integer, String> queries_topics = new HashMap<Integer, String>();
	public static HashMap<Integer, String> kb_topics = new HashMap<Integer, String>();
	
	/* lucene indexes */
	public static IndexSearcher searcher = null;
	public static SpellChecker spellchecker = null;
	public static IndexSearcher documents = null;

	/* StanfordNER CRF classifier */
	public static AbstractSequenceClassifier classifier = null;
	
	/* REDIS server */
	public static int redis_port = 6379;
	public static String redis_host = "agatha";
	public static BinaryJedis binaryjedis = null;
	
	
	public static void loadAll(String queriesPath, String stopWordsFile, String goldStandardPath, String kbIndex, String spellCheckerIndex, String dcIndex, String queries_lda_topics, String kb_lda_topics) throws Exception {
		
		/* Lucene Index */
		System.out.println("Knowledge Base index: " + kbIndex);
		searcher = new IndexSearcher(FSDirectory.open(new File(kbIndex)));
		
		/* SpellChecker Index */
		System.out.println("SpellChecker index: " + spellCheckerIndex);
		FSDirectory spellDirectory = FSDirectory.open(new File(spellCheckerIndex));
		spellchecker = new SpellChecker(spellDirectory, "name", "id");
		
		/* Document Collection Index */
		System.out.println("Document Collection index: " + dcIndex);
		documents = new IndexSearcher(FSDirectory.open(new File(dcIndex)));
		
		System.out.println("Loading stopwords from: " + stopWordsFile);
		loadStopWords(stopWordsFile);
		
		System.out.println("Loading queries answers from: " + goldStandardPath);
		loadGoldStandard(goldStandardPath);
		
		loadClassifier(serializedClassifier);
		
		System.out.println("Loading queries from: " + queriesPath);
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(queriesPath);
		
		System.out.println("Loading queries LDA topics from: " + queries_lda_topics);
		loadLDATopics(queries_lda_topics,queries_topics);
		
		System.out.println("Loading KB LDA topics from: " + kb_lda_topics);
		loadLDATopics(kb_lda_topics,kb_topics);
		
		System.out.println("Number of lda topics loaded: " + Definitions.queries_topics.size());
		
		System.out.println("Connecting to REDIS server.. ");
		binaryjedis = new BinaryJedis(redis_host, redis_port);

		if (queriesPath.contains("2009"))
			queries_set = "2009";
		
		if (queriesPath.contains("2010"))
			queries_set = "2010";
		
		if (queriesPath.contains("2011"))
			queries_set = "2011";
		
	}
	
	public static void loadAll(String queriesPath, String docLocationsPath, String stopWordsFile) throws Exception {
		
		System.out.println("Loading stopwords from: " + stopWordsFile);
		loadStopWords(stopWordsFile);
		
		/*
		System.out.println("Loading support documents locations from: " + docLocationsPath);
		loadDocsLocations(docLocationsPath);
		*/
		
		System.out.println("Loading queries from: " + queriesPath);
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(queriesPath);
		
		if (queriesPath.contains("2009"))
			queries_set = "2009";
		
		if (queriesPath.contains("2010"))
			queries_set = "2010";
		
		if (queriesPath.contains("2011"))
			queries_set = "2011";
	}
	
	public static void loadClassifier(String filename) {
		classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
	}

	public static void loadLDATopics(String filename, HashMap<Integer, String> hashtable) throws Exception {
		
		BigFile file = new BigFile(filename);
		int i=0;
		
		for (String line : file) {
			hashtable.put(i, line);
			i++;
		}
		
		System.out.println("lines red: " + i);
		
	}

	public static void loadStopWords(String file) { 
		
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
	
	public static void loadGoldStandard(String filename) throws IOException {
		
		BufferedReader input;
		
		if (filename.contains("2009") || filename.contains("2010/trainning")) {
			
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
}
