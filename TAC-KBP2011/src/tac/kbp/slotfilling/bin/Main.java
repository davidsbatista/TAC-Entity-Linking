package tac.kbp.slotfilling.bin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

import tac.kbp.configuration.Definitions;
import tac.kbp.slotfilling.queries.LoadQueries;
import tac.kbp.slotfilling.queries.SFQuery;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		Definitions.loadDocumentCollecion();
		Definitions.loadClassifier("/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz");
		Definitions.connectionREDIS();
		
		Map<String, SFQuery> queries = LoadQueries.loadXML(args[0]);
		parseQueries(queries);
		//loadQueriesAnswers(args[1],queries);
	}
	
	public static void parseQueries(Map<String, SFQuery> queries) throws Exception {
		
		Set<String> keys = queries.keySet();
		
		for (String k : keys) {
			
			SFQuery q = queries.get(k);
			
			System.out.println("q_id: " + q.query_id);
			System.out.println("name: " + q.name);			
			System.out.println("ignore:" + q.ignore);			
			q.getSupportDocument();
			
			//TODO: get extra-information from KB: wiki_text, other attributes fills
			
			
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
				
			System.out.println("");
			// query document collection
									
		}
	}
	
	public static void loadQueriesAnswers(String filename, Map<String, SFQuery> queries) {
		
		System.out.println("Loading answers from: " + filename);			
		
		try {
			
			FileInputStream fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));			
			String strLine;
				
			while ((strLine = br.readLine()) != null)   {
				String[] data = strLine.split("\t");
				SFQuery q = queries.get(data[1]);
				/*
				filler_id
				sf_id	
				system_id	
				slot_name	
				docid	
				start_char	
				end_char		
				response	
				norm_response	
				equiv_class_id	
				judgment
				*/
			}
				
			in.close();
		}
		
		catch (Exception e)	{
				//Catch exception if any
				System.err.println("Error: " + e.getMessage());
			}
		}
}
