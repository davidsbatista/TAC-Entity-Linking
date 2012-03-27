package tac.kbp.slotfilling.bin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

import tac.kbp.configuration.Definitions;
import tac.kbp.slotfilling.queries.LoadQueries;
import tac.kbp.slotfilling.queries.SFQuery;
import tac.kbp.slotfilling.queries.attributes.Attribute;
import tac.kbp.slotfilling.queries.attributes.Attributes;
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
			
			System.out.println("q_id: " + q.query_id);
			System.out.println("name: " + q.name);	
			System.out.println("ignore:" + q.ignore);			
			q.getSupportDocument();
			
			// get nodeid from KB: wiki_text, other attributes
			if (!q.nodeid.startsWith("NIL"))
				q.getKBEntry();
			
			// get sentences where entity occurs
			q.extractSentences();
			System.out.println("sentences: " + q.sentences.size());
			
			// extract other entities in the support document
			q.getNamedEntities();			
			System.out.println("persons: " + q.persons.size());
			System.out.println("places: " + q.places.size());
			System.out.println("org: " + q.organizations.size());
					
			// get alternative senses for entity name and extract acronyms from support doc.
			q.getAlternativeSenses();
			q.extracAcronyms();
			System.out.println("senses: " + q.alternative_names);			
			System.out.println("abbreviations: " + q.abbreviations);

			System.out.println("attributes to be filled:");
			if (q.etype.equalsIgnoreCase("PER")) {
				PER_Attributes attributes = (PER_Attributes) q.attributes;
				System.out.println(attributes.toString());
				
			}
			else if (q.etype.equalsIgnoreCase("ORG")) {
				ORG_Attributes attributes = (ORG_Attributes) q.attributes;
				System.out.println(attributes.toString());
			}
			
			q.queryCollection();
			
		}
		
	}
	
	public static void recall(Map<String, SFQuery> queries) {
				
		Map<String, List<SFQuery>> answers_found = new HashMap<String, List<SFQuery>>();
		Set<SFQuery> queries_with_zero_docs = new HashSet<SFQuery>();
		
		Set<String> keys = queries.keySet();
		
		for (String k : keys) {
			SFQuery q = queries.get(k);
			
			if (q.documents.size()==0) {
				queries_with_zero_docs.add(q);
				continue;
			}
			
			for (Document d : q.documents) {
				String docid = d.get("docid");				
				for (HashMap<String, String> answer : q.answers) {
					if (docid.equalsIgnoreCase(answer.get("answer_doc"))) {
						
						List<SFQuery> list = answers_found.get(answer.get("slot_name"));
												
						if (list==null)
							list = new LinkedList<SFQuery>();
						
						list.add(q);						
						answers_found.put(answer.get("slot_name"), list);
					}
				}
			}
		}
		
		Set<String> answers_keys = answers_found.keySet();
		
		for (String s : answers_keys) {
			System.out.println(s + '\t' + answers_found.get(s).size());
		}
		
		//for (String a : answers_keys)
		//	System.out.println(a);
		
		/*
		for (String a : answers_keys) {
			if (answers_found.get(a).size()>0)
				System.out.println("documents found for " + a + ':' + answers_found.get(a).size());
		}
		*/
		
		//TODO: para cada query mostrar: lista de attributos em que o doc com resposta foi encontrado e os que nao foi
		
		/*
		for (String string : answers_keys) {
			
		}
		*/
		
		System.out.println("\nQueries with 0 docs retrieved: " + queries_with_zero_docs.size());
		
		for (SFQuery q : queries_with_zero_docs) {
			System.out.println(q.query_id + '\t' + q.name);
		}
		
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
	
	public static void run(CommandLine line) throws Exception {
		
		Definitions.loadDocumentCollecion();
		Definitions.loadClassifier("/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz");
		Definitions.connectionREDIS();
		Definitions.loadKBIndex();
		
		/* Load Test Queries + answers */
		String queriesTestFile = line.getOptionValue("queriesTest");
		System.out.println("\nLoading test queries from: " + queriesTestFile);
		Map<String, SFQuery> test_queries = LoadQueries.loadXML(queriesTestFile);		
		String queriesTestAnswers = line.getOptionValue("queriesTestAnswers");
		loadQueriesAnswers(queriesTestAnswers,test_queries);
		
		/* Load Train Queries + answers */
		String queriesTrainFile = line.getOptionValue("queriesTrain");
		System.out.println("\nLoading train queries from: " + queriesTrainFile);
		Map<String, SFQuery> train_queries = LoadQueries.loadXML(queriesTrainFile);		
		String queriesTrainAnswers = line.getOptionValue("queriesTrainAnswers");
		loadQueriesAnswers(queriesTrainAnswers,train_queries);
		
		
		/* For the Test Queries: 
		 * 		get document answer for each attribute;
		 * 		get sentence with answer; 
		 * 		get answer/normalized answer
		 */
		Set<String> train_queries_keys = train_queries.keySet();
		
		for (String q_id : train_queries_keys) {
			
			Attribute attr = null;
			
			SFQuery q = train_queries.get(q_id);
			
				for (HashMap<String, String> a : q.answers) {
					
					String slot_name = a.get("slot_name");
					String response = a.get("response");
					String doc_id = a.get("docid");
					
					if (q.etype.equalsIgnoreCase("PER")) {
						attr = ((PER_Attributes) q.attributes).attributes.get(slot_name);
					}					
					else if (q.etype.equalsIgnoreCase("ORG")) {
						attr = ((ORG_Attributes) q.attributes).attributes.get(slot_name);
					}
					
					attr.slot_name = slot_name;
					attr.answer.add(response);
					attr.doc_id = attr.getAnswerDocument(doc_id);
					
				}
		}
			
		//check that insertions were done correctly
		for (String s : train_queries_keys) {			
			
			SFQuery q = train_queries.get(s);
				
			if (q.etype.equalsIgnoreCase("PER")) {
				HashMap<String,Attribute> a = ((PER_Attributes) q.attributes).attributes;
				Set<String> keys = a.keySet();
				
				System.out.println(q.name + '\t' + q.query_id);
				System.out.println("attributes: " + keys.size());				
				for (String k : keys) {
					System.out.println(k + '\t' + a.get(k).slot_name);
					System.out.println(k + '\t' + a.get(k).supportDocument);
					System.out.println(k + '\t' + a.get(k).answer);
				}
			}
			
			else if (q.etype.equalsIgnoreCase("ORG")) {
				HashMap<String,Attribute> a = ((ORG_Attributes) q.attributes).attributes;
				Set<String> keys = a.keySet();
				
				System.out.println(q.name + '\t' + q.query_id);
				System.out.println("attributes: " + keys.size());
				for (String k : keys) {
					System.out.println(k + '\t' + a.get(k).slot_name);
					System.out.println(k + '\t' + a.get(k).supportDocument);
					System.out.println(k + '\t' + a.get(k).answer);
				}
			}
			
			System.out.println();
		}	
	}
		//parseQueries(test_queries);
		//parseQueries(train_queries);
		//recall(test_queries);
}		


	
	
	
	
	
	
	
	
	
	