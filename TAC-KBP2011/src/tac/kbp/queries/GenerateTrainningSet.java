package tac.kbp.queries;

import java.util.ArrayList;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import tac.kbp.utils.Definitions;

public final class GenerateTrainningSet {
	
	static ArrayList<double[]> inputs = new ArrayList<double[]>();
	static ArrayList<Integer> outputs = new ArrayList<Integer>();
	
	public static void generateFeatures() throws Exception {

		for (KBPQuery query : Definitions.queries) {
			
			String correctAnswer =  Definitions.queriesGold.get(query.query_id).answer;
			boolean foundAnswer = false;
			
			System.out.println("Extracting features from candidates");
			
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
			
			/* falta abrir o indice aqui! */
			
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
			getVectors();
			}
	}	

	public static void getVectors() {
		
		Integer[] outputInt;
		outputInt = (Integer[]) outputs.toArray();
		int z=0;
		
		for (double[] vector : inputs) {
			for (int i = 0; i < vector.length; i++) {
				System.out.print(vector[i] + " ");
			}
			System.out.println(" " + outputInt[z]);
			z++;
		}
	
	}
}




