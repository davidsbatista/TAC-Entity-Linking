package tac.kbp.bin;

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
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import tac.kbp.kb.index.spellchecker.SuggestWord;
import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.candidates.Candidate;

import com.google.common.base.Joiner;

public class Train {
	
	static int total_n_docs = 0;
	static int FOUND_queries = 0;
	static int n_queries_zero_docs = 0;
	static int NIL_queries = 0;
	static int MISS_queries = 0;
	public static ArrayList<double[]> inputs = new ArrayList<double[]>();
	public static ArrayList<Integer> outputs = new ArrayList<Integer>();
	
	static void tune(KBPQuery q) throws Exception {
		
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
	
	static void statisticsRecall() throws Exception {
		
		float miss_rate = (float) Train.MISS_queries / ((float) tac.kbp.bin.Definitions.queriesTrain.size()-Train.NIL_queries);
		float coverage = (float) Train.FOUND_queries / ((float) tac.kbp.bin.Definitions.queriesTrain.size()-Train.NIL_queries);
		
		System.out.println("Documents Retrieved: " + Integer.toString(Train.total_n_docs));
		System.out.println("Queries: " + Integer.toString(tac.kbp.bin.Definitions.queriesTrain.size()));
		System.out.println("Docs p/ query: " + ( (float) Train.total_n_docs / (float) tac.kbp.bin.Definitions.queriesTrain.size()));
		System.out.println("Queries with 0 docs retrieved: " + Integer.toString(Train.n_queries_zero_docs));
		System.out.println("Queries NIL: " + Train.NIL_queries);
		System.out.println("Queries Not Found (Miss Rate): " + Train.MISS_queries + " (" + miss_rate * 100 + "%)" );
		System.out.println("Queries Found (Coverage): " + Train.FOUND_queries + " (" + coverage * 100 + "%)" );
	}
	
	static void process(List<KBPQuery> queries, boolean topics, boolean supportDocument) throws Exception {
		
		// Process each query 
		for (KBPQuery q : queries) {

			if (supportDocument) {
				q.getSupportDocument();
				//q.getNamedEntities();
			}
			if (topics) q.getTopicsDistribution(queries.indexOf(q));
			q.getAlternativeSenses(Definitions.binaryjedis);			
			System.out.print("\n"+q.query_id + " \"" + q.name + '"' + "\t" + q.alternative_names.size());			
		}
	}
	
	static void generateFeatures(List<KBPQuery> queries) throws Exception{
		
		int count = 1;
		
		for (KBPQuery q : queries) {
			System.out.print("\n"+q.query_id + " \"" + q.name + '"');
			retrieveCandidates(q);
			extractFeatures(q, false);
			System.out.print("\t(" + count + "/" + queries.size() + ")\n");
			count++;
		}
	}	
	
	static void retrieveCandidates(KBPQuery q) throws Exception {
		
		List<SuggestWord> suggestedwords = queryKB(q);		
		int n_docs = getCandidates(q,suggestedwords);
		
		System.out.println(" " + n_docs + " candidates");				
		
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
	
	static void extractFeatures(KBPQuery q, boolean saveToFile) throws Exception {
		
		System.out.print("Extracting features from candidates");
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
			QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", new WhitespaceAnalyzer());
			ScoreDoc[] scoreDocs = null;
			String queryS = "id:" + q.gold_answer;
			
			TopDocs docs = tac.kbp.bin.Definitions.searcher.search(queryParser.parse(queryS), 1);				
			scoreDocs = docs.scoreDocs;
			
			if (docs.totalHits != 0) {
				Document doc = tac.kbp.bin.Definitions.searcher.doc(scoreDocs[0].doc);				
				
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
		
	static String concatenateEntities(String str1, String str2) {
		
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
	
	static int tune_recall(KBPQuery q) throws IOException, ParseException {
		
		HashMap<String, HashSet<String>> query = q.generateQuery();
		ArrayList<SuggestWord> suggestedwords = new ArrayList<SuggestWord>();		
		HashSet<String> eid_already_retrieved = new HashSet<String>();
		
		for (String sense : query.get("strings")) {
			List<SuggestWord> list = tac.kbp.bin.Definitions.spellchecker.suggestSimilar(sense, Definitions.max_candidates);
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
	
	static List<SuggestWord> queryKB(KBPQuery q) throws IOException, ParseException {
		
		HashMap<String, HashSet<String>> query = q.generateQuery();
		Set<SuggestWord> suggestedwords = new HashSet<SuggestWord>();		
		HashSet<String> eid_already_retrieved = new HashSet<String>();
		
		for (String sense : query.get("strings")) {
			List<SuggestWord> list = tac.kbp.bin.Definitions.spellchecker.suggestSimilar(sense, Definitions.max_candidates);
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
		
	static int getCandidates(KBPQuery q, List<SuggestWord> suggestedwordsList) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", analyzer);
		
		List<Integer> repeated = new LinkedList<Integer>(); // to avoid having repeated docs
		
		for (SuggestWord suggestWord : suggestedwordsList) {
			
			String queryS = "id:" + suggestWord.eid;
			TopDocs docs = tac.kbp.bin.Definitions.searcher.search(queryParser.parse(queryS), 1);
			
			if (docs.totalHits == 0)
				continue;
			else {
				Document doc = tac.kbp.bin.Definitions.searcher.doc(docs.scoreDocs[0].doc);
				if (repeated.contains(docs.scoreDocs[0].doc))
					continue;
				else {
					Candidate c = new Candidate(doc,docs.scoreDocs[0].doc); 
					q.candidates.add(c);
					repeated.add(docs.scoreDocs[0].doc);
				}
			}
		}
		
		return q.candidates.size();		
	}
}