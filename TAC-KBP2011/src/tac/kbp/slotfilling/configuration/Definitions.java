package tac.kbp.slotfilling.configuration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import redis.clients.jedis.Jedis;
import tac.kbp.kb.index.spellchecker.SpellChecker;
import tac.kbp.slotfilling.db.MySQLConnection;

import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

public class Definitions {
		
	public static String basePath = "/collections/TAC-2011/";
	
	public static int max_docs = 70;
	
	/* indexes locations */
	public static String KB_location = basePath+"index";	
	public static String DocumentCollection_location = basePath+"DocumentCollection-Index";
	public static String WikipediaIndexEn_location = basePath+"Wikipedia-Index-En";
	
	/* support doc and named-entities recognizer */
	public static String serializedClassifier = basePath+"resources/english.muc.7class.distsim.crf.ser.gz";
	
	/* stopwords */
	public static String stop_words_location = basePath+"resources/stopwords.txt";
	public static Set<String> stop_words = new HashSet<String>();

	/* list of all entities in Wikipedia, for link disambiguation */
	static final double CHUNK_SCORE = 1.0;
	public static String entities = basePath+"resources/entities.txt";
	public static ExactDictionaryChunker chunker = null;
	
	/* Lucene indexes */
	public static IndexSearcher knowledge_base = null;
	public static IndexSearcher documents = null;
	public static SpellChecker spellchecker = null;
	
	public static IndexReader docs_reader = null;

	/* StanfordNER CRF classifier */
	@SuppressWarnings("rawtypes")
	public static AbstractSequenceClassifier classifier = null;
	
	/* REDIS server */
	public static int redis_port = 6379;
	public static String redis_host = "agatha.inesc-id.pt";
	public static Jedis jedis = null;
	
	/* MySQL Connections */
	public static Connection connection = null;
	
	
	/* lists of facts and entities */
	public static HashMap<String,LinkedList<String>> lists_of_answers = new HashMap<String, LinkedList<String>>();

	/* slots with only one answer */
	public static HashSet<String> one_answer_only = new HashSet<String>();
	

	/* load slots that have only one possibly answer */
	public static void oneAnswersSlots() {
		
		/*
	 	 *	just one correct answer:
		 *
		 *	per:place of birth
		 *	per:date of death
		 *	per:cause of death 	 
		 *	per:place of death
		 *  per:date of birth
		 *  per:age
		 *  per:origin
		 *  per:religion
		 *  
		 *  several correct answers
		 *  
		 *  per:charges
		 *  per:spouse
		 *  per:schools attended  
		 *	per:place of residence 	 
		 *	per:alternative name  
		 *	per:title
		 *	per:member of 
		 *	per:employee of 	 
		 *	per:children  
		 *	per:siblings 
		 *	per:other family
		 *	per:parents 
		 */
		
		one_answer_only.add("per:place_of_birth");
		one_answer_only.add("per:date_of_death");
		one_answer_only.add("per:cause_of_death");
		one_answer_only.add("per:place_of_death");
		one_answer_only.add("per:date_of_birth");
		one_answer_only.add("per:age");
		one_answer_only.add("per:origin");
		one_answer_only.add("per:religion");
		
	}
	
	
	/* load lists */
	public static void loadLists(String path) throws IOException {
		
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles(); 
		                    
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) { 
				BufferedReader input = new BufferedReader( new FileReader(path+listOfFiles[i].getName()) );
				lists_of_answers.put(listOfFiles[i].getName(), new LinkedList<String>());
				
				LinkedList<String> list = new LinkedList<String>();
				
		    	String aux = null;	    	
				while ((aux=input.readLine())!=null) {			
					if (aux.length()==0)
						continue;
			
					list.add(aux);
				}
				
				lists_of_answers.put(listOfFiles[i].getName(), list);
			}	
		}
		
		Set<String> lists = lists_of_answers.keySet();
		
		for (String key : lists) {
			System.out.println(key + '\t' + lists_of_answers.get(key).size());
		}
	}
	
	
	public static void getDBConnection() throws Exception{
		connection = MySQLConnection.getConnection("root", "07dqeuedm", "jdbc:mysql://borat/reverb-extractions");
	}
	
	public static void closeDBConnection() throws Exception{
		connection.close();
	}
		
	/* builds a dictionary of entities from the KB */
	public static void buildDictionary() throws IOException {
		
    	System.out.println("Loading dictionary...");
    	
    	BufferedReader input = new BufferedReader( new FileReader(entities) );
    	MapDictionary<String> dictionary = new MapDictionary<String>();	
    	String aux = null;
    	
		while ((aux=input.readLine())!=null) {			
			if (aux.length()==0)
				continue;

			aux.replaceAll("([A-Z])"," $1").trim();			
			dictionary.addEntry(new DictionaryEntry<String>(aux,aux,CHUNK_SCORE));
	        
		}
		
        chunker = new ExactDictionaryChunker(dictionary,IndoEuropeanTokenizerFactory.INSTANCE,true,true);
        System.out.println("Dictionary contains " + dictionary.size() + " entries.");
    }

	
	public static void loadKBIndex() throws CorruptIndexException, IOException {
		
		System.out.println("Knowledge Base index: " + KB_location);
		Directory KBIndexDirectory = FSDirectory.open(new File(KB_location));		
		knowledge_base = new IndexSearcher((IndexReader.open(KBIndexDirectory)));
		
	}
	
	/* Document Collection Index */		
	public static void loadDocumentCollecion() throws CorruptIndexException, IOException {
		
		System.out.println("Document Collection index: " + DocumentCollection_location);		
		Directory DocsIndexDirectory = FSDirectory.open(new File(DocumentCollection_location));		
		documents = new IndexSearcher((IndexReader.open(DocsIndexDirectory)));		
	}
	
	/* REDIS Connection */
	public static void connectionREDIS() {
		System.out.println("Connecting to REDIS server.. ");
		jedis = new Jedis(redis_host, redis_port);
	}

	/* Stanford NER-CRF Classifier */
	public static void loadClassifier(String filename) {
		classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
	}
	
	/* load StopWords list */
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
}