package tac.kbp.ranking;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import tac.kbp.queries.Candidate;
import tac.kbp.queries.KBPQuery;
import tac.kbp.utils.Definitions;

public final class GenerateTrainningSet {
	
	public static ArrayList<double[]> inputs = new ArrayList<double[]>();
	public static ArrayList<Integer> outputs = new ArrayList<Integer>();
	
	public static void generateFeatures() throws Exception {

		for (KBPQuery query : Definitions.queries) {
			
			String correctAnswer =  Definitions.queriesGold.get(query.query_id).answer;
			boolean foundAnswer = false;
			
			System.out.print("Extracting features from candidates for query " + query.query_id);
			
			for (Candidate c : query.candidates) {
				System.out.print(".");
				c.extractFeatures(query);
				if (c.entity.id.equalsIgnoreCase(correctAnswer)) {
					c.features.correct_answer = true;
					foundAnswer = true;
				}
				else c.features.correct_answer = false;
				
				inputs.add(c.features.inputVector());
				outputs.add(c.features.output());
				
			}
			
			if (!foundAnswer) {
				//get answer doc from KB				
				QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", new WhitespaceAnalyzer());
				ScoreDoc[] scoreDocs = null;
				String queryS = "id:" + correctAnswer;
				
				TopDocs docs = tac.kbp.utils.Definitions.searcher.search(queryParser.parse(queryS), 1);				
				scoreDocs = docs.scoreDocs;
				
				if (docs.totalHits != 0) {
					Document doc = tac.kbp.utils.Definitions.searcher.doc(scoreDocs[0].doc);
					
					//extract features
					Candidate c = new Candidate(doc,scoreDocs[0].doc);
					c.features.lucene_score = scoreDocs[0].score; 
					c.extractFeatures(query);
					inputs.add(c.features.inputVector());
					outputs.add(c.features.output());
					}
				}
			System.out.println();
			getVectors(query.query_id);
			}
	}
	
	public static void getVectors(String query_id) throws FileNotFoundException {	
		int z=0;
		PrintStream out = new PrintStream( new FileOutputStream(query_id+".txt"));		
		for (double[] vector : inputs) {
			for (int i = 0; i < vector.length; i++) {
				out.print(vector[i] + ",");
			}
			out.println(outputs.get(z));
			z++;
		}
		out.close();
	}

	
}




