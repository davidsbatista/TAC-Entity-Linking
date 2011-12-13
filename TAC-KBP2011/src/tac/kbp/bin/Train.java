package tac.kbp.bin;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import tac.kbp.kb.index.spellchecker.SuggestWord;
import tac.kbp.queries.GoldQuery;
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
	
	static void getCandidates(KBPQuery q) throws Exception {

		q.getSupportDocument();
		
		int n_docs = queryKB(q);
		System.out.print("  " + n_docs);
		
		total_n_docs += n_docs;
		
		if (n_docs == 0)
			n_queries_zero_docs++;
		
		System.out.print("\t correct answer: "+ tac.kbp.bin.Definitions.queriesGold.get(q.query_id).answer);
		findCorrectEntity(q);

		//load the recognized named-entities in the support document
		q.getNamedEntities();
		
		//load the LDA topics distribution for the query support document
		q.getTopicsDistribution(tac.kbp.bin.Definitions.queries.indexOf(q));
		
		//extract features from all candidates
		extractFeatures(q, true);
	}
	
	static void extractFeatures(KBPQuery q, boolean saveToFile) throws Exception {
		
		System.out.print(" Extracting features from candidates");
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
		
		System.out.println("foundCorrecEntity: " + foundCorrecEntity);
		
		//if answer entity is not part of the retrieved entities and correct answer is not NIL, 
		//retrieve answer entity from KB and extract features 
		if (!foundCorrecEntity && !(Definitions.queriesGold.get(q.query_id).answer.startsWith("NIL")) ) {
			
			System.out.println("retrieving answer from KB");

			QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", new WhitespaceAnalyzer());
			ScoreDoc[] scoreDocs = null;
			String queryS = "id:" + Definitions.queriesGold.get(q.query_id).answer;
			
			TopDocs docs = tac.kbp.bin.Definitions.searcher.search(queryParser.parse(queryS), 1);				
			scoreDocs = docs.scoreDocs;
			
			if (docs.totalHits != 0) {
				Document doc = tac.kbp.bin.Definitions.searcher.doc(scoreDocs[0].doc);
				
				//extract features
				Candidate c = new Candidate(doc,scoreDocs[0].doc);
				c.features.lucene_score = scoreDocs[0].score; 
				c.extractFeatures(q);
				if (saveToFile) {
					writeFeaturesVectortoFile(out, c);
					out.println();
				}
			}
		}
		System.out.println();
		if (saveToFile) {
			out.close();
		}
	}

	static void writeFeaturesVectortoFile(PrintStream out, Candidate c) {
		
		//write feature vector to file
		double[] vector = c.features.inputVector();
		int output = c.features.output();

		//first field of line is candidate identifier;
		out.print(c.entity.id+":");
		
		for (int i = 0; i < vector.length; i++) {
			out.print(vector[i] + ",");
		}
		out.print(output);

		//structures holding all the generated features vectors + outputs: to be passed to LogisticRegression
		inputs.add(c.features.inputVector());
		outputs.add(c.features.output());
	}

	static void findCorrectEntity(KBPQuery q) throws CorruptIndexException, IOException {
				
		GoldQuery q_gold = tac.kbp.bin.Definitions.queriesGold.get(q.query_id);
		
		boolean found = false;
		
		for (Candidate c : q.candidates) {			
			String eid = c.entity.id;
			if (eid.equalsIgnoreCase(q_gold.answer)) {
				System.out.print('\t' + " found");
				FOUND_queries++;
				found = true;
				break;
			}
		}
		
		if (!found && q_gold.answer.startsWith("NIL"))
			NIL_queries++;
		
		if (!found && !q_gold.answer.startsWith("NIL"))
			MISS_queries++;
		}
	
	static void generateOutput(String output) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (Iterator<KBPQuery> iterator = tac.kbp.bin.Definitions.queries.iterator(); iterator.hasNext();) {
			KBPQuery q = (KBPQuery) iterator.next();
			out.println(q.query_id.trim()+"\t"+q.answer_kb_id.trim());
		}
		out.close();		
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
	
	static int queryKB(KBPQuery q) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		HashMap<String, HashSet<String>> query = generateQuery(q);
		
		Set<SuggestWord> suggestedwords = new HashSet<SuggestWord>();
		
		for (String sense : query.get("strings")) {
			List<SuggestWord> l = tac.kbp.bin.Definitions.spellchecker.suggestSimilar(sense, 10);
			suggestedwords.addAll(l);
		}
		
		QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", analyzer);
		ScoreDoc[] scoreDocs = null;
				
		List<SuggestWord> suggestedwordsList = new ArrayList<SuggestWord>(suggestedwords);
		Collections.sort(suggestedwordsList);
		
		int i=0; // to limit the number of docs
		List<Integer> repeated = new LinkedList<Integer>(); // to avoid having repeated docs
		
		for (SuggestWord suggestWord : suggestedwordsList) {
			
			if (i >= 70)
				break;			
			
			String queryS = "id:" + suggestWord.eid;
			TopDocs docs = tac.kbp.bin.Definitions.searcher.search(queryParser.parse(queryS), 1);
			
			if (docs.totalHits == 0) {
				continue;				
			}
			
			else {
				scoreDocs = docs.scoreDocs; 
				Document doc = tac.kbp.bin.Definitions.searcher.doc(scoreDocs[0].doc);
				if (repeated.contains(scoreDocs[0].doc))
					continue;
				else {
					Candidate c = new Candidate(doc,scoreDocs[0].doc);
					c.features.lucene_score = scoreDocs[0].score; 
					q.candidates.add(c);
					repeated.add(scoreDocs[0].doc);
				}
			}
			i++;
		}
		
		/*
		Joiner orJoiner = Joiner.on(" OR ");
		
		HashSet<String> strings = query.get("strings");
		HashSet<String> tokens = query.get("tokens");
		
		// remove stop words
		strings.removeAll(stop_words);
		tokens.removeAll(stop_words);
		
		String qString = orJoiner.join(strings);		
		String qTokens = orJoiner.join(tokens);
		
		String qStringTokens =  qString + " OR " + qTokens;
		
		/*
		String persons = orJoiner.join(q.persons); 
		String organizations = orJoiner.join(q.organizations);
		String places = orJoiner.join(q.places);
		
		String queryEntities = concatenateEntities(persons, organizations);
		queryEntities += concatenateEntities(queryEntities, places);
		
		if (queryEntities.length() > 0) {
			qStringTokens += " OR " + queryEntities;
		}

		//query the name and the wiki_title with the alternative names and tokens made up from the alternative names
		MultiFieldQueryParser multiFieldqueryParser = new MultiFieldQueryParser(org.apache.lucene.util.Version.LUCENE_30, new String[] {"name", "wiki_title","wiki_text"}, analyzer);		
		scoreDocs = null;

		try {
			
			TopDocs docs = searcher.search(multiFieldqueryParser.parse(qStringTokens), 30);
			scoreDocs = docs.scoreDocs;
			
			for (int i = 0; i < scoreDocs.length; i++) {
				Document doc = searcher.doc(scoreDocs[i].doc);
				String id = doc.getField("id").stringValue();
				q.candidates.add(id);
			}
			
		} catch (Exception e) {
			//TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);		
		}
		*/
		
		return q.candidates.size();
	}

	static HashMap<String, HashSet<String>> generateQuery(KBPQuery q) {
		
		HashSet<String> queryStrings = new HashSet<String>(); 		
		HashSet<String> queryTokens = new HashSet<String>();
			
		HashMap<String, HashSet<String>> query = new HashMap<String,HashSet<String>>();
		
		queryStrings.add('"' + q.name + '"');
		
		String[] tmp = q.name.split("\\s");
		for (int z = 0; z < tmp.length; z++) {
			if (!tmp[z].matches("\\s*-\\s*|.*\\!|\\!.*|.*\\:|\\:.*|\\s*")) {
				queryTokens.add('"' + tmp[z] + '"');
			}
		}
		
		for (Iterator<String> iterator = q.alternative_names.iterator(); iterator.hasNext();) {
			String alternative = (String) iterator.next();
			
			String queryParsed = alternative.replaceAll("\\(", "").replaceAll("\\)","").
										replaceAll("\\]", "").replaceAll("\\[", "").replaceAll("\"", "");
			
			queryStrings.add('"' + queryParsed + '"');
			
			String[] tokens = queryParsed.split("\\s");
			
			for (int i = 0; i < tokens.length; i++) {
				if (!tokens[i].matches("\\s*-\\s*|.*\\!|\\!.*|.*\\:|\\:.*|\\s*")) {
					queryTokens.add('"' + tokens[i].trim() + '"');
				}
			}
		}
		
		query.put("strings", queryStrings);
		query.put("tokens", queryTokens);
				
		return query;
		
	}

}