package tac.kbp.slotfilling.bin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import tac.kbp.slotfilling.configuration.Definitions;
import tac.kbp.slotfilling.queries.LoadQueries;
import tac.kbp.slotfilling.queries.SFQuery;
import tac.kbp.slotfilling.queries.attributes.Attribute;
import tac.kbp.slotfilling.queries.attributes.ORG_Attributes;
import tac.kbp.slotfilling.queries.attributes.PER_Attributes;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		// create Options object
		Options options = new Options();

		// add options		
		options.addOption("run", false, "complete run");
		
		// add argument options
		Option queriesTrain = OptionBuilder.withArgName("queriesTrain").hasArg().withDescription("XML file containing queries for trainning").create( "queriesTrain" );
		Option queriesTest = OptionBuilder.withArgName("queriesTest").hasArg().withDescription("XML file containing queries for testing").create( "queriesTest" );
		Option queriesTestAnswers = OptionBuilder.withArgName("queriesTestAnswers").hasArg().withDescription("test queries answers").create( "queriesTestAnswers" );
		Option queriesTrainAnswers = OptionBuilder.withArgName("queriesTrainAnswers").hasArg().withDescription("train queries answers").create( "queriesTrainAnswers" );
				
		options.addOption(queriesTrain);
		options.addOption(queriesTrainAnswers);
		options.addOption(queriesTest);
		options.addOption(queriesTestAnswers);
				
		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse( options, args );
		
		if (args.length == 0) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(" ", options );
			System.exit(0);
		}
		
		else {		
			if (line.hasOption("run"))
				run(line);	
		}
	}
	
	public static void parseQueries(Map<String, SFQuery> queries) throws Exception {
		
		Set<String> keys = queries.keySet();
		
		for (String k : keys) {
			
			SFQuery q = queries.get(k);
			
			/*
			System.out.println("q_id: " + q.query_id);
			System.out.println("name: " + q.name);	
			System.out.println("ignore:" + q.ignore);
			*/			
			q.getSupportDocument();
			
			// get nodeid from KB: wiki_text, other attributes
			if (!q.nodeid.startsWith("NIL"))
				q.getKBEntry();
			
			// get sentences where entity occurs
			q.extractSentences();
			//System.out.println("sentences: " + q.sentences.size());
			
			/*
			// extract other entities in the support document
			q.getNamedEntities();			
			System.out.println("persons: " + q.persons.size());
			System.out.println("places: " + q.places.size());
			System.out.println("org: " + q.organizations.size());
			*/
					
			// get alternative senses for entity name and extract acronyms from support doc.
			q.getAlternativeSenses();
			//q.extracAcronyms();
			//System.out.println("senses: " + q.alternative_names);			
			//System.out.println("abbreviations: " + q.abbreviations);

			//System.out.println("attributes to be filled:");
			if (q.etype.equalsIgnoreCase("PER")) {
				PER_Attributes attributes = (PER_Attributes) q.attributes;
				//System.out.println(attributes.toString());
				
			}
			else if (q.etype.equalsIgnoreCase("ORG")) {
				ORG_Attributes attributes = (ORG_Attributes) q.attributes;
				//System.out.println(attributes.toString());
			}
			
			q.queryCollection();
			
		}
		
	}
	
	public static void recall(Map<String, SFQuery> queries) {
				
		Map<String, List<SFQuery>> answers_found = new HashMap<String, List<SFQuery>>();
		Set<SFQuery> queries_with_zero_docs = new HashSet<SFQuery>();
		
		Set<String> keys = queries.keySet();
		int answer_doc_founded = 0;
		int answer_doc_not_founded = 0;
		int total_answer_doc_founded = 0;
		int total_answer_doc_not_founded = 0;
		
		for (String k : keys) {
			
			SFQuery q = queries.get(k);			
			//System.out.print(q.query_id + '\t' + q.name);
			answer_doc_founded = 0;
			answer_doc_not_founded = 0;
			
			if (q.documents.size()==0) {
				queries_with_zero_docs.add(q);
				for (HashMap<String, String> answer : q.answers) {
					answer_doc_not_founded++;
					total_answer_doc_not_founded++;
					q.answer_doc_not_founded.add(answer.get("slot_name"));
				}
				float coverage = ((float) answer_doc_founded / (float) (answer_doc_founded + answer_doc_not_founded));
				q.coverage = coverage;
				//System.out.println("\t\tcoverage: " + coverage );				
				continue;
			}
			
			for (HashMap<String, String> answer : q.answers) {
				
				//System.out.print(answer.get("slot_name") + '\t' + answer.get("docid"));
				boolean found = false;
				
				for (Document d : q.documents) {
					if (d.get("docid").equalsIgnoreCase(answer.get("docid"))) {
						//System.out.println("\tfound");
						found = true;
						answer_doc_founded++;
						total_answer_doc_founded++;
						q.answer_doc_founded.add(answer.get("slot_name"));
						break;
					}
				}
				
				if (!found) {
					//System.out.println("\tnot found");
					answer_doc_not_founded++;
					total_answer_doc_not_founded++;
				}					
			}
			//System.out.println("number of answers: " + Integer.toString(answer_doc_founded + answer_doc_not_founded));			
			float coverage = ((float) answer_doc_founded / (float) (answer_doc_founded + answer_doc_not_founded));
			q.coverage = coverage;
			//System.out.println("\t\tcoverage: " + coverage );			
			//System.out.println("number of docs retrieved: " + q.documents.size());
		}
		
		Set<String> answers_keys = answers_found.keySet();
		
		/*
		for (String s : answers_keys) {
			System.out.println(s + '\t' + answers_found.get(s).size());
		}
		
		// queries that did not had any document retrieved
		for (String a : answers_keys) {
			if (answers_found.get(a).size()>0)
				System.out.println("documents found for " + a + ':' + answers_found.get(a).size());
		}
		*/
		
		System.out.println("\nQueries with 0 docs retrieved: " + queries_with_zero_docs.size());
		
		/*
		for (SFQuery q : queries_with_zero_docs) {
			System.out.println(q.query_id + '\t' + q.name);
		}
		*/
		
		System.out.println("average coverage: " + ( (float) total_answer_doc_founded / (float) (total_answer_doc_founded + total_answer_doc_not_founded)) );		
	}
		
	public static void loadQueriesAnswers(String filename, Map<String, SFQuery> queries) throws IOException {
		
		System.out.println("Loading answers from: " + filename);			
		
		try {
			
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));			
			
			//first line is just the identifiers
			String strLine = br.readLine();
			strLine = br.readLine();
			
			String[] data = strLine.split("\t");
			String current_qid = data[1];

			SFQuery q = queries.get(current_qid);
			HashMap<String,String> answers = new HashMap<String, String>();			
			answers.put("slot_name", data[3]);
			answers.put("docid", data[4]);
			answers.put("start_char", data[5]);
			answers.put("end_char", data[6]);
			answers.put("response", data[7]);
			answers.put("norm_response", data[8]);
			answers.put("equiv_class_id", data[9]);
			answers.put("judgment", data[10]);
			q.answers.add(answers);
			
			while ((strLine = br.readLine()) != null)   {
				String previous_qid = current_qid;
				data = strLine.split("\t");
				current_qid = data[1];
				
				if (!current_qid.equalsIgnoreCase(previous_qid)) {					
					q = queries.get(current_qid);
					answers = new HashMap<String, String>();
					answers.put("slot_name", data[3]);
					answers.put("docid", data[4]);
					answers.put("start_char", data[5]);
					answers.put("end_char", data[6]);
					answers.put("response", data[7]);
					answers.put("norm_response", data[8]);
					answers.put("equiv_class_id", data[9]);
					answers.put("judgment", data[10]);
					q.answers.add(answers);
				}
				
				else {
					answers = new HashMap<String, String>();
					answers.put("slot_name", data[3]);
					answers.put("docid", data[4]);
					answers.put("start_char", data[5]);
					answers.put("end_char", data[6]);
					answers.put("response", data[7]);
					answers.put("norm_response", data[8]);
					answers.put("equiv_class_id", data[9]);
					answers.put("judgment", data[10]);
					q.answers.add(answers);
				}
			}
			in.close();
		}
		
		catch (Exception e)	{
				//Catch exception if any			
				System.err.println("Error: " + e.getMessage());
		}
		
	}
	
	public static String getAnswerDocument(String docid) throws IOException {
        Term t = new Term("docid", docid); 
        Query query = new TermQuery(t);                 
        TopDocs docs = Definitions.documents.search(query, 1);
        ScoreDoc[] scoredocs = docs.scoreDocs;
        Document doc = Definitions.documents.doc(scoredocs[0].doc);        
        return doc.get("text");
	}
	
	public static void run(CommandLine line) throws Exception {
		
		Definitions.loadDocumentCollecion();
		//Definitions.loadClassifier("/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz");
		Definitions.connectionREDIS();
		
		System.out.println(Definitions.jedis);
		
		Definitions.loadKBIndex();
		
		/* Load Train Queries + answers */
		String queriesTrainFile = line.getOptionValue("queriesTrain");
		System.out.println("\nLoading train queries from: " + queriesTrainFile);
		Map<String, SFQuery> train_queries = LoadQueries.loadXML(queriesTrainFile);		
		String queriesTrainAnswers = line.getOptionValue("queriesTrainAnswers");
		loadQueriesAnswers(queriesTrainAnswers,train_queries);
		
		System.out.println("Loaded: " + train_queries.size() + " train queries");
		
		/* Load Test Queries + answers */
		String queriesTestFile = line.getOptionValue("queriesTest");
		System.out.println("\nLoading test queries from: " + queriesTestFile);
		Map<String, SFQuery> test_queries = LoadQueries.loadXML(queriesTestFile);		
		String queriesTestAnswers = line.getOptionValue("queriesTestAnswers");
		loadQueriesAnswers(queriesTestAnswers,test_queries);
		
		System.out.println("Loaded: " + test_queries.size() + " test queries");
		
		/* For the Train Queries: 
		 * 		get document answer for each attribute;
		 * 		get sentence with answer;
		 * 		get answer/normalized answer
		 */
		
		Set<String> train_queries_keys = train_queries.keySet();
		
		for (String q_id : train_queries_keys) {
			
			SFQuery q = train_queries.get(q_id);			
			//System.out.println(q.name + '\t' + q.query_id);	
			
			for (HashMap<String, String> a : q.answers) {
				
				String slot_name = a.get("slot_name");
				String response = a.get("response");
				String norm_response = a.get("norm_response");
				String doc_id = a.get("docid");
				String start_char = a.get("start_char");
				String end_char = a.get("end_char");
				String judgment = a.get("judgment");
				
				//System.out.println(slot_name + '\t' + response + '\t' + doc_id);
					
				if (q.etype.equalsIgnoreCase("PER")) {		
					((PER_Attributes) q.attributes).attributes.get(slot_name).response.add(response);
					((PER_Attributes) q.attributes).attributes.get(slot_name).response_normalized.add(norm_response);
					((PER_Attributes) q.attributes).attributes.get(slot_name).slot_name = (slot_name);					
					((PER_Attributes) q.attributes).attributes.get(slot_name).answer_doc = getAnswerDocument(doc_id);
					((PER_Attributes) q.attributes).attributes.get(slot_name).start_char = Integer.parseInt(start_char);
					((PER_Attributes) q.attributes).attributes.get(slot_name).end_char = Integer.parseInt(end_char);
					((PER_Attributes) q.attributes).attributes.get(slot_name).judgment = Integer.parseInt(judgment);
				}
				
				else if (q.etype.equalsIgnoreCase("ORG")) {
					((ORG_Attributes) q.attributes).attributes.get(slot_name).response.add(response);
					((ORG_Attributes) q.attributes).attributes.get(slot_name).response_normalized.add(norm_response);
					((ORG_Attributes) q.attributes).attributes.get(slot_name).slot_name = (slot_name);
					((ORG_Attributes) q.attributes).attributes.get(slot_name).answer_doc = getAnswerDocument(doc_id);
					((ORG_Attributes) q.attributes).attributes.get(slot_name).start_char = Integer.parseInt(start_char);
					((ORG_Attributes) q.attributes).attributes.get(slot_name).end_char = Integer.parseInt(end_char);
					((ORG_Attributes) q.attributes).attributes.get(slot_name).judgment = Integer.parseInt(judgment);
				}
			}
		}
		

		//check that insertions were done correctly
		for (String s : train_queries_keys) {			
			
			SFQuery q = train_queries.get(s);
				
			if (q.etype.equalsIgnoreCase("PER")) {
				
				HashMap<String,Attribute> a = ((PER_Attributes) q.attributes).attributes;
				Set<String> keys = a.keySet();
				
				/*
				System.out.println(q.name + '\t' + q.query_id);
				System.out.println("attributes: " + keys.size());
				*/				
				
				for (String k : keys) {
					
					if (a.get(k).answer_doc!=null && a.get(k).judgment==1) {
						//System.out.println(k);
						//System.out.println('\t' + a.get(k).answer.get(0));
						a.get(k).extractSentences();
						//System.out.println('\t' + a.get(k).answer_doc);						
						//System.out.println('\t' + a.get(k).answer.size());
					}
				}
			}
			
			else if (q.etype.equalsIgnoreCase("ORG")) {
				HashMap<String,Attribute> a = ((ORG_Attributes) q.attributes).attributes;
				Set<String> keys = a.keySet();
				
				/*
				System.out.println(q.name + '\t' + q.query_id);
				System.out.println("attributes: " + keys.size());
				*/
				for (String k : keys) {
					if (a.get(k).answer_doc!=null && a.get(k).judgment==1) {
						//System.out.println(k);
						//System.out.println('\t' + a.get(k).answer.get(0));				
						a.get(k).extractSentences();
						//System.out.println('\t' + a.get(k).answer_doc);						
						//System.out.println('\t' + a.get(k).answer.size());
					}
				}
			}
		}

		/*
		System.out.println("train queries");
		
		Multimap<String, Pattern> patterns = PatternMatching.qaPairs(train_queries);
		PatternMatching.parsePatterns(patterns);
		*/
		
		parseQueries(test_queries);		
		parseQueries(train_queries);
		
		System.out.println("\n\n2010 queries");
		recall(train_queries);
		
		System.out.println("\n\n2011 queries");
		recall(test_queries);
		
		//printResults(test_queries);		
		//printResults(train_queries);
		
	}
	
	public static void extractRelations() {
		
		/* Retrieve all relations extracted by ReVerb for all the documents retrieved */
		
		/* Extract relations based on the slots that have to be filled, using the patterns mappings */
		
	}
	
	
	public static void printResults(Map<String, SFQuery> queries) throws Exception {
		Set<String> keys = queries.keySet();		
		for (String k : keys) {
			SFQuery q = queries.get(k);
			System.out.println(q.name + '\t' + q.documents.size() + '\t' + q.coverage);			
		}		
	}
}


