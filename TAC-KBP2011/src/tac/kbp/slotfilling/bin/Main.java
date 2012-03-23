package tac.kbp.slotfilling.bin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;

import tac.kbp.configuration.Definitions;
import tac.kbp.slotfilling.queries.LoadQueries;
import tac.kbp.slotfilling.queries.SFQuery;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multiset;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		Definitions.loadDocumentCollecion();
		//Definitions.loadClassifier("/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz");
		Definitions.connectionREDIS();
		Definitions.loadKBIndex();
		
		Map<String, SFQuery> queries = LoadQueries.loadXML(args[0]);		
		parseQueries(queries);
		loadQueriesAnswers(args[1],queries);
		recall(queries);
	}
	
	public static void parseQueries(Map<String, SFQuery> queries) throws Exception {
		
		Set<String> keys = queries.keySet();
		int docs_with_answers_found = 0;
		int queries_with_zero_docs = 0;
		
		for (String k : keys) {
			
			SFQuery q = queries.get(k);
			
			System.out.println("q_id: " + q.query_id);
			//System.out.println("name: " + q.name);			
			//System.out.println("ignore:" + q.ignore);			
			q.getSupportDocument();
			
			// get nodeid from KB: wiki_text, other attributes
			if (!q.nodeid.startsWith("NIL"))
				q.getKBEntry();
			
			// get sentences where entity occurs
			q.extractSentences();
			//System.out.println("sentences: " + q.sentences.size());
			
			// extract other entities in the support document
			//q.getNamedEntities();
			/*
			System.out.println("persons: " + q.persons.size());
			System.out.println("places: " + q.places.size());
			System.out.println("org: " + q.organizations.size());
			*/
					
			// get alternative senses for entity name and extract acronyms from support doc.
			/*
			q.getAlternativeSenses();
			q.extracAcronyms();
			System.out.println("senses: " + q.alternative_names);			
			System.out.println("abbreviations: " + q.abbreviations);
			*/

			/*
			System.out.println("attributes to be filled:");
			if (q.etype.equalsIgnoreCase("PER")) {
				PER_Attributes attributes = (PER_Attributes) q.attributes;
			}
			*/
			
			q.queryCollection();
			
		}
		
		
		
		
		
		
		
	}
	
	public static void recall(Map<String, SFQuery> queries) {
				
		Map<String, List<SFQuery>> answers_found = new HashMap<String, List<SFQuery>>();
		int queries_with_zero_docs=0;
		
		Set<String> keys = queries.keySet();
		
		for (String k : keys) {
			SFQuery q = queries.get(k);
			
			if (q.documents.size()==0) {
				queries_with_zero_docs++;
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
		
		System.out.println("Queries with 0 docs retrieved: " + queries_with_zero_docs);
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
			answers.put("answer_doc", data[4]);
			answers.put("response", data[5]);
			answers.put("norm_response", data[6]);
			answers.put("equiv_class_id", data[7]);
			answers.put("judgment", data[8]);
			q.answers.add(answers);
			
			while ((strLine = br.readLine()) != null)   {
				String previous_qid = current_qid;
				data = strLine.split("\t");
				current_qid = data[1];
				
				if (!current_qid.equalsIgnoreCase(previous_qid)) {					
					q = queries.get(current_qid);
					answers = new HashMap<String, String>();
					answers.put("slot_name", data[3]);
					answers.put("answer_doc", data[4]);
					answers.put("response", data[5]);
					answers.put("norm_response", data[6]);
					answers.put("equiv_class_id", data[7]);
					answers.put("judgment", data[8]);					
					q.answers.add(answers);
					
					}
				
				else {					
					answers.put("slot_name", data[3]);
					answers.put("answer_doc", data[4]);
					answers.put("response", data[5]);
					answers.put("norm_response", data[6]);
					answers.put("equiv_class_id", data[7]);
					answers.put("judgment", data[8]);
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
		
}
