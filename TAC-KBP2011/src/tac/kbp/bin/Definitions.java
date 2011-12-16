package tac.kbp.bin;

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

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import redis.clients.jedis.BinaryJedis;
import tac.kbp.kb.index.spellchecker.SpellChecker;
import tac.kbp.queries.GoldQuery;
import tac.kbp.queries.KBPQuery;
import tac.kbp.utils.misc.BigFile;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

public class Definitions {
	
	/* Settings for Retrieval from Lucene */
	
	public static int max_retrieves = 10;
	public static int max_candidates = 50;
	
	
	/* named-entities type */
	public enum NERType {
	    PERSON, ORGANIZATION, PLACE, UNK
	}
	
	/* queries */
	public static List<KBPQuery> queries = null;
	public static HashMap<String, GoldQuery> queriesGold = new HashMap<String, GoldQuery>();
	public static String queries_set = new String();
	
	/* resources locations */
	public static String named_entities_supportDoc = "/collections/TAC-2011/named-entities-Stanford-CRF-XML";
	public static String serializedClassifier = "/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz";
	public static String stop_words_location = "/collections/TAC-2011/resources/stopwords.txt";
	public static String KB_location = "/collections/TAC-2011/index";
	public static String SpellChecker_location = "/collections/TAC-2011/spellchecker_index";
	public static String DocumentCollection_location = "/collections/TAC-2011/document_collection_index";
	public static Set<String> stop_words = new HashSet<String>();
	public static String kb_lda_topics = "/collections/TAC-2011/LDA/model/model-final.theta";
	public static String queries_lda_path = "/collections/TAC-2011/LDA/queries/";
	public static String gold_standard_path = "/collections/TAC-2011/queries/ivo/";
	
	public static HashMap<Integer, String> queries_topics = new HashMap<Integer, String>();
	public static HashMap<Integer, String> kb_topics = new HashMap<Integer, String>();
	
	static String lda_topics = new String();
	static String gold_standard = new String();
	
	/* lucene indexes */
	public static IndexSearcher searcher = null;
	public static SpellChecker spellchecker = null;
	public static IndexSearcher documents = null;
	public static IndexReader docs_reader = null;

	/* StanfordNER CRF classifier */
	@SuppressWarnings("rawtypes")
	public static AbstractSequenceClassifier classifier = null;
	
	/* REDIS server */
	public static int redis_port = 6379;
	public static String redis_host = "agatha";
	public static BinaryJedis binaryjedis = null;
	
	
	public static void loaddRecall(String queriesFile) throws CorruptIndexException, IOException {
		
		/* Lucene Index */
		System.out.println("Knowledge Base index: " + KB_location);
		searcher = new IndexSearcher(FSDirectory.open(new File(KB_location)));
		
		/* SpellChecker Index */
		System.out.println("SpellChecker index: " + SpellChecker_location);
		FSDirectory spellDirectory = FSDirectory.open(new File(SpellChecker_location));
		spellchecker = new SpellChecker(spellDirectory, "name", "id");
		
		/* Document Collection Index */
		System.out.println("Document Collection (IndexReader): " + DocumentCollection_location);
		documents = new IndexSearcher(FSDirectory.open(new File(DocumentCollection_location)));
		docs_reader = documents.getIndexReader();
		
		//Stop-Words
		System.out.println("Loading stopwords from: " + stop_words_location);
		loadStopWords(stop_words_location);
		
		if (queriesFile.contains("train_queries_2009")) {
			lda_topics = queries_lda_path+"train_queries_2009.txt.theta";
			gold_standard = gold_standard_path+"train_results_2009.tab"; 
			
		}
		else if (queriesFile.contains("train_queries_2010")) {
			lda_topics = queries_lda_path+"train_queries_2010.txt.theta";
			gold_standard = gold_standard_path+"train_results_2010.tab";
		}
		
		else if (queriesFile.contains("train_queries_2011")) {
			lda_topics = queries_lda_path+"train_queries_2011.txt.theta";
			gold_standard = gold_standard_path+"train_results_2011.tab";
			
		}
		
		else if (queriesFile.contains("test_queries_2009")) {
			lda_topics = queries_lda_path+"test_queries_2009.txt.theta";
			gold_standard = gold_standard_path+"test_results_2009.tab";
		}
		
		else if (queriesFile.contains("test_queries_2010")) {
			lda_topics = queries_lda_path+"test_queries_2010.txt.theta";
			gold_standard = gold_standard_path+"test_results_2010.tab";
		}
		
		else if (queriesFile.contains("test_queries_2011")) {
			lda_topics = queries_lda_path+"test_queries_2011.txt.theta";
			gold_standard = gold_standard_path+"test_results_2011.tab";
		}
		
		//Queries answer file
		System.out.println("Loading queries answers from: " + gold_standard);
		loadGoldStandard(gold_standard);
		
		//Queries XML file
		System.out.println("Loading queries from: " + queriesFile);
		queries = tac.kbp.queries.xml.ParseQueriesXMLFile.loadQueries(queriesFile);
		
		System.out.println("Connecting to REDIS server.. ");
		binaryjedis = new BinaryJedis(redis_host, redis_port);
	}
	
	public static void loadAll(String queriesFile) throws Exception {
		
		/* Lucene Index */
		System.out.println("Knowledge Base index: " + KB_location);
		searcher = new IndexSearcher(FSDirectory.open(new File(KB_location)));
		
		/* SpellChecker Index */
		System.out.println("SpellChecker index: " + SpellChecker_location);
		FSDirectory spellDirectory = FSDirectory.open(new File(SpellChecker_location));
		spellchecker = new SpellChecker(spellDirectory, "name", "id");
		
		/* Document Collection Index */
		System.out.println("Document Collection index: " + DocumentCollection_location);
		documents = new IndexSearcher(FSDirectory.open(new File(DocumentCollection_location)));
		
		//Stop-Words
		System.out.println("Loading stopwords from: " + stop_words_location);
		loadStopWords(stop_words_location);
		
		if (queriesFile.contains("train_queries_2009")) {
			lda_topics = queries_lda_path+"train_queries_2009.txt.theta";
			gold_standard = gold_standard_path+"train_results_2009.tab"; 
			
		}
		else if (queriesFile.contains("train_queries_2010")) {
			lda_topics = queries_lda_path+"train_queries_2010.txt.theta";
			gold_standard = gold_standard_path+"train_results_2010.tab";
		}
		
		else if (queriesFile.contains("train_queries_2011")) {
			lda_topics = queries_lda_path+"train_queries_2011.txt.theta";
			gold_standard = gold_standard_path+"train_results_2011.tab";
			
		}
		
		else if (queriesFile.contains("test_queries_2009")) {
			lda_topics = queries_lda_path+"test_queries_2009.txt.theta";
			gold_standard = gold_standard_path+"test_results_2009.tab";
		}
		
		else if (queriesFile.contains("test_queries_2010")) {
			lda_topics = queries_lda_path+"test_queries_2010.txt.theta";
			gold_standard = gold_standard_path+"test_results_2010.tab";
		}
		
		else if (queriesFile.contains("test_queries_2011")) {
			lda_topics = queries_lda_path+"test_queries_2011.txt.theta";
			gold_standard = gold_standard_path+"test_results_2011.tab";
		}
		
		//Queries answer file
		System.out.println("Loading queries answers from: " + gold_standard);
		loadGoldStandard(gold_standard);
		
		//Queries XML file
		System.out.println("Loading queries from: " + queriesFile);
		queries = tac.kbp.queries.xml.ParseQueriesXMLFile.loadQueries(queriesFile);

		//LDA topics for queries
		System.out.print("Loading queries LDA topics from: " + lda_topics + "..." );
		loadLDATopics(lda_topics,queries_topics);
		
		//LDA topics for KB
		System.out.print("Loading KB LDA topics from: " + kb_lda_topics + "..." );
		loadLDATopics(kb_lda_topics,kb_topics);
		
		//StanfordNER: Classifier MUC 3 
		loadClassifier(serializedClassifier);
		
		System.out.println("Connecting to REDIS server.. ");
		binaryjedis = new BinaryJedis(redis_host, redis_port);
		
		System.out.println(tac.kbp.bin.Definitions.queries.size() + " queries loaded");
		System.out.println(tac.kbp.bin.Definitions.stop_words.size() + " stopwords loaded");
		System.out.println(tac.kbp.bin.Definitions.queriesGold.size() + " queries gold standard loaded");

	}
	
	/*
	public static void loadAll(String queriesPath) throws Exception {
		
		System.out.println("Loading stopwords from: " + stop_words_location);
		loadStopWords(stop_words_location);
		
		System.out.println("Loading queries from: " + queriesPath);
		queries = tac.kbp.queries.xml.ParseXML.loadQueries(queriesPath);
		
		//Document Collection Index
		System.out.println("Document Collection index: " + DocumentCollection_location);
		documents = new IndexSearcher(FSDirectory.open(new File(DocumentCollection_location)));
	}
	*/
	
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
		
		if (filename.contains("2009") || filename.contains("2010/trainning") || filename.contains("train_results_2010")) {
			
			try {
				
				input = new BufferedReader(new FileReader(filename));
				String line = null;
		        
				while (( line = input.readLine()) != null){
		          String[] contents = line.split("\t");	          
		          GoldQuery gold = new GoldQuery(contents[0], contents[1], contents[2]);
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
		          GoldQuery gold = new GoldQuery(contents[0], contents[1], contents[2], contents[3], contents[4]);
		          queriesGold.put(contents[0], gold);
		        }
		        
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();    	
			}
			
		}

	}
}
