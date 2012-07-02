package tac.kbp.slotfilling.bin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
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

import com.google.common.collect.Multiset;
import com.mysql.jdbc.PreparedStatement;

public class Main {
	
	public static LinkedList<String> PER_attributes = new LinkedList<String>();
	public static LinkedList<String> ORG_attributes = new LinkedList<String>();
	public static int total_n_correct_slots = 0;
	
	public static void main(String[] args) throws Exception {
		
		// create Options object
		Options options = new Options();

		// add options		
		options.addOption("run", false, "complete run");
		
		// add argument options
		Option queries = OptionBuilder.withArgName("queries").hasArg().withDescription("XML file containing queries for trainning").create("queries");		
		Option answers = OptionBuilder.withArgName("answers").hasArg().withDescription("queries answers").create("answers");
		Option lists   = OptionBuilder.withArgName("lists").hasArg().withDescription("lists of facts and entities").create("lists");
		Option max_docs   = OptionBuilder.withArgName("max_docs").hasArg().withDescription("lists of facts and entities").create("max_docs");
						
		options.addOption(queries);
		options.addOption(answers);
		options.addOption(lists);
		options.addOption(max_docs);
				
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
	
	public static void statistics(Map<String, SFQuery> queries) {
				
		Set<SFQuery> queries_with_zero_docs = new HashSet<SFQuery>();		
		Set<String> keys = queries.keySet();
		
		int total_n_docs = 0;
		int total_n_relations = 0;
		
		for (String k : keys) {			
			SFQuery q = queries.get(k);
			total_n_docs += q.documents.size();
			
			for (DocumentRelations docRel : q.relations) {
				total_n_relations += docRel.relations.size();
			}			 
			
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
				}				
			}
		}
		
				
		
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
				avg_coverage += q.coverage;				
			}
		}
		
		System.out.println("Queries with 0 docs retrieved: " + queries_with_zero_docs.size());
		System.out.println("Average docs p/ query: " + (float) total_n_docs / (float) queries.size());
		System.out.println("Average coverage: " + (float) avg_coverage / (float) (queries.size()-no_answers));		
		System.out.println("Average relations p/ query: " + (float) (float) total_n_relations / (float) queries.size());
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
		Definitions.loadClassifier("/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz");
		Definitions.connectionREDIS();
		Definitions.loadKBIndex();
		Definitions.getDBConnection();
		Definitions.loadLists(line.getOptionValue("lists"));
		Definitions.oneAnswersSlots();
		Definitions.max_docs = Integer.parseInt(line.getOptionValue("max_docs"));
		loadAttributes();
		
		/* Load queries + answers */
		String queriesPath = line.getOptionValue("queries");
		System.out.println("\nLoading train queries from: " + queriesPath);
		Map<String, SFQuery> queries = LoadQueries.loadXML(queriesPath);
		String answers = line.getOptionValue("answers");
		loadQueriesAnswers(answers,queries);		
		System.out.println("Loaded: " + queries.size() + " queries");
		
		/* Load patterns to extract slot answers */	
		PERSlots.load_patterns();
		ORGSlots.load_patterns();
		
		/* parse the queries: alternative senses + extract documents */
		System.out.println("\nParsing queries...\n");
		parseQueries(queries);		
		
		/* extract relations from documents */
		retrieveRelations(queries);
		
		/* select relations based on pattern matching */
		matchPatternExtractions(queries);
		
		/* select best answers for each slot */
		selectExtractions(queries);
		
		/* calculate coverage, avg docs per query, avg relations per query */				
		statistics(queries);
		
		/* calculates precision for each query */
		precision(queries);
		
		/* write results file */
		outputresults(queries);
		
		System.out.println("Total number of correct slots: " + total_n_correct_slots);
		
		Definitions.closeDBConnection();
	}
	
	public static void extractGoldenAnswers(Map<String, SFQuery> queries) {
		
		/* For the Train Queries: 
		 * 		get document answer for each attribute;
		 * 		get sentence with answer;
		 * 		get answer/normalized answer

		
		Set<String> train_queries_keys = queries.keySet();
		
		for (String q_id : train_queries_keys) {
			
			SFQuery q = queries.get(q_id);			
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
			
			SFQuery q = queries.get(s);
				
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
	}
	
	public static DocumentRelations getExtractions(String docid, SFQuery q) throws Exception {
		
		String filename_sha1 = SHA1.digest(docid);
		
		PreparedStatement stm = (PreparedStatement) Definitions.connection.prepareStatement(
				
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction1 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction2 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction3 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction4 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction5 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction6 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction7 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction8 WHERE file_name_sha1 = ? UNION " +
			"SELECT arg1, rel, arg2, confidence, sentence, filename, sentence_number FROM extraction9 WHERE file_name_sha1 = ? " +
			"ORDER BY filename,sentence_number ASC"			
			);
		
		stm.setString(1, filename_sha1);
		stm.setString(2, filename_sha1);
		stm.setString(3, filename_sha1);
		stm.setString(4, filename_sha1);
		stm.setString(5, filename_sha1);
		stm.setString(6, filename_sha1);
		stm.setString(7, filename_sha1);
		stm.setString(8, filename_sha1);
		stm.setString(9, filename_sha1);
		
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
	
	public static void retrieveRelations(Map<String, SFQuery> queries) throws Exception {

		System.out.println("\nRetrieving relations...\n");
		
		Set<String> keys = queries.keySet();		
		
		for (String keyQ : keys) {			
			SFQuery q = queries.get(keyQ);
			
			System.out.print(q.query_id + '\t'+ q.etype + '\t' + q.name + "\t" + q.documents.size());
			
			int total_relations = 0;
			
			/* Retrieve all relations extracted by ReVerb for each document */ 
			for (Document d : q.documents) {			
				DocumentRelations relations = getExtractions(d.get("docid"),q);
				total_relations += relations.relations.size();
				q.relations.add(relations);
			}
			q.TAGRelations();			
			System.out.println("\t" + total_relations );
		}
	}
	
	public static void printCorrectAnswers(SFQuery q) {
		
		/* print correct answers for each slot */
		Set<String> answers = q.correct_answers.keySet();			
		System.out.println("\ncorrect answers: ");			
		for (String a : answers) {				
			Set<AnswerGoldenStandard> correctAnswers = q.correct_answers.get(a);								
			if (correctAnswers.size()>0) {					
				for (AnswerGoldenStandard correctAnswer : correctAnswers) {						
					System.out.println(correctAnswer.slot_name + "\t" + correctAnswer.response + "\t" + correctAnswer.docid);
				}
			}
		}		
	}
	
	public static void printSystemAnswers(SFQuery q) {

		System.out.println("\nsystem answers: ");
		
		Set<String> slotsKeys = q.selected_answers.keySet();
		
		System.out.println("selected answers: " + slotsKeys.size());
		
		for (String slotKey : slotsKeys) {
			SystemAnswer answer = q.selected_answers.get(slotKey);
			System.out.println(answer.slot_name + "\t" + answer.slot_filler + "\t" + answer.justify);		
		}
		
		slotsKeys = q.system_answers.keySet();
		
		for (String slotKey : slotsKeys) {
			if (!Definitions.one_answer_only.contains(slotKey)) {				
				Set<SystemAnswer> systemAnswers = q.system_answers.get(slotKey);
				
				System.out.println("system_answers: " + systemAnswers.size());
				
				for (SystemAnswer answer : systemAnswers)				
						System.out.println(answer.slot_name + "\t" + answer.slot_filler + "\t" + answer.justify);				
			}
		}
	}
		
	public static void printSlotsIgnore(SFQuery q) {
		
		System.out.println("\nslots to ignore: " + q.ignore.size());
		
		for (String slot : q.ignore) {
			System.out.println(slot);
		}
		
	}
	
	public static void printRelationsExtracted(SFQuery q) {
		
		for (DocumentRelations doc : q.relations) {			
			if (doc.relations.size()>0) {
				System.out.println("doc: " + doc.docid);
				for (ReverbRelation relation : doc.relations)
					System.out.println('\t' + relation.arg1_tagged + '\t' + relation.rel + '\t' + relation.arg2_tagged);
			}
		}
		
	}
	
	public static void precision(Map<String, SFQuery> queries) {

		Set<String> keys = queries.keySet();
		
		for (String keyQ : keys) {
			
			SFQuery q = queries.get(keyQ);
			//System.out.println("===============================================================");
			System.out.print(q.query_id + '\t' + q.etype + '\t' + q.name + "\tcoverage:" + q.coverage + "\t#docs:" + q.documents.size());			
			
			/*
			 * Correct 		= number of non-NIL output slots judged correct
			 * System 		= number of non-NIL output slots
			 * Reference 	= number of single-valued slots with a correct non-NIL response + plus number of equivalence classes for all list-valued slots
			 * 
			 * Recall 		= Correct / Reference
			 * Precision 	= Correct / System
			 *
			 */
			
			int correct = 0;
			int system = 0;
			int reference = 0;
			
			Set<String> slotsWithAnswer = q.correct_answers.keySet();
			
			for (String slot : slotsWithAnswer) {
				
				Set<AnswerGoldenStandard> correctAnswers = q.correct_answers.get(slot);				
				
				for (AnswerGoldenStandard correctAnswer : correctAnswers) {				
					if (Definitions.one_answer_only.contains(slot)) {						
						try {
							system += 1;
							if (q.selected_answers.get(slot).slot_filler.equalsIgnoreCase(correctAnswer.response) || q.selected_answers.get(slot).slot_filler.equalsIgnoreCase(correctAnswer.norm_response)) {
								correct += 1;
								continue;
							}								
						} 						
						catch (Exception e) {						
						}
					}					
					else {
						Set<SystemAnswer> systemAnswers;
						try {						
							systemAnswers = q.system_answers.get(slot);
							for (SystemAnswer systemAnswer : systemAnswers) {
								if ( correctAnswer.response.equalsIgnoreCase(systemAnswer.slot_filler) || correctAnswer.norm_response.equalsIgnoreCase(systemAnswer.slot_filler) )
									correct += 1;
								}
						}							
						catch (Exception e) {
						}
					}
				}
			}
			
			/* counts total number of NON-NIL answers */
			Multiset<String> slots = q.system_answers.keys();
			
			for (String slot : slots) {
				if (!Definitions.one_answer_only.contains(slot)) {
					//System.out.println(slot + '\t' + q.system_answers.get(slot).size());
					system += q.system_answers.get(slot).size();
				}
			}
						
			System.out.println("\t correct slots: " + correct + "\ttotal slots: " + system + '\t' + "precision: " + (float) correct / (float) system );
			
			total_n_correct_slots += correct;
			//printCorrectAnswers(q);			
			//printSystemAnswers(q);
			//printRelationsExtracted(q);
		}
		
		
	}
	
	public static boolean inList(String arg2, LinkedList<String> list) {
		return list.contains(arg2); 
	}
	
	public static void matchPERPatterns(SFQuery q) {

		for (DocumentRelations docRelations : q.relations) {
				
				for (ReverbRelation relation : docRelations.relations) {
					
					for (String attribute: PER_attributes) {
						
						if (!q.ignore.contains(attribute)) {
							
							/* place_of_residence */ 
							if (attribute.equalsIgnoreCase("per:countries_of_residence") || attribute.equalsIgnoreCase("per:stateorprovinces_of_residence") || attribute.equalsIgnoreCase("per:cities_of_residence")) {									
								LinkedList<String> patterns = PERSlots.slots_patterns.get("per:place_of_residence");									
								for (String pattern : patterns) {
									
									if (relation.rel.matches(pattern)) {
										
										if (relation.arg2_tagged.contains("LOCATION")) {
										
											SystemAnswer answer = new SystemAnswer();
											answer.confidence_score = relation.confidence;
											answer.justify = docRelations.docid;
											answer.slot_name = "per:place_of_residence";											
											answer.slot_filler = relation.arg2;											
											answer.start_offset_filler = 0;
											answer.end_offset_filler = 0;
											answer.start_offset_justification = 0;
											answer.end_offset_justification = 0;
											answer.relation = relation;
											q.system_answers.put("per:place_of_residence", answer);
										}
									}							
								}
							}
						
						/* place_of_death */						
						else if (attribute.equalsIgnoreCase("per:city_of_death") || attribute.equalsIgnoreCase("per:stateorprovince_of_death") || attribute.equalsIgnoreCase("per:country_of_death")) {									
							LinkedList<String> patterns = PERSlots.slots_patterns.get("per:place_of_death");
							for (String pattern : patterns) {
								
								if (relation.rel.matches(pattern)) {
									
									if (relation.arg2_tagged.contains("LOCATION")) {
									
										SystemAnswer answer = new SystemAnswer();
										answer.confidence_score = relation.confidence;
										answer.justify = docRelations.docid;
										answer.slot_name = "per:place_of_death";											
										answer.slot_filler = relation.arg2;
										answer.start_offset_filler = 0;
										answer.end_offset_filler = 0;
										answer.start_offset_justification = 0;
										answer.end_offset_justification = 0;
										answer.relation = relation;
										q.system_answers.put("per:place_of_death", answer);
									}
								}
							}
						}
							
						/* place_of_birth */
						else if (attribute.equalsIgnoreCase("per:country_of_birth") || attribute.equalsIgnoreCase("per:stateorprovince_of_birth") || attribute.equalsIgnoreCase("per:city_of_birth")) {									
							LinkedList<String> patterns = PERSlots.slots_patterns.get("per:place_of_birth");
							for (String pattern : patterns) {
								if (relation.rel.matches(pattern)) {	
									
									if (relation.arg2_tagged.contains("LOCATION")) {
										
										SystemAnswer answer = new SystemAnswer();
										answer.confidence_score = relation.confidence;
										answer.justify = docRelations.docid;
										answer.slot_name = "per:place_of_birth";											
										answer.slot_filler = relation.arg1;
										answer.start_offset_filler = 0;
										answer.end_offset_filler = 0;
										answer.start_offset_justification = 0;
										answer.end_offset_justification = 0;
										answer.relation = relation;
										q.system_answers.put("per:place_of_birth", answer);
									}
								}
							}
						}
						
						else {
							/* other patterns */
							LinkedList<String> patterns = PERSlots.slots_patterns.get(attribute);
							for (String pattern : patterns) {									
								if (relation.rel.matches(pattern)) {
									
									SystemAnswer answer = new SystemAnswer();
									
									if ( attribute.equalsIgnoreCase("per:date_of_birth") && relation.arg2_tagged.contains("DATE") 	||
										 attribute.equalsIgnoreCase("per:date_of_death") && relation.arg2_tagged.contains("DATE") 	||
										 attribute.equalsIgnoreCase("per:origin") && relation.arg2_tagged.contains("LOCATION") 		||
										 attribute.equalsIgnoreCase("per:spouse") && relation.arg2_tagged.contains("PERSON") 		||
										 attribute.equalsIgnoreCase("per:other_family") && relation.arg2_tagged.contains("PERSON") 	||
										 attribute.equalsIgnoreCase("per:siblings") && relation.arg2_tagged.contains("PERSON") 		||
										 attribute.equalsIgnoreCase("per:children") && relation.arg2_tagged.contains("PERSON") 		||
										 attribute.equalsIgnoreCase("per:parents") && relation.arg2_tagged.contains("PERSON") 		||
										 attribute.equalsIgnoreCase("per:age") && relation.arg2_tagged.contains("NUMBER") 			||
										 (attribute.equalsIgnoreCase("per:schools_attended") && (relation.arg2_tagged.contains("ORGANIZATION") || inList(relation.arg2.toLowerCase(),Definitions.lists_of_answers.get("list_of_schools.txt")))) ||
										 (attribute.equalsIgnoreCase("per:member_of") && (relation.arg2_tagged.contains("ORGANIZATION") || inList(relation.arg2.toLowerCase(),Definitions.lists_of_answers.get("list_of_companies.txt")))) || 
										 (attribute.equalsIgnoreCase("per:employ_of") && (relation.arg2_tagged.contains("ORGANIZATION") || inList(relation.arg2.toLowerCase(),Definitions.lists_of_answers.get("list_of_companies.txt"))))											 

									) {										
											answer.confidence_score = relation.confidence;
											answer.justify = docRelations.docid;
											answer.slot_name = attribute;											
											answer.slot_filler = relation.arg2;
											answer.start_offset_filler = 0;
											answer.end_offset_filler = 0;
											answer.start_offset_justification = 0;
											answer.end_offset_justification = 0;
											answer.relation = relation;											
											q.system_answers.put(attribute, answer);
										}
									
									else {
										
										 //attribute.equalsIgnoreCase("per:alternate_names")	PERSON										 
										 //attribute.equalsIgnoreCase("per:charges")			list_of_charges
										 //attribute.equalsIgnoreCase("per:religion")			list of religions
										 //attribute.equalsIgnoreCase("per:title")				list of positions
										
										answer.confidence_score = relation.confidence;
										answer.justify = docRelations.docid;
										answer.slot_name = attribute;											
										answer.slot_filler = relation.arg2;
										answer.start_offset_filler = 0;
										answer.end_offset_filler = 0;
										answer.start_offset_justification = 0;
										answer.end_offset_justification = 0;
										answer.relation = relation;											
										q.system_answers.put(attribute, answer);
										
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static void matchORGPatterns(SFQuery q) {
		
		for (DocumentRelations docRelations : q.relations) {
			
			for (ReverbRelation relation : docRelations.relations) {
				
				for (String attribute: ORG_attributes) {
					
					if (!q.ignore.contains(attribute)) {
						
						/* place_of_headquarters */ 
						if (attribute.equalsIgnoreCase("org:country_of_headquarters") || attribute.equalsIgnoreCase("org:stateorprovince_of_headquarters") || attribute.equalsIgnoreCase("org:city_of_headquarters")) {
							LinkedList<String> patterns = ORGSlots.slots_patterns.get("org:place_of_headquarters");									
							for (String pattern : patterns) {
								
								if (relation.rel.matches(pattern)) {
									SystemAnswer answer = new SystemAnswer();
									answer.confidence_score = relation.confidence;
									answer.justify = docRelations.docid;
									answer.slot_name = "org:place_of_headquarters";											
									answer.slot_filler = relation.arg2;											
									answer.start_offset_filler = 0;
									answer.end_offset_filler = 0;
									answer.start_offset_justification = 0;
									answer.end_offset_justification = 0;
									answer.relation = relation;
									q.system_answers.put("org:place_of_headquarters", answer);					
								}
							}	
						}
						
						else {
							/* other patterns */
							LinkedList<String> patterns = ORGSlots.slots_patterns.get(attribute);
							
							for (String pattern : patterns) {									
								if (relation.rel.matches(pattern + ".*")) {
									
									SystemAnswer answer = new SystemAnswer();
									
									/*
									System.out.println(pattern + '\t' + relation.arg2_tagged);
									
									if (attribute.equalsIgnoreCase("per:date_of_birth")) {
										String expectedEntityType = "DATE";
										
										System.out.println("place of birth arg2: " + relation.arg2_tagged);
									}
									*/
									
									answer.confidence_score = relation.confidence;
									answer.justify = docRelations.docid;
									answer.slot_name = attribute;											
									answer.slot_filler = relation.arg2;
									answer.start_offset_filler = 0;
									answer.end_offset_filler = 0;
									answer.start_offset_justification = 0;
									answer.end_offset_justification = 0;
									answer.relation = relation;											
									q.system_answers.put(attribute, answer);
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static void matchPatternExtractions( Map<String, SFQuery> queries ) {
		
		Set<String> queries_keys = queries.keySet();
		
		System.out.println("\nLooking for relations matching patterns\n");
		
		for (String q_id : queries_keys) {
			SFQuery q = queries.get(q_id);		
			System.out.print(q.query_id + '\t' + q.etype + '\t' + q.name + '\n');

			if (q.etype.equalsIgnoreCase("PER"))
				matchPERPatterns(q);
			else if (q.etype.equalsIgnoreCase("ORG"))
				matchORGPatterns(q);
		}
		System.out.println();
	}

	public static void selectExtractions(Map<String, SFQuery> queries) {
								
		//TODO: determine if place is country, state or city			
		//TODO: get the offsets of sentence and args
		
		//TODO: fazer match de forma a evitar falsos positivos
		
		/* falsely accused of being*/
		/* have never been accused of */
		/* were wrongly accused of */

		/* was not born in */
		
		/* joined by */
		/* joined in */
		/* joined to */
		/* joined at */
		/* joined forces with */
		/* joined [A-Z]+ of */
		/* joined [A-Z]+ for */
		/* joined [A-Z]+ as */
		/* joined with */
		/* joined from*/
		
		/* can also be paid by */
		
		Set<String> keys = queries.keySet();
		
		for (String keyQ : keys) {

			SFQuery q = queries.get(keyQ);
			Multiset<String> slots = q.system_answers.keys();
			
			for (String slot : slots) {
				
				Set<SystemAnswer> answers = q.system_answers.get(slot);				
				
				if (Definitions.one_answer_only.contains(slot)) {
					
					if (slot.equalsIgnoreCase("per:date_of_birth")) {
						String expectedEntityType = "DATE";
					}
					
					
					if (slot.equalsIgnoreCase("per:date_of_death")) {
						String expectedEntityType = "DATE";
					}
					
					if (slot.equalsIgnoreCase("per:place_of_birth")) {
						String expectedEntityType = "LOCATION";
					}
					
					if (slot.equalsIgnoreCase("per:place of death")) {
						String expectedEntityType = "LOCATION";
					}
					
					if (slot.equalsIgnoreCase("per:age")) {
						String expectedEntityType1 = "NUMBER";
						String expectedEntityType2 = "DATE";
					}
					
					if (slot.equalsIgnoreCase("per:origin")) {
						String expectedEntityType1 = "LOCATION";
					}
					
					if (slot.equalsIgnoreCase("per:religion")) {
						String expectedEntityType = "LOCATION";
					}
					
					if (slot.equalsIgnoreCase("per:cause_of_death")) {
						
					}
					
					/* choose only one best answer */					
					float top_score = 0;
					SystemAnswer answer = new SystemAnswer();
					
					for (SystemAnswer systemAnswer : answers) {					
						if (systemAnswer.confidence_score > top_score) {
							answer = systemAnswer;
						}
					}					
					q.selected_answers.put(slot, answer);					
				}
			}
		}
	}
		
	public static void outputresults(Map<String, SFQuery> queries) throws IOException {
		
		int unique_run_id = 0;
		FileWriter output = new FileWriter("results.txt"); 
		Set<String> keys = queries.keySet();
		
		for (String keyQ : keys) {			
			
			SFQuery q = queries.get(keyQ);
			
			Set<String> one_answer_only_slots = q.selected_answers.keySet();
			
			for (String slot : one_answer_only_slots) {
				SystemAnswer answer = q.selected_answers.get(slot);
				
				output.write(q.query_id + '\t' + answer.slot_name + '\t' + Integer.toString(unique_run_id) + '\t' + 
						answer.justify + '\t' + answer.slot_filler + '\t' + answer.start_offset_filler + '\t' + answer.end_offset_filler + '\t'
						+ answer.start_offset_justification + '\t' + answer.end_offset_justification + '\t' + answer.confidence_score + '\n');
			}


			Multiset<String> answers_keys = q.system_answers.keys();
			
			for (String slot : answers_keys) {
				
				if (Definitions.one_answer_only.contains(slot)) {
					continue;
				}
				
				else {					
					Set<SystemAnswer> answers = q.system_answers.get(slot);				
					
					for (SystemAnswer answer : answers) {					
						output.write(q.query_id + '\t' + answer.slot_name + '\t' + Integer.toString(unique_run_id) + '\t' + 
						answer.justify + '\t' + answer.slot_filler + '\t' + answer.start_offset_filler + '\t' + answer.end_offset_filler + '\t'
						+ answer.start_offset_justification + '\t' + answer.end_offset_justification + '\t' + answer.confidence_score + '\n');
					}					
				}				
			}
		}
		
		output.close();
	}
	
}





