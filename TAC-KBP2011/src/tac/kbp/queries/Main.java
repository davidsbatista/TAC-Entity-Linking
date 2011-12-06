package tac.kbp.queries;

import java.util.Iterator;

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
			
			tac.kbp.utils.Definitions.loadAll(args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
			
			System.out.println(tac.kbp.utils.Definitions.queries.size() + " queries loaded");
			System.out.println(tac.kbp.utils.Definitions.stop_words.size() + " stopwords loaded");
			System.out.println(tac.kbp.utils.Definitions.queriesGold.size() + " queries gold standard loaded");
			
			for (Iterator<KBPQuery> iterator = tac.kbp.utils.Definitions.queries.iterator(); iterator.hasNext();) {
				KBPQuery query = (KBPQuery) iterator.next();
				
				System.out.print(query.query_id + " \"" + query.name + '"');
				Train.getSenses(Definitions.binaryjedis, query);
				Train.processQuery(query);		
			}
			
			float miss_rate = (float) Train.MISS_queries / ((float) tac.kbp.utils.Definitions.queries.size());
			float found_rate = (float) Train.FOUND_queries / ((float) tac.kbp.utils.Definitions.queries.size());
			
			System.out.println("Documents Retrieved: " + Integer.toString(Train.total_n_docs));
			System.out.println("Queries: " + Integer.toString(tac.kbp.utils.Definitions.queries.size()));
			System.out.println("Docs p/ query: " + ( (float) Train.total_n_docs / (float) tac.kbp.utils.Definitions.queries.size()));
			System.out.println("Queries with 0 docs retrieved: " + Integer.toString(Train.n_queries_zero_docs));
			System.out.println("Queries NIL: " + Train.NIL_queries);
			System.out.println("Queries Misses: " + Train.MISS_queries + " (" + miss_rate * 100 + "%)" );
			System.out.println("Queries Found: " + Train.FOUND_queries + " (" + found_rate * 100 + "%)" );
			
			LogisticRegressionLingPipe trainning = new LogisticRegressionLingPipe();
			trainning.generateFeatures();
			trainning.trainModel();
			
			//save model to disk
			trainning.writeModel("linear-regression");
			
			//close indexes and REDIS connection
			Definitions.searcher.close();
			Definitions.documents.close();
			Definitions.binaryjedis.disconnect();
		}
		
		else if (args[0].equalsIgnoreCase("test")) {
			
			//read model from disk
			LogisticRegressionLingPipe trainning = new LogisticRegressionLingPipe();
			trainning.readModel("linear-regression");
			
			//load feature vectors
			trainning.loadVectors(args[1]);			
			trainning.applyTrainedModel();
		}
		
		else if (args[0].equalsIgnoreCase("ldatopics"))
			SupportDocLDA.process(args[1],args[2],args[3],args[4]);
		
		else usage();
	}
	
	public static void usage(){
		System.out.println("Usage:");
		System.out.println("  train queriesPath stopWordsFile goldStandardPath kbIndex spellCheckerIndex dcIndex queries_lda_topics kb_lda_topics");
		System.out.println("  test vectorsPath");
		System.out.println("  ldatopics queriesPath stopwords dcIndex outputfile");
		System.out.println();
	}
}