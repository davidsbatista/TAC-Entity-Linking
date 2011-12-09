package tac.kbp.queries;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import tac.kbp.ranking.LogisticRegressionLingPipe;
import tac.kbp.utils.Definitions;
import tac.kbp.utils.lda.SupportDocLDA;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		if (args.length == 0) {
			usage();
			System.exit(0);
		}
		
		if (args[0].equalsIgnoreCase("train")) {
			
			tac.kbp.utils.Definitions.loadAll(args[1]);
			
			System.out.println(tac.kbp.utils.Definitions.queries.size() + " queries loaded");
			System.out.println(tac.kbp.utils.Definitions.stop_words.size() + " stopwords loaded");
			System.out.println(tac.kbp.utils.Definitions.queriesGold.size() + " queries gold standard loaded");
						
			for (Iterator<KBPQuery> iterator = tac.kbp.utils.Definitions.queries.iterator(); iterator.hasNext();) {
				KBPQuery query = (KBPQuery) iterator.next();
				
				System.out.print(query.query_id + " \"" + query.name + '"');
				Train.getSenses(Definitions.binaryjedis, query);
				Train.processQuery(query);		
			}
						
			float miss_rate = (float) Train.MISS_queries / ((float) tac.kbp.utils.Definitions.queries.size()-Train.NIL_queries);
			float coverage = (float) Train.FOUND_queries / ((float) tac.kbp.utils.Definitions.queries.size()-Train.NIL_queries);
			
			System.out.println("Documents Retrieved: " + Integer.toString(Train.total_n_docs));
			System.out.println("Queries: " + Integer.toString(tac.kbp.utils.Definitions.queries.size()));
			System.out.println("Docs p/ query: " + ( (float) Train.total_n_docs / (float) tac.kbp.utils.Definitions.queries.size()));
			System.out.println("Queries with 0 docs retrieved: " + Integer.toString(Train.n_queries_zero_docs));
			System.out.println("Queries NIL: " + Train.NIL_queries);
			System.out.println("Queries Not Found (Miss Rate): " + Train.MISS_queries + " (" + miss_rate * 100 + "%)" );
			System.out.println("Queries Found (Coverage): " + Train.FOUND_queries + " (" + coverage * 100 + "%)" );

			//training logistic regression model
			LogisticRegressionLingPipe trainning = new LogisticRegressionLingPipe(Train.inputs, Train.outputs);
			trainning.trainModel();
			
			//save model to disk
			trainning.writeModel("linear-regression");
			
			//close indexes and REDIS connection
			Definitions.searcher.close();
			Definitions.documents.close();
			Definitions.binaryjedis.disconnect();
		}
		
		else if (args[0].equalsIgnoreCase("testVectors")) {
			
			//load queries gold standard
			Definitions.loadGoldStandard(args[3]);
			System.out.println(tac.kbp.utils.Definitions.queriesGold.size() + " gold standard queries loaded");

			LogisticRegressionLingPipe regression = new LogisticRegressionLingPipe();
			
			//read model from disk
			regression.readModel(args[1]);
			
			//load features vectors with features already extracted, filename is query name;
			//construct a KBPQuery with candidates and features for each candidate;
			//add it to queries collection
			Definitions.queries = new LinkedList<KBPQuery>();
			regression.loadVectors(args[2]);
			
			System.out.println(Definitions.queries.size() + " queries loaded from feature vectors");
			
			//file to write output results
			FileWriter fstream = new FileWriter("results.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			
			//apply the model to feature vectors
			for (KBPQuery query : Definitions.queries) {
				for (Candidate c: query.candidates) {
					regression.applyTrainedModelCandidate(c);
				}
				
				//rank candidates according to conditional_probabilities
				query.candidatesRanked = new ArrayList<Candidate>(query.candidates);
				Collections.sort(query.candidatesRanked, new CandidateComparator());
				
				System.out.println(query.query_id);
				System.out.println("candidates: " + query.candidates.size());
				System.out.println("candidates ranked: " + query.candidatesRanked.size());
				
				//write results to file
				if (query.candidatesRanked.size() != 0) {			
					out.write(query.query_id+'\t'+query.candidatesRanked.get(0).entity.id +"\n");
				}
				else
					out.write(query.query_id+"\tNIL\n");
			}
			out.close();
		}
		
		
		else if (args[0].equalsIgnoreCase("test")) {
			
			//load queries
			tac.kbp.utils.Definitions.loadAll(args[2]);
			
			System.out.println(tac.kbp.utils.Definitions.queries.size() + " queries loaded");
			System.out.println(tac.kbp.utils.Definitions.queriesGold.size() + " gold standard queries loaded");
			System.out.println(tac.kbp.utils.Definitions.stop_words.size() + " stopwords loaded");			
			
			//get candidates and extract features
			for (Iterator<KBPQuery> iterator = tac.kbp.utils.Definitions.queries.iterator(); iterator.hasNext();) {
				KBPQuery query = (KBPQuery) iterator.next();
				
				System.out.print(query.query_id + " \"" + query.name + '"');
				Train.getSenses(Definitions.binaryjedis, query);
				Train.processQuery(query);		
			}
			
			float miss_rate = (float) Train.MISS_queries / ((float) tac.kbp.utils.Definitions.queries.size());
			float coverage = (float) Train.FOUND_queries / ((float) tac.kbp.utils.Definitions.queries.size());
			
			System.out.println("Documents Retrieved: " + Integer.toString(Train.total_n_docs));
			System.out.println("Queries: " + Integer.toString(tac.kbp.utils.Definitions.queries.size()));
			System.out.println("Docs p/ query: " + ( (float) Train.total_n_docs / (float) tac.kbp.utils.Definitions.queries.size()));
			System.out.println("Queries with 0 docs retrieved: " + Integer.toString(Train.n_queries_zero_docs));
			System.out.println("Queries NIL: " + Train.NIL_queries);
			System.out.println("Queries Not Found (Miss Rate): " + Train.MISS_queries + " (" + miss_rate * 100 + "%)" );
			System.out.println("Queries Found (Coverage): " + Train.FOUND_queries + " (" + coverage * 100 + "%)" );

			//read model from disk
			LogisticRegressionLingPipe regression = new LogisticRegressionLingPipe(Train.inputs, Train.outputs);
			regression.readModel(args[1]);
						
			//file to write output results
			FileWriter fstream = new FileWriter("results.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			
			//apply the model to feature vectors
			for (KBPQuery query : Definitions.queries) {
				for (Candidate c: query.candidates) {
					regression.applyTrainedModelCandidate(c);
				}
				
				//rank candidates according to conditional_probabilities
				query.candidatesRanked = new ArrayList<Candidate>(query.candidates);
				Collections.sort(query.candidatesRanked, new CandidateComparator());
				
				if (query.candidatesRanked.size() != 0) {
					//write results to file			
					out.write(query.query_id+'\t'+query.candidatesRanked.get(0).entity.id +"\n");
				}
				else
					out.write(query.query_id+"\tNIL\n");
			}
			out.close();
		}
		
		else if (args[0].equalsIgnoreCase("ldatopics"))
			SupportDocLDA.process(args[1],args[2],args[3],args[4]);
		
		else usage();
	}
	
	public static void usage(){
		System.out.println("Usage:");
		System.out.println("\t -train [logistic|svmrank] -queries <file>");
		System.out.println("\t -test [logistic|svmrank] -model <file> -queries <dir> -results <file>"); 
		System.out.println("\t -test model vectorsPath goldStandardPath");
		System.out.println("\t ldatopics queriesPath stopwords dcIndex outputfile");
		System.out.println();
	}
}