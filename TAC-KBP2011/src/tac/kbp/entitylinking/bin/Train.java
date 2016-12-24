package tac.kbp.entitylinking.bin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;

import tac.kbp.configuration.Definitions;
import tac.kbp.entitylinking.queries.ELQuery;
import tac.kbp.entitylinking.queries.candidates.Candidate;
import tac.kbp.kb.index.spellchecker.SuggestWord;
import tac.kbp.utils.string.Abbreviations;
import tac.kbp.utils.string.ExtractAbbrev;

import com.google.common.base.Joiner;


public class Train {
	
	static int total_n_docs = 0;
	static int FOUND_queries = 0;
	static int n_queries_zero_docs = 0;
	static int NIL_queries = 0;
	static int MISS_queries = 0;
	public static ArrayList<double[]> inputs = new ArrayList<double[]>();
	public static ArrayList<Integer> outputs = new ArrayList<Integer>();
	
	static void tune(ELQuery q) throws Exception {
		
		//q.getSupportDocument();
		//q.getSupportDocumentTerms();
		
		int n_docs = tune_recall(q);
		
		total_n_docs += n_docs;
		
		if (n_docs == 0)
			n_queries_zero_docs++;
		
		boolean found = false;
		
		for (Candidate c : q.candidates) {			
			if (c.entity.id.equalsIgnoreCase(q.gold_answer)) {				
				FOUND_queries++;
				found = true;
				break;
			}
		}
		if (!found && q.gold_answer.startsWith("NIL"))
			NIL_queries++;
		
		if (!found && !q.gold_answer.startsWith("NIL"))
			MISS_queries++;
		
		System.out.println();
	}
	
	static void statisticsRecall(List<ELQuery> queries) throws Exception {
		
		float miss_rate = (float) Train.MISS_queries / ((float) queries.size()-Train.NIL_queries);
		float coverage = (float) Train.FOUND_queries / ((float) queries.size()-Train.NIL_queries);
		
		System.out.println("Documents Retrieved: " + Integer.toString(Train.total_n_docs));
		System.out.println("Queries: " + Integer.toString(queries.size()));
		System.out.println("Docs p/ query: " + ( (float) Train.total_n_docs / (float) queries.size()));
		System.out.println("Queries with 0 docs retrieved: " + Integer.toString(Train.n_queries_zero_docs));
		System.out.println("Queries NIL: " + Train.NIL_queries);
		System.out.println("Queries Not Found (Miss Rate): " + Train.MISS_queries + " (" + miss_rate * 100 + "%)" );
		System.out.println("Queries Found (Coverage): " + Train.FOUND_queries + " (" + coverage * 100 + "%)" );
	}
	
	static void process(List<ELQuery> queries, boolean topics, boolean supportDocument) throws Exception {
		
		// Process each query 
		for (ELQuery q : queries) {

			if (supportDocument) {
				q.getSupportDocument();
				q.getNamedEntities();
			}
			
			if (topics) q.getTopicsDistribution(queries.indexOf(q));
			
			q.getAlternativeSenses();			
			System.out.print("\n"+q.query_id + " \"" + q.name + '"');
			
			/* Schwartz and Hirst abbreviations and acronyms extractor*/
			ExtractAbbrev extractAbbrv =  new ExtractAbbrev();
			q.abbreviations = extractAbbrv.extractAbbrPairs(q.supportDocument);			
			boolean acronym = true;
			
			for (int j = 0; j < q.name.length(); j++) {
				if (Character.isLowerCase(q.name.charAt(j))) {
					acronym = false;
				}
			}
			
			if (acronym) {
				 for (Abbreviations abbreviation : q.abbreviations) {					
					if (abbreviation.getShortForm().equalsIgnoreCase(q.name)) {
						q.alternative_names.add(abbreviation.getLongForm());
					}
				}
			}
			
			System.out.print("\t" + q.alternative_names.size());
		}
	}
	
	static void generateFeatures(List<ELQuery> queries) throws Exception{
		
		int count = 1;
		
		for (ELQuery q : queries) {
			retrieveCandidates(q);
			extractFeatures(q, false);
			System.out.print("\t(" + count + "/" + queries.size() + ")\n");
			count++;
		}
	}	
	
	static void retrieveCandidates(ELQuery q) throws Exception {
		
		//TODO: if query-string is an acronym:
		// look for expansions:
		//	support-document
		//	mappings dictionary
		//  if expansions are found use them only, instead of original name-string		
		//name-string AND in wiki_text
		
		List<SuggestWord> suggestedwords = queryKB(q);
		int n_docs = getCandidates(q,suggestedwords);
		
		System.out.println(q.query_id + ": \"" + q.name + "\" " + n_docs + " candidates");
		
		total_n_docs += n_docs;		
		if (n_docs == 0) n_queries_zero_docs++;		
		boolean found = false;
		for (Candidate c : q.candidates) {			
			if (c.entity.id.equalsIgnoreCase(q.gold_answer)) {				
				FOUND_queries++;
				found = true;
				break;
			}
		}		
		if (!found && q.gold_answer.startsWith("NIL"))
			NIL_queries++;
		if (!found && !q.gold_answer.startsWith("NIL"))
			MISS_queries++;
	}		
	
	static void extractFeatures(ELQuery q, boolean saveToFile) throws Exception {
		
		System.out.println("Extracting features from candidates");
		PrintStream out = null;
		
		if (saveToFile) {
			//file to where feature vectors are going to be written
			out = new PrintStream( new FileOutputStream(q.query_id+".txt"));
		}
		
		boolean foundCorrecEntity = false;
		
		for (Candidate c : q.candidates) {
			System.out.print(".");
			c.extractFeatures(q);
			if (c.features.correct_answer) {
				foundCorrecEntity = true;
			}
			if (saveToFile) {
				writeFeaturesVectortoFile(out, c);
				out.println();
			}
		}
		
		//if answer entity is not part of the retrieved entities and correct answer is not NIL,
		//we retrieve answer entity from KB and extract features
		
		if (!foundCorrecEntity && !(q.gold_answer.startsWith("NIL")) ) {
			QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_35,"id", new WhitespaceAnalyzer(Version.LUCENE_35));
			ScoreDoc[] scoreDocs = null;
			String queryS = "id:" + q.gold_answer;
			
			TopDocs docs = tac.kbp.configuration.Definitions.knowledge_base.search(queryParser.parse(queryS), 1);				
			scoreDocs = docs.scoreDocs;
			
			if (docs.totalHits != 0) {
				Document doc = tac.kbp.configuration.Definitions.knowledge_base.doc(scoreDocs[0].doc);				
				
				Candidate c = new Candidate(doc,scoreDocs[0].doc);
				
				//associate candidate with query
				q.candidates.add(c);
				
				//extract features
				c.extractFeatures(q);
				if (saveToFile) {
					writeFeaturesVectortoFile(out, c);
					out.println();
				}
			}
		}
		
		System.out.print("done");
		if (saveToFile) {
			out.close();
		}
	}

	static void writeFeaturesVectortoFile(PrintStream out, Candidate c) {
		
		//write feature vector to file
		double[] vector = c.features.featuresVector();
		int output = c.features.output();

		//first field of line is candidate identifier;
		out.print(c.entity.id+":");
		
		for (int i = 0; i < vector.length; i++) {
			out.print(vector[i] + ",");
		}
		out.print(output);

		//structures holding all the generated features vectors + outputs: to be passed to LogisticRegression
		inputs.add(c.features.featuresVector());
		outputs.add(c.features.output());
	}
		
	static String concatenateOR(String str1, String str2) {
		
		String result = new String();
		
		Joiner orJoiner = Joiner.on(" OR ");
		
		if (str1.length() > 0 && str2.length()>0) {
			result = orJoiner.join(str1, str2);
		}
		
		else if (str1.length()>0 && str2.length()==0) {
			result = str1;
		}
		
		else if (str1.length()==0 && str2.length()>0) {
			result = str2;
		}
		
		return result;
	}
	
	static int tune_recall(ELQuery q) throws IOException, ParseException {
		
		HashMap<String, HashSet<String>> query = q.generateQuery();
		ArrayList<SuggestWord> suggestedwords = new ArrayList<SuggestWord>();		
		HashSet<String> eid_already_retrieved = new HashSet<String>();
		
		for (String sense : query.get("strings")) {
			List<SuggestWord> list = tac.kbp.configuration.Definitions.spellchecker.suggestSimilar(sense, Definitions.candidates_per_sense);
			for (SuggestWord s : list) {
				if (eid_already_retrieved.contains(s.eid)) continue;
				else {
					suggestedwords.add(s);
					eid_already_retrieved.add(s.eid);
				}
			}
		}
		
		Collections.sort(suggestedwords);		
		System.out.print("\t"+suggestedwords.size());
		
		boolean found = false;
		
		for (SuggestWord s : suggestedwords) {
			
			if (s.eid.equalsIgnoreCase(q.gold_answer)) {
				System.out.print("\t" + q.alternative_names.size() + "\t" + s.score + "\t" + (suggestedwords.indexOf(s)+1));
				found = true;
			}
			
			Candidate c = new Candidate();
			c.entity.id = s.eid;
			q.candidates.add(c);
		}
		if (!found) System.out.print("\t0"+"\t"+q.alternative_names.size()+"\t0");
		
		return suggestedwords.size();
	}
	
	static List<SuggestWord> queryKB(ELQuery q) throws IOException, ParseException {
		
		HashMap<String, HashSet<String>> query = q.generateQuery();
		
		Set<SuggestWord> suggestedwords = new HashSet<SuggestWord>();		
		HashSet<String> eid_already_retrieved = new HashSet<String>();
		
		for (String sense : query.get("strings")) {
			List<SuggestWord> list = tac.kbp.configuration.Definitions.spellchecker.suggestSimilar(sense, Definitions.candidates_per_sense);
			for (SuggestWord s : list) {
				if (eid_already_retrieved.contains(s.eid)) continue;
				else {
					suggestedwords.add(s);
					eid_already_retrieved.add(s.eid);
				}
			}
		}

		List<SuggestWord> suggestedwordsList = new ArrayList<SuggestWord>(suggestedwords);
		Collections.sort(suggestedwordsList);
		
		return suggestedwordsList;
	}
		
	static int getCandidates(ELQuery q, List<SuggestWord> suggestedwordsList) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_35);
		QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_35,"id", analyzer);
		List<Integer> repeated = new LinkedList<Integer>(); // to avoid having repeated docs
		
		for (SuggestWord suggestWord : suggestedwordsList) {
			
			String queryS = "id:" + suggestWord.eid;
			TopDocs docs = tac.kbp.configuration.Definitions.knowledge_base.search(queryParser.parse(queryS), 1);
			
			if (docs.totalHits == 0)
				continue;
			
			else {
				
				if (repeated.contains(docs.scoreDocs[0].doc))
					continue;
				
				else {
					Document doc = tac.kbp.configuration.Definitions.knowledge_base.doc(docs.scoreDocs[0].doc);
					Candidate c = new Candidate(doc,docs.scoreDocs[0].doc); 
					q.candidates.add(c);
					repeated.add(docs.scoreDocs[0].doc);
					
					if (q.candidates.size() == Definitions.max_candidates) {
						break;
					}
					
				}
			}
		}
		
		return q.candidates.size();		
	}

	static int getCandidates(ELQuery q) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_35);
		QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", analyzer);
		
		List<Integer> repeated = new LinkedList<Integer>(); // to avoid having repeated docs
		Joiner orJoiner = Joiner.on(" OR ");
		Joiner andJoiner = Joiner.on(" AND ");
		String queryAND = "";
		
		if (q.alternative_names.size()>0) {
			
			queryAND = andJoiner.join(q.alternative_names);
			
			System.out.println("query: " + queryAND);
			
			Query query = queryParser.parse("wiki_text: " + queryAND);			
			TopDocs docs = tac.kbp.configuration.Definitions.knowledge_base.search(query, 20);
			
			if (docs.totalHits != 0) {
				for (int i = 0; i < docs.scoreDocs.length; i++) {		
					
					Document doc = tac.kbp.configuration.Definitions.knowledge_base.doc(docs.scoreDocs[i].doc);
					Candidate c = new Candidate(doc,docs.scoreDocs[i].doc); 
					q.candidates.add(c);
					repeated.add(docs.scoreDocs[i].doc);
					
					if (q.candidates.size() == Definitions.max_candidates) {
						break;
					}
				}
			}

			
			for (String w : q.alternative_names) {
				
				String queryOR = "";
				
				String[] parts = w.split("\\s+");
				Set<String> valid = new HashSet<String>();
				
				for (int i = 0; i < parts.length; i++) {
					if (Definitions.stop_words.contains(parts[i]) && parts[i].length()< 3 )
						continue;
					
					else  valid.add( '"' + parts[i] + '"');
						
				}
				
				queryOR += orJoiner.join(valid);
				
				query = queryParser.parse("name:" + queryOR);			
				docs = tac.kbp.configuration.Definitions.knowledge_base.search(query, 30);
				
				if (docs.totalHits == 0)
					continue;
				
				else {
					
					if (repeated.contains(docs.scoreDocs[0].doc))
						continue;
					
					else {
						Document doc = tac.kbp.configuration.Definitions.knowledge_base.doc(docs.scoreDocs[0].doc);
						Candidate c = new Candidate(doc,docs.scoreDocs[0].doc); 
						q.candidates.add(c);
						repeated.add(docs.scoreDocs[0].doc);
						
						if (q.candidates.size() == Definitions.max_candidates) {
							break;
						}
						
					}
				}
			}
		}
		
		else {
			
			Query query = queryParser.parse(q.name);			
			TopDocs docs = tac.kbp.configuration.Definitions.knowledge_base.search(query, 20);
			if (docs.totalHits != 0) {
				for (int i = 0; i < docs.scoreDocs.length; i++) {		
					
					Document doc = tac.kbp.configuration.Definitions.knowledge_base.doc(docs.scoreDocs[i].doc);
					Candidate c = new Candidate(doc,docs.scoreDocs[i].doc); 
					q.candidates.add(c);
					repeated.add(docs.scoreDocs[i].doc);
				}
			}
			
		}
		
		return q.candidates.size();		
	}
}