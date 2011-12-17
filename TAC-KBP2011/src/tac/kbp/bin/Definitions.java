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
import tac.kbp.queries.xml.ParseQueriesXMLFile;

public class Definitions {
	
	/* Settings for Retrieval from Lucene */
	public static int max_candidates = 30;
	
	/* named-entities type */
	public enum NERType {
	    PERSON, ORGANIZATION, PLACE, UNK
	}
	
	/* queries */
	public static List<KBPQuery> queriesTrain = null;
	public static List<KBPQuery> queriesTest = null;
	
	public static String test_queries = new String();
	public static String test_queries_answers = new String();
	
	public static HashMap<String, GoldQuery> queriesGoldTrain = null;
	public static HashMap<String, GoldQuery> queriesGoldTest = null;
	
	public static String queriesPath = "/collections/TAC-2011/queries/ivo/";
	
	/* resources locations */
	public static String KB_location = "/collections/TAC-2011/index";
	public static String SpellChecker_location = "/collections/TAC-2011/spellchecker_index";
	public static String DocumentCollection_location = "/collections/TAC-2011/document_collection_index";
	
	public static String named_entities_supportDoc = "/collections/TAC-2011/named-entities-Stanford-CRF-XML";
	public static String serializedClassifier = "/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz";
	
	public static String stop_words_location = "/collections/TAC-2011/resources/stopwords.txt";
	public static Set<String> stop_words = new HashSet<String>();
	
	public static String kb_lda_topics = "/collections/TAC-2011/LDA/model/model-final.theta";
	public static String queries_lda_path = "/collections/TAC-2011/LDA/queries/";
	public static String gold_standard_path = "/collections/TAC-2011/queries/ivo/";
	public static HashMap<Integer, String> queries_topics = new HashMap<Integer, String>();
	public static HashMap<Integer, String> kb_topics = new HashMap<Integer, String>();
	static String lda_topics_train = new String();
	static String lda_topics_test = new String();
	static String gold_standard = new String();
	
	/* Lucene indexes */
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
	
	/* 3rd party software */
	public static String SVMRankPath = "/collections/TAC-2011/SVMRank/";
	public static String SVMRanklLearn = "svm_rank_learn";
	public static String SVMRanklClassify = "svm_rank_classify";
	
	public static void loaddRecall(String queriesFile, String n_candidates) throws CorruptIndexException, IOException {
		
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
		
		/* Number of candidates to retrieve per sense */
		Definitions.max_candidates = Integer.parseInt(n_candidates);
		System.out.println("Number of candidates to retrieve per sense: " + Definitions.max_candidates);
		
		//Stop-Words
		System.out.println("Loading stopwords from: " + stop_words_location);
		loadStopWords(stop_words_location);
		
		if (queriesFile.contains("train_queries_2009")) {
			lda_topics_train = queries_lda_path+"train_queries_2009.txt.theta";
			gold_standard = gold_standard_path+"train_results_2009.tab"; 
			
		}
		else if (queriesFile.contains("train_queries_2010")) {
			lda_topics_train = queries_lda_path+"train_queries_2010.txt.theta";
			gold_standard = gold_standard_path+"train_results_2010.tab";
		}
		
		else if (queriesFile.contains("train_queries_2011")) {
			lda_topics_train = queries_lda_path+"train_queries_2011.txt.theta";
			gold_standard = gold_standard_path+"train_results_2011.tab";
			
		}
		
		else if (queriesFile.contains("test_queries_2009")) {
			lda_topics_train = queries_lda_path+"test_queries_2009.txt.theta";
			gold_standard = gold_standard_path+"test_results_2009.tab";
		}
		
		else if (queriesFile.contains("test_queries_2010")) {
			lda_topics_train = queries_lda_path+"test_queries_2010.txt.theta";
			gold_standard = gold_standard_path+"test_results_2010.tab";
		}
		
		else if (queriesFile.contains("test_queries_2011")) {
			lda_topics_train = queries_lda_path+"test_queries_2011.txt.theta";
			gold_standard = gold_standard_path+"test_results_2011.tab";
		}
		
		//Queries answer file
		System.out.println("Loading queries answers from: " + gold_standard);
		queriesGoldTrain = loadGoldStandard(gold_standard);
		
		//Queries XML file
		System.out.println("Loading queries from: " + queriesFile);
		queriesTrain = tac.kbp.queries.xml.ParseQueriesXMLFile.loadQueries(queriesFile);
		
		System.out.println("Connecting to REDIS server.. ");
		binaryjedis = new BinaryJedis(redis_host, redis_port);
	}
	
	public static void loadAll(String queriesTrainFile, String queriesTestFile) throws Exception {
		
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
		
		System.out.println();
		
		//StanfordNER: Classifier MUC 3 
		loadClassifier(serializedClassifier);
		
		//Stop-Words
		System.out.println("Loading stopwords from: " + stop_words_location);
		loadStopWords(stop_words_location);
		
		System.out.println();
		
		if (queriesTestFile!=null && queriesTestFile.contains("small")) {
			
			//Training Queries XML file
			System.out.println("Loading training queries from: " + queriesTrainFile);
			queriesTrain = ParseQueriesXMLFile.loadQueries(queriesTrainFile);
			
			//Test Queries XML file
			System.out.println("Loading test queries: " + queriesTestFile);
			queriesTest = ParseQueriesXMLFile.loadQueries(queriesTestFile);
			
			if (queriesTrainFile.contains("train_queries_2009")) {				
				lda_topics_train = queries_lda_path+"train_queries_2009.txt.theta";
				lda_topics_test = queries_lda_path+"test_queries_2009.txt.theta";				
				gold_standard = gold_standard_path+"train_results_2009.tab";
				test_queries_answers = gold_standard_path+"test_results_2009.tab";
			}
						
			//Test Queries answer file
			System.out.println("Loading test queries answers from: " + test_queries_answers);
			queriesGoldTest = loadGoldStandard(test_queries_answers);
			
			System.out.println("Loading training queries answers from: " + gold_standard);
			//Training Queries answer file
			queriesGoldTrain = loadGoldStandard(gold_standard);
			
			for (KBPQuery q : queriesTest) {
				q.gold_answer = queriesGoldTest.get(q.query_id).answer;
			}
			
			for (KBPQuery q : queriesTrain) {
				q.gold_answer = queriesGoldTrain.get(q.query_id).answer;
			}
			
			//LDA topics for queries
			System.out.print("Loading queries LDA topics from: " + lda_topics_train + "..." );
			loadLDATopics(lda_topics_train,queries_topics);
			
			//LDA topics for KB
			System.out.print("Loading KB LDA topics from: " + kb_lda_topics + "..." );
			loadLDATopics(kb_lda_topics,kb_topics);
			
		}
		
		else {
			
			if (queriesTrainFile.contains("train_queries_2009")) {
				lda_topics_train = queries_lda_path+"train_queries_2009.txt.theta";
				gold_standard = gold_standard_path+"train_results_2009.tab";
				test_queries = queriesPath+"test_queries_2009.xml";
				test_queries_answers = gold_standard_path+"test_results_2009.tab";
				lda_topics_test = queries_lda_path+"test_queries_2009.txt.theta";
				
			}
			else if (queriesTrainFile.contains("train_queries_2010")) {
				lda_topics_train = queries_lda_path+"train_queries_2010.txt.theta";
				gold_standard = gold_standard_path+"train_results_2010.tab";
				test_queries = queriesPath+"test_queries_2010.xml";
				test_queries_answers = gold_standard_path+"test_results_2010.tab";
				lda_topics_test = queries_lda_path+"test_queries_2010.txt.theta";
			}
			
			else if (queriesTrainFile.contains("train_queries_2011")) {
				lda_topics_train = queries_lda_path+"train_queries_2011.txt.theta";
				gold_standard = gold_standard_path+"train_results_2011.tab";
				test_queries = queriesPath+"test_queries_2010.xml";
				test_queries_answers = gold_standard_path+"test_results_2011.tab";
				lda_topics_test = queries_lda_path+"test_queries_2010.txt.theta";
				
			}
			
			else if (queriesTrainFile.contains("test_queries_2009")) {
				lda_topics_train = queries_lda_path+"test_queries_2009.txt.theta";
				gold_standard = gold_standard_path+"test_results_2009.tab";
			}
			
			else if (queriesTrainFile.contains("test_queries_2010")) {
				lda_topics_train = queries_lda_path+"test_queries_2010.txt.theta";
				gold_standard = gold_standard_path+"test_results_2010.tab";
			}
			
			else if (queriesTrainFile.contains("test_queries_2011")) {
				lda_topics_train = queries_lda_path+"test_queries_2011.txt.theta";
				gold_standard = gold_standard_path+"test_results_2011.tab";
			}
				
			//Loads all queries
			
			//Training Queries answer file
			System.out.println("Loading training queries answers from: " + gold_standard);
			queriesGoldTrain = loadGoldStandard(gold_standard);
			
			//Training Queries XML file
			System.out.println("Loading training queries from: " + queriesTrainFile);
			queriesTrain = ParseQueriesXMLFile.loadQueries(queriesTrainFile);
			
			for (KBPQuery q : queriesTrain) {
				q.gold_answer = queriesGoldTrain.get(q.query_id).answer;
			}
			
			//Test Queries answer file
			System.out.println("Loading test queries answers from: " + test_queries);
			queriesGoldTest = loadGoldStandard(test_queries_answers);
			
			//Test Queries XML file
			System.out.println("Loading test queries: " + test_queries);
			queriesTest = ParseQueriesXMLFile.loadQueries(test_queries);
			
			for (KBPQuery q : queriesTest) {
				q.gold_answer = queriesGoldTest.get(q.query_id).answer;
			}
			
			System.out.println();
	
			//LDA topics for queries
			System.out.print("Loading queries LDA topics from: " + lda_topics_train + "..." );
			loadLDATopics(lda_topics_train,queries_topics);
			
			//LDA topics for KB
			System.out.print("Loading KB LDA topics from: " + kb_lda_topics + "..." );
			loadLDATopics(kb_lda_topics,kb_topics);
		}
		
		System.out.println("Connecting to REDIS server.. ");
		binaryjedis = new BinaryJedis(redis_host, redis_port);
		
		System.out.println(tac.kbp.bin.Definitions.stop_words.size() + " stopwords loaded");
		System.out.println();
		System.out.println(tac.kbp.bin.Definitions.queriesTrain.size() + " queries loaded");
		System.out.println(tac.kbp.bin.Definitions.queriesGoldTrain.size() + " queries gold standard loaded");
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
	
	public static HashMap<String, GoldQuery> loadGoldStandard(String filename) throws IOException {
		
		BufferedReader input;
		HashMap<String, GoldQuery> queriesGoldTrain = new HashMap<String, GoldQuery>();
		
		if (filename.contains("2009") || filename.contains("2010/trainning") || filename.contains("train_results_2010")) {
			
			try {
				
				input = new BufferedReader(new FileReader(filename));
				String line = null;
		        
				while (( line = input.readLine()) != null){
		          String[] contents = line.split("\t");	          
		          GoldQuery gold = new GoldQuery(contents[0], contents[1], contents[2]);
		          queriesGoldTrain.put(contents[0], gold);
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
		          queriesGoldTrain.put(contents[0], gold);
		        }
		        
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();    	
			}
			
		}

		return queriesGoldTrain;
		
	}
}
