package tac.kbp.ranking;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.candidates.Candidate;
import tac.kbp.queries.candidates.CandidateComparator;
import tac.kbp.utils.misc.BigFile;

public class SVMRankOutputResults {

	static HashMap<Integer, Double> predictions = new HashMap<Integer, Double>();
	
	static List<KBPQuery> queries = new LinkedList<KBPQuery>();
	
	public static void results(String predictionsFilePath, String goundtruthFilePath) throws Exception {
		parse(predictionsFilePath,goundtruthFilePath);
		
		float mean_reciprocalRank = 0; 
		
		//rank candidates according to classification scores
		for (KBPQuery q : queries) {
			q.candidatesRanked = new ArrayList<Candidate>(q.candidates);
			Collections.sort(q.candidatesRanked, new CandidateComparator());
			
			float reciprocalRank = q.reciprocalRank();
			
			//System.out.println(q.query_id + "\t\t" + "Answer:" + q.gold_answer + "\t\t" + "reciprocal rank: " +reciprocalRank);
			mean_reciprocalRank += reciprocalRank;
			/*
			for (Candidate c : q.candidatesRanked) {
				System.out.print(c.entity.id + '\t' + c.conditionalProbabilities[1] + "\n");
			}
			*/
		}
		System.out.println("Mean Reciprocal Rank: " + mean_reciprocalRank / queries.size());
		generateOutput("results-SVMRank.txt");
	}
	
	static void generateOutput(String output) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (KBPQuery q : queries) {
			
			if (q.candidatesRanked.size()==0) {
				out.println(q.query_id.trim()+"\tNIL");
			}
			
			// if inDegree is 0 then return NIL
			else if (q.candidatesRanked.get(0).features.inDegree == 0 || q.candidatesRanked.get(0).features.outDegree == 0) {
				out.println(q.query_id.trim()+"\tNIL");
			}
			
			else {
				out.println(q.query_id.trim()+"\t"+q.candidatesRanked.get(0).entity.id);
			}
		}
		out.close();		
	}

	public static void parse(String predictionsFilePath, String goundtruthFilePath) throws Exception{
		
		BigFile predictionsFile = new BigFile(predictionsFilePath);
		BigFile goundtruthFile = new BigFile(goundtruthFilePath);
		int i=0;
		
		for (String line : predictionsFile) {
			predictions.put(i, Double.parseDouble(line));
			i++;
		}
		
		i=0;
		KBPQuery q = null;			
		for (String line: goundtruthFile) {
			
			if (line.startsWith("#")) {
				if (q!=null) {
					queries.add(q);
				}
				//Extracts the query ID and the correct entity's ID
				//#EL00003 E0182788
				String[] data = line.split("\\s");
				String query_id = data[0].split("#")[1];
				String correct_answer = data[1];
					
				//Builds new query and fills the query_id and correct answer
				q = new KBPQuery();
				q.query_id = query_id;
				q.gold_answer = correct_answer;

				continue; //to avoid increment of i
			}
			
			else  {
				String[] data = line.split("\\s");
				
				//entity's id at the end of the line #E0554903
				String entity_id = data[(data.length)-1].split("#")[1];
				
				Candidate c = new Candidate();
				
				c.entity.id = entity_id;	//.split("#")[1];
				c.features.outDegree = (int) Double.parseDouble( data[24].split(":")[1] );
				c.features.inDegree = (int) Double.parseDouble( data[25].split(":")[1] );
				c.conditionalProbabilities[1] = predictions.get(i);
				q.candidates.add(c);
				
			}
			i++;				
		}
		//to add last query
		queries.add(q);
	}
}

