package tac.kbp.bin;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.candidates.Candidate;
import tac.kbp.queries.candidates.CandidateComparator;
import tac.kbp.ranking.LogisticRegressionLingPipe;
import tac.kbp.ranking.SVMRank;
import tac.kbp.utils.lda.SupportDocLDA;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		// create Options object
		Options options = new Options();

		// add options
		options.addOption("train", false, "train a ranking model");
		options.addOption("test", false, "test a trained ranking model");
		options.addOption("vectors", true, "test from a directory with extracted feature vectors");
		options.addOption("recall", false, "to tune the recall");
				
		// add argument options
		Option queries = OptionBuilder.withArgName("queries").hasArg().withDescription("XML file containing the queries").create( "queries" );
		Option vectors = OptionBuilder.withArgName("vectors").hasArg().withDescription("directory with extracted feature vectors").create( "vectors" );
		Option model = OptionBuilder.withArgName("model").hasArg().withDescription("svmrank or logistic").create( "model" );
		Option modelFile = OptionBuilder.withArgName("filename").hasArg().withDescription("filename to save model").create("modelFilename");
		
		options.addOption(queries);
		options.addOption(vectors);
		options.addOption(model);
		
		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse( options, args );
		
		if (args.length == 0) {
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(" ", options );
			System.exit(0);
		}
		
		else {
			if (line.hasOption("train")) {
				train(line);
		}
		
		else if (line.hasOption("test")) {
				test(args, line);
		}
			
		else if (line.hasOption("recall")) {
				recall(line);
		}
		
		else if (line.hasOption("vectors")) {
			
			//load queries
			tac.kbp.bin.Definitions.loadAll(args[2]);
			
			generateCandidates(false);

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
		}
	}
	
	static void recall(CommandLine line) throws Exception {
		
		tac.kbp.bin.Definitions.loaddRecall(line.getOptionValue("queries"));
		generateCandidates(true);
		
	}

	static void train(CommandLine line) throws Exception, IOException {
		
		tac.kbp.bin.Definitions.loadAll(line.getOptionValue("queries"));			
		
		generateCandidates(false);
		
		// to train a logistic regression model
		if (line.getOptionValue("model").equalsIgnoreCase("logistic")) {
			
			//training logistic regression model
			LogisticRegressionLingPipe trainning = new LogisticRegressionLingPipe(Train.inputs, Train.outputs);
			trainning.trainModel();
			
			//save model to disk
			String filename = line.getOptionValue("modelFilename");
			
			if (!filename.equalsIgnoreCase("")) {
				trainning.writeModel(filename);
			}
			
			else trainning.writeModel("linear-regression");

		}
		
		// to train a SVMRank model
		else if (line.getOptionValue("model").equalsIgnoreCase("svmrank")) {
			SVMRank svmrank = new SVMRank();
			svmrank.svmRankFormat(Definitions.queries, "svmrank-train.dat");
			
			//TODO
			//automatizar o processo
			//svm_rank_learn -c 3 example3/train.dat example3/model
			//svm_rank_classify -c 3 example3/train.dat example3/model
 
			
		}
	}

	static void test(String[] args, CommandLine line) throws Exception, IOException, ClassNotFoundException {
		
		tac.kbp.bin.Definitions.loadAll(line.getOptionValue("queries"));
		
		//load queries gold standard
		System.out.println(tac.kbp.bin.Definitions.queriesGold.size() + " gold standard queries loaded");

		LogisticRegressionLingPipe regression = new LogisticRegressionLingPipe();
		
		//read model from disk
		regression.readModel(line.getOptionValue("filename"));
		
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

	static void generateCandidates(boolean tune) throws Exception {
		
		//process each query
		for (KBPQuery q : Definitions.queries) {
			
			System.out.print(q.query_id + "\t \"" + q.name + '"');
			
			//get possible alternative names/senses
			q.getSenses(Definitions.binaryjedis);
			
			if (tune) {
				Train.tune(q);
			}
			else Train.retrieveCandidates(q);					
		}
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();
		
		//close indexes
		Definitions.searcher.close();
		if (!tune) 
			Definitions.documents.close();

		float miss_rate = (float) Train.MISS_queries / ((float) tac.kbp.bin.Definitions.queries.size()-Train.NIL_queries);
		float coverage = (float) Train.FOUND_queries / ((float) tac.kbp.bin.Definitions.queries.size()-Train.NIL_queries);
		
		System.out.println("Documents Retrieved: " + Integer.toString(Train.total_n_docs));
		System.out.println("Queries: " + Integer.toString(tac.kbp.bin.Definitions.queries.size()));
		System.out.println("Docs p/ query: " + ( (float) Train.total_n_docs / (float) tac.kbp.bin.Definitions.queries.size()));
		System.out.println("Queries with 0 docs retrieved: " + Integer.toString(Train.n_queries_zero_docs));
		System.out.println("Queries NIL: " + Train.NIL_queries);
		System.out.println("Queries Not Found (Miss Rate): " + Train.MISS_queries + " (" + miss_rate * 100 + "%)" );
		System.out.println("Queries Found (Coverage): " + Train.FOUND_queries + " (" + coverage * 100 + "%)" );
	}
}