package tac.kbp.slotfilling.bin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.LinkedList;
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
import tac.kbp.slotfilling.patterns.slots.ORGSlots;
import tac.kbp.slotfilling.patterns.slots.PERSlots;
import tac.kbp.slotfilling.queries.AnswerGoldenStandard;
import tac.kbp.slotfilling.queries.LoadQueries;
import tac.kbp.slotfilling.queries.SFQuery;
import tac.kbp.slotfilling.queries.SystemAnswer;
import tac.kbp.slotfilling.relations.DocumentRelations;
import tac.kbp.slotfilling.relations.ReverbRelation;
import tac.kbp.utils.SHA1;

import com.mysql.jdbc.PreparedStatement;

public class Main {
	
	public static LinkedList<String> PER_attributes = new LinkedList<String>();
	public static LinkedList<String> ORG_attributes = new LinkedList<String>();
	
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
			
			q.queryCollection();
			
		}
		
	}
	
	public static void recall(Map<String, SFQuery> queries) {
				
		Set<SFQuery> queries_with_zero_docs = new HashSet<SFQuery>();		
		Set<String> keys = queries.keySet();		
		
		for (String k : keys) {			
			SFQuery q = queries.get(k);
			
			System.out.println('\n' + q.query_id + '\t' + q.name + '\t' + q.documents.size());
			
			if (q.documents.size()==0) {
				queries_with_zero_docs.add(q);
				Set<String> answers = q.correct_answers.keySet();				
				for (String slot_name : answers) {
					Set<AnswerGoldenStandard> answer = q.correct_answers.get(slot_name);
					for (AnswerGoldenStandard answerGoldenStandard : answer) {						
						if (!q.ignore.contains(answerGoldenStandard.slot_name)) {
							q.coverages.put(answerGoldenStandard.slot_name, new Float(0));
						}
					}
				}				
			}
			
			else {
				
				Set<String> answers = q.correct_answers.keySet();
				
				if (answers.size() == 0) {
					q.coverages = null;
				}
				
				for (String slot_name : answers) {
					Set<AnswerGoldenStandard> answer = q.correct_answers.get(slot_name);
					
					/* Gather all the docid which hold the answers */
					Set<String> docs_hold_answers = new HashSet<String>();
					for (AnswerGoldenStandard answerGoldenStandard : answer) {							
						docs_hold_answers.add(answerGoldenStandard.docid);
					}
					
					//System.out.println(slot_name + "\t #answers: "+answer.size() + "\t#docs with answers:"+docs_hold_answers.size());
					
					
					Set<String> docs_found = new HashSet<String>();
					/* Check if any of the retrieved docs is in the set */
					for (Document d : q.documents) {						
						for (AnswerGoldenStandard answerGoldenStandard : answer) {
							if (!q.ignore.contains(answerGoldenStandard.slot_name)) {								
								if (docs_hold_answers.contains(d.get("docid"))) {
									docs_found.add(d.get("docid"));
								}
							}
						}					
					}
					q.coverages.put(slot_name, (float) docs_found.size() / (float) docs_hold_answers.size());
					System.out.println(slot_name + '\t' + q.coverages.get(slot_name));					
				}				
			}
			
			if (q.query_id.equalsIgnoreCase("SF42")) {
				System.out.println();
			}
			
		}
		
		System.out.println("\nQueries with 0 docs retrieved: " + queries_with_zero_docs.size());
		
		float avg_coverage = 0;
		int no_answers = 0;
		
		for (String k : keys) {			
			SFQuery q = queries.get(k);
			
			if (q.coverages == null) {
				no_answers++;
			}
			
			else {
				
				Set<String> slots = q.coverages.keySet();
				for (String string : slots) {
					q.coverage += q.coverages.get(string);
				}
				
				q.coverage = q.coverage / q.coverages.size();
				//TODO: write this to an output file				
				//System.out.println(q.query_id + '\t' + q.name + '\t' +q.coverage);				
				avg_coverage += q.coverage;				
			}
		}
		
		System.out.println("Average coverage: " + (float) avg_coverage / (float) (queries.size()-no_answers));		
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
			
			AnswerGoldenStandard answer = new AnswerGoldenStandard();			
			answer.slot_name = data[3];
			answer.docid = data[4];
			answer.start_char = data[5];
			answer.end_char = data[6];
			answer.response = data[7];
			answer.norm_response = data[8];
			answer.equiv_class_id = data[9];
			answer.judgment = data[10];			
			q.correct_answers.put(data[3], answer);
			
			
			
			while ((strLine = br.readLine()) != null)   {
				String previous_qid = current_qid;
				data = strLine.split("\t");
				current_qid = data[1];
				
				if (!current_qid.equalsIgnoreCase(previous_qid)) {					
					q = queries.get(current_qid);
					answer = new AnswerGoldenStandard();			
					answer.slot_name = data[3];
					answer.docid = data[4];
					answer.start_char = data[5];
					answer.end_char = data[6];
					answer.response = data[7];
					answer.norm_response = data[8];
					answer.equiv_class_id = data[9];
					answer.judgment = data[10];			
					q.correct_answers.put(data[3], answer);
					
				}
				
				else {					
					answer = new AnswerGoldenStandard();
					answer.slot_name = data[3];
					answer.docid = data[4];
					answer.start_char = data[5];
					answer.end_char = data[6];
					answer.response = data[7];
					answer.norm_response = data[8];
					answer.equiv_class_id = data[9];
					answer.judgment = data[10];			
					q.correct_answers.put(data[3], answer);
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
	
	
	public static void loadAttributes() {
		
		PER_attributes.add("per:alternate_names");
		PER_attributes.add("per:date_of_birth");		
		PER_attributes.add("per:age");		
		PER_attributes.add("per:country_of_birth");		
		PER_attributes.add("per:stateorprovince_of_birth");		
		PER_attributes.add("per:city_of_birth");
		PER_attributes.add("per:origin");		
		PER_attributes.add("per:date_of_death");		
		PER_attributes.add("per:country_of_death");		
		PER_attributes.add("per:stateorprovince_of_death");		
		PER_attributes.add("per:city_of_death");
		PER_attributes.add("per:cause_of_death");		
		PER_attributes.add("per:countries_of_residence");
		PER_attributes.add("per:stateorprovinces_of_residence");
		PER_attributes.add("per:cities_of_residence");
		PER_attributes.add("per:schools_attended");		
		PER_attributes.add("per:title");		
		PER_attributes.add("per:member_of");		
		PER_attributes.add("per:employee_of");		
		PER_attributes.add("per:religion");		
		PER_attributes.add("per:spouse");		
		PER_attributes.add("per:children");		
		PER_attributes.add("per:parents");		
		PER_attributes.add("per:siblings");		
		PER_attributes.add("per:other_family");
		PER_attributes.add("per:charges");
		
		ORG_attributes.add("org:alternate_names");
		ORG_attributes.add("org:political_religious_affiliation");
		ORG_attributes.add("org:top_members_employees");		
		ORG_attributes.add("org:number_of_employees_members");
		ORG_attributes.add("org:members");
		ORG_attributes.add("org:member_of");
		ORG_attributes.add("org:subsidiaries");
		ORG_attributes.add("org:parents");
		ORG_attributes.add("org:founded_by");
		ORG_attributes.add("org:founded");
		ORG_attributes.add("org:dissolved");
		ORG_attributes.add("org:country_of_headquarters");
		ORG_attributes.add("org:city_of_headquarters");
		ORG_attributes.add("org:shareholders");
		ORG_attributes.add("org:website");	
	}
	
	
	public static void run(CommandLine line) throws Exception {
		
		Definitions.loadDocumentCollecion();
		//Definitions.loadClassifier("/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz");
		Definitions.connectionREDIS();
		Definitions.loadKBIndex();
		Definitions.getDBConnection();
		loadAttributes();
		
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
		
		/*
		 * Load patterns to extract slot answers
		 */
		
		PERSlots.load_patterns();
		ORGSlots.load_patterns();
		
		System.out.println("Loaded: " + test_queries.size() + " test queries");
		
		/* For the Train Queries: 
		 * 		get document answer for each attribute;
		 * 		get sentence with answer;
		 * 		get answer/normalized answer
		
		Set<String> train_queries_keys = train_queries.keySet();
		
		for (String q_id : train_queries_keys) {
			
			SFQuery q = train_queries.get(q_id);			
			//System.out.println(q.name + '\t' + q.query_id);	
			
			for (HashMap<String, String> a : q.correct_answers) {
				
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
		*/
		
		parseQueries(train_queries);
		
		//System.out.println("\n\n2010 queries");
		//recall(train_queries);
		//System.out.println();
		
		selectExtractions(train_queries);		
		evaluation(train_queries);
		
		//outputresults(train_queries);
		Definitions.closeDBConnection();
	}
	
	
	public static DocumentRelations getExtractions(String docid, SFQuery q) throws Exception {
		
		String filename_sha1 = SHA1.digest(docid);
		
		PreparedStatement stm = (PreparedStatement) Definitions.connection.prepareStatement(
				
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction1 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction2 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction3 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction4 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction5 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction6 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction7 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence FROM extraction8 WHERE file_name_sha1 = ? " +
			"ORDER BY confidence DESC"			
			);
		
		stm.setString(1, filename_sha1);
		stm.setString(2, filename_sha1);
		stm.setString(3, filename_sha1);
		stm.setString(4, filename_sha1);
		stm.setString(5, filename_sha1);
		stm.setString(6, filename_sha1);
		stm.setString(7, filename_sha1);
		stm.setString(8, filename_sha1);
		
		ResultSet resultSet = stm.executeQuery();
		LinkedList<ReverbRelation> relations = new LinkedList<ReverbRelation>();
		
		while (resultSet.next()) {
			String arg1 = resultSet.getString(1);			
			String rel = resultSet.getString(2);
			String arg2 = resultSet.getString(3);
			Float confidence = resultSet.getFloat(4);
			String sentence = resultSet.getString(5);			
			ReverbRelation relation = new ReverbRelation(docid, arg1, rel, arg2, sentence, confidence);		
			relations.add(relation);
		}
		
		DocumentRelations doc = new DocumentRelations(relations, docid);
		
		return doc;
	}
	
	
	public static void evaluation(Map<String, SFQuery> queries) {
		
		int correct_slots = 0;
		//int wrong_slots = 0;
		
		Set<String> keys = queries.keySet();
		
		for (String keyQ : keys) {
			
			SFQuery q = queries.get(keyQ);
			System.out.println('\n' + q.query_id + '\t' + q.name + '\t' + q.coverage + '\t' + q.documents.size());						
			Set<String> answers = q.system_answers.keySet();
			
			for (String a : answers) {				
				Set<SystemAnswer> answersSet = q.system_answers.get(a);
				
				if (answersSet.size()>0) {
					System.out.println("slot name: " + a + '\t' + answersSet.size());
					for (SystemAnswer systemAnswer : answersSet) {						
						System.out.println("\tsystem answer: " + '\t' + systemAnswer.slot_filler);
					}
				}				
				Set<AnswerGoldenStandard> correctAnswers = q.correct_answers.get(a);
				
				if (correctAnswers.size()>0) {				
					for (AnswerGoldenStandard correctAnswer : correctAnswers) {						
						System.out.println("\tcorrect answer: " + '\t' + correctAnswer.response);
					}
				}				
			}
		}
	}

	
	public static void selectExtractions(Map<String, SFQuery> queries) throws Exception {
		
		Set<String> keys = queries.keySet();		
		
		for (String keyQ : keys) {			
			SFQuery q = queries.get(keyQ);
			
			System.out.println("query: " + q.name);
			System.out.println("support document: " + q.docid);
			System.out.println();
			
			/* Retrieve all relations extracted by ReVerb for each document retrieved and
			/* extract relations based on the slots that have to be filled, using the patterns mappings */
			
			for (Document d : q.documents) {			
				DocumentRelations relations = getExtractions(d.get("docid"),q);				
				if (q.etype.equalsIgnoreCase("PER")) {					
					for (ReverbRelation relation : relations.relations) {						
						for (String attribute: PER_attributes) {							
							if (!q.ignore.contains(attribute)) {
								
								/* place_of_residence */
								if (attribute.equalsIgnoreCase("per:countries_of_residence") || attribute.equalsIgnoreCase("per:stateorprovinces_of_residence") || attribute.equalsIgnoreCase("per:cities_of_residence")) {									
									LinkedList<String> patterns = PERSlots.slots_patterns.get("per:place_of_residence");									
									for (String pattern : patterns) {
										if (relation.rel.equalsIgnoreCase(pattern)) {
											SystemAnswer answer = new SystemAnswer();
											answer.confidence_score = relation.confidence;
											answer.justify = d.get("docid");
											//TODO: determine if place is country, state or city
											answer.slot_name = "per:place_of_residence";											
											answer.slot_filler = relation.arg1;
											//TODO: get the offsets of sentence and args
											answer.start_offset_filler = 0;
											answer.end_offset_filler = 0;
											answer.start_offset_justification = 0;
											answer.end_offset_justification = 0;											
											q.system_answers.put("per:place_of_residence", answer);
										}
									}
								}
								
								/* place_of_death */									
								else if (attribute.equalsIgnoreCase("per:city_of_death") || attribute.equalsIgnoreCase("per:stateorprovince_of_death") || attribute.equalsIgnoreCase("per:country_of_death")) {									
									LinkedList<String> patterns = PERSlots.slots_patterns.get("per:place_of_death");
									for (String pattern : patterns) {
										if (relation.rel.equalsIgnoreCase(pattern)) {
											SystemAnswer answer = new SystemAnswer();
											answer.confidence_score = relation.confidence;
											answer.justify = d.get("docid");
											//TODO: determine if place is country, state or city
											answer.slot_name = "per:place_of_death";											
											answer.slot_filler = relation.arg1;
											//TODO: get the offsets of sentence and args
											answer.start_offset_filler = 0;
											answer.end_offset_filler = 0;
											answer.start_offset_justification = 0;
											answer.end_offset_justification = 0;											
											q.system_answers.put("per:place_of_death", answer);
										}
									}
								}
								/* place_of_birth */
								else if (attribute.equalsIgnoreCase("per:country_of_birth") || attribute.equalsIgnoreCase("per:stateorprovince_of_birth") || attribute.equalsIgnoreCase("per:city_of_birth")) {									
									LinkedList<String> patterns = PERSlots.slots_patterns.get("per:place_of_birth");
									for (String pattern : patterns) {										
										if (relation.rel.equalsIgnoreCase(pattern)) {
											SystemAnswer answer = new SystemAnswer();
											answer.confidence_score = relation.confidence;
											answer.justify = d.get("docid");
											//TODO: determine if place is country, state or city
											answer.slot_name = "per:place_of_birth";											
											answer.slot_filler = relation.arg1;
											//TODO: get the offsets of sentence and args
											answer.start_offset_filler = 0;
											answer.end_offset_filler = 0;
											answer.start_offset_justification = 0;
											answer.end_offset_justification = 0;											
											q.system_answers.put("per:place_of_birth", answer);
										}
									}
								}
								
								else {
									/* other patterns */
									LinkedList<String> patterns = PERSlots.slots_patterns.get(attribute);
									for (String pattern : patterns) {
										if (relation.rel.equalsIgnoreCase(pattern)) {
											System.out.println("MATCH!");
											SystemAnswer answer = new SystemAnswer();
											answer.confidence_score = relation.confidence;
											answer.justify = d.get("docid");
											answer.slot_name = attribute;											
											answer.slot_filler = relation.arg1;
											//TODO: get the offsets of sentence and args
											answer.start_offset_filler = 0;
											answer.end_offset_filler = 0;
											answer.start_offset_justification = 0;
											answer.end_offset_justification = 0;											
											q.system_answers.put(attribute, answer);
										}
									}
									
								}								
							}
						}
					}
					
				}
			
				else if (q.etype.equalsIgnoreCase("ORG"))  {
				
				}
			}			
		}		
	}
		
	/*
	public static void outputresults(Map<String, SFQuery> queries) throws IOException {
		
		int unique_run_id = 0;		
		FileWriter output = new FileWriter("results.txt"); 
		Set<String> keys = queries.keySet();
		
		for (String keyQ : keys) {			
			SFQuery q = queries.get(keyQ);
			
			for (HashMap<String, String> answer : q.system_answers) {
				
				output.write(q.query_id + '\t' + answer.get("slot_name") + '\t' + Integer.toString(unique_run_id) + '\t' + answer.get("justify") + 
						'\t' + answer.get("slot_filler") + '\t' + answer.get("start_offset_filler") + '\t' + answer.get("end_offset_filler") + '\t'
						+ answer.get("start_offset_justification") + '\t' + answer.get("end_offset_justification") + '\t' + 
						answer.get("confidence_score") + '\n');
			}
		}
		
		output.close();
	}
	*/
	
	public static void printResults(Map<String, SFQuery> queries) throws Exception {
		//TODO: for which slots were the documents with the answer retrieved and for which slots were not
	}
}





