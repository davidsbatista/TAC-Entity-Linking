package tac.kbp.slotfilling.configuration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import redis.clients.jedis.Jedis;
import tac.kbp.entitylinking.queries.GoldQuery;
import tac.kbp.entitylinking.queries.ELQuery;
import tac.kbp.kb.index.spellchecker.SpellChecker;
import tac.kbp.slotfilling.db.MySQLConnection;
import tac.kbp.utils.misc.BigFile;

import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

import de.tudarmstadt.ukp.wikipedia.api.DatabaseConfiguration;
import de.tudarmstadt.ukp.wikipedia.api.Wikipedia;
import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language;
import de.tudarmstadt.ukp.wikipedia.api.exception.WikiApiException;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

public class Definitions {
		
	public static String basePath = "/collections/TAC-2011/";
	
	/* indexes locations */
	public static String KB_location = basePath+"index";	
	public static String DocumentCollection_location = basePath+"DocumentCollection-Index";
	public static String WikipediaIndexEn_location = basePath+"Wikipedia-Index-En";
	
	/* support doc and named-entities recognizer */
	public static String serializedClassifier = basePath+"resources/all.3class.distsim.crf.ser.gz";
	
	/* stopwords */
	public static String stop_words_location = basePath+"resources/stopwords.txt";
	public static Set<String> stop_words = new HashSet<String>();

	/* list of all entities in Wikipedia, for link disambiguation */
	static final double CHUNK_SCORE = 1.0;
	public static String entities = basePath+"resources/entities.txt";
	public static ExactDictionaryChunker chunker = null;
	
	/* JWLP Wikipedia Interface */
	public static DatabaseConfiguration dbConfig;
	public static Wikipedia wiki;
	
	/* Lucene indexes */
	public static IndexSearcher knowledge_base = null;
	public static IndexSearcher wikipediaEn = null;
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
	
	
	
	public static void getDBConnection() throws Exception{
		connection = MySQLConnection.getConnection("root", "agatha", "jdbc:mysql://agatha/extractions");
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

	/* Wikipedia Index */		
	public static void loadWikipediaIndex() throws CorruptIndexException, IOException {
		
		System.out.println("Wikpedia index: " + WikipediaIndexEn_location);		
		Directory WikiIndexDirectory = FSDirectory.open(new File(WikipediaIndexEn_location));		
		wikipediaEn = new IndexSearcher((IndexReader.open(WikiIndexDirectory)));	
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
	
	public static void initJWLPWikipedia() throws WikiApiException {
		
		String host = "borat.inesc-id.pt";
		String database = "JWLPWikiEn";
		String user = "wiki";
		String password = "07dqeuedm"; 
		
		// configure the database connection parameters
		dbConfig = new DatabaseConfiguration();
		dbConfig.setHost(host);
		dbConfig.setDatabase(database);
		dbConfig.setUser(user);
		dbConfig.setPassword(password);
		dbConfig.setLanguage(Language.english);

		System.out.print("Loading JWLP Wikipedia interface...");
		
		// Create the Wikipedia object
		wiki = new Wikipedia(dbConfig);
		
		System.out.println("\t" + wiki.getWikipediaId());
		 
	}
	
}