package tac.kbp.queries;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import redis.clients.jedis.BinaryJedis;
import tac.kbp.kb.ivo_spellchecker.SuggestWord;
import tac.kbp.ranking.GenerateTrainningSet;
import tac.kbp.ranking.Regression;
import tac.kbp.utils.Definitions;

import com.google.common.base.Joiner;

public class ProcessQuery {
	
	static int total_n_docs = 0;
	static int FOUND_queries = 0;
	static int n_queries_zero_docs = 0;
	static int NIL_queries = 0;
	static int MISS_queries = 0;
	
	public static void main(String[] args) throws Exception {
		
		tac.kbp.utils.Definitions.loadAll(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);

		System.out.println(tac.kbp.utils.Definitions.queries.size() + " queries loaded");
		System.out.println(tac.kbp.utils.Definitions.stop_words.size() + " stopwords loaded");
		System.out.println(tac.kbp.utils.Definitions.queriesGold.size() + " queries gold standard loaded");
		
		for (Iterator<KBPQuery> iterator = tac.kbp.utils.Definitions.queries.iterator(); iterator.hasNext();) {
			KBPQuery query = (KBPQuery) iterator.next();
			
			System.out.print(query.query_id + " \"" + query.name + '"');
			getSenses(Definitions.binaryjedis, query);
			processQuery(query);		
		}
		
		float miss_rate = (float) MISS_queries / ((float) tac.kbp.utils.Definitions.queries.size()-NIL_queries);
		float found_rate = (float) FOUND_queries / ((float) tac.kbp.utils.Definitions.queries.size()-NIL_queries);
		
		System.out.println("Documents Retrieved: " + Integer.toString(total_n_docs));
		System.out.println("Queries: " + Integer.toString(tac.kbp.utils.Definitions.queries.size()));
		System.out.println("Docs p/ query: " + ( (float) total_n_docs / (float) tac.kbp.utils.Definitions.queries.size()));
		System.out.println("Queries with 0 docs retrieved: " + Integer.toString(n_queries_zero_docs));
		System.out.println("Queries NIL: " + NIL_queries);
		System.out.println("Queries Misses: " + MISS_queries + " (" + miss_rate + "%)" );
		System.out.println("Queries Found: " + FOUND_queries + " (" + found_rate + "%)" );
		
		//String.format("%.2g%n", 0.912300);
		
		GenerateTrainningSet.generateFeatures();
		Regression.generateVectors(GenerateTrainningSet.inputs, GenerateTrainningSet.outputs);
		Regression.calculate();
				
		Definitions.searcher.close();
		Definitions.documents.close();
		Definitions.binaryjedis.disconnect();

	}

	private static void getSenses(BinaryJedis binaryjedis, KBPQuery query) {
	
	try {

		byte[] queryStringbyteArray = query.name.getBytes("UTF-8");
		byte[] queryStringLowbyteArray = query.name.toLowerCase().getBytes("UTF-8");
		
		byte[] acronyms = binaryjedis.get(queryStringLowbyteArray);
		byte[] senses = binaryjedis.get(queryStringbyteArray);
		
		if (acronyms != null) {						
			String acr = new String(acronyms, "UTF8");
			String[] acronymsArray = acr.split(",\\s");
			
			for (int i = 0; i < acronymsArray.length; i++) {
				String cleaned = tac.kbp.utils.string.StringUtils.cleanString(acronymsArray[i]);
				if (cleaned.compareToIgnoreCase(query.name) != 0) {
					query.alternative_names.add(cleaned);
				}
										
			}
		}
		
		if (senses != null) {
			String ses = new String(senses, "UTF8");
			String[] sensesArray = ses.split(",\\s");
			for (int i = 0; i < sensesArray.length; i++) {
				String cleaned = tac.kbp.utils.string.StringUtils.cleanString(sensesArray[i]);			
				if (cleaned.compareToIgnoreCase(query.name) != 0) {
					query.alternative_names.add(cleaned);
				}		
			}
		}
	}
	
	catch (Exception e) {
			// Catch exception if any
			System.out.println(e);
			System.err.println("Error: " + e.getMessage());
		}
	}

	private static void processQuery(KBPQuery q) throws Exception {

		q.getSupportDocument();
		
		int n_docs = queryKB(q);
		System.out.print("  " + n_docs);

		total_n_docs += n_docs;
		
		if (n_docs == 0)
			n_queries_zero_docs++;
		
		System.out.print("\t correct answer: "+ tac.kbp.utils.Definitions.queriesGold.get(q.query_id).answer);
		findCorrectEntity(q);
		System.out.println();

		//load the recognized named-entities in the support document
		q.loadNamedEntitiesXML();
		
		//load the LDA topics distribution for the query support document
		//q.getTopicsDistribution(tac.kbp.utils.Definitions.queries.indexOf(q));
	}

	private static void findCorrectEntity(KBPQuery q) throws CorruptIndexException, IOException {
				
		GoldStandardQuery q_gold = tac.kbp.utils.Definitions.queriesGold.get(q.query_id);
		
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
	
	private static void generateOutput(String output) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (Iterator<KBPQuery> iterator = tac.kbp.utils.Definitions.queries.iterator(); iterator.hasNext();) {
			KBPQuery q = (KBPQuery) iterator.next();
			out.println(q.query_id.trim()+"\t"+q.answer_kb_id.trim());
		}
		out.close();		
	}
	
	private static String concatenateEntities(String str1, String str2) {
		
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
	
	private static int queryKB(KBPQuery q) throws IOException, ParseException {
		
		WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
		HashMap<String, HashSet<String>> query = generateQuery(q);
		
		Set<SuggestWord> suggestedwords = new HashSet<SuggestWord>();
		
		for (String sense : query.get("strings")) {
			List<SuggestWord> l = tac.kbp.utils.Definitions.spellchecker.suggestSimilar(sense, 10);
			suggestedwords.addAll(l);
		}
		
		QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", analyzer);
		ScoreDoc[] scoreDocs = null;
				
		List<SuggestWord> suggestedwordsList = new ArrayList<SuggestWord>(suggestedwords);
		Collections.sort(suggestedwordsList);
		
		int i=0;
		
		for (SuggestWord suggestWord : suggestedwordsList) {
			
			if (i > 70)
				break;			
			
			String queryS = "id:" + suggestWord.eid;
			TopDocs docs = tac.kbp.utils.Definitions.searcher.search(queryParser.parse(queryS), 1);
			
			if (docs.totalHits == 0) {
				continue;				
			}
			
			else {
				scoreDocs = docs.scoreDocs; 
				Document doc = tac.kbp.utils.Definitions.searcher.doc(scoreDocs[0].doc);
				Candidate c = new Candidate(doc,scoreDocs[0].doc);
				c.features.lucene_score = scoreDocs[0].score; 
				q.candidates.add(c);
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

	private static HashMap<String, HashSet<String>> generateQuery(KBPQuery q) {
		
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