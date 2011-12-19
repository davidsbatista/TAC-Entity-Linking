package tac.kbp.bin;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import tac.kbp.queries.KBPQuery;
import tac.kbp.ranking.LogisticRegressionLingPipe;
import tac.kbp.ranking.SVMRank;
import tac.kbp.utils.lda.SupportDocLDA;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		// create Options object
		Options options = new Options();

		// add options
		options.addOption("train", false, "train a ranking model");
		options.addOption("test", false,  "test a trained ranking model");
		//options.addOption("run", false, "train a ranking model and test it");
		//options.addOption("vectors", true, "test from a directory with extracted feature vectors");
		options.addOption("recall", false, "used to tune the recall");
				
		// add argument options
		Option queriesTrain = OptionBuilder.withArgName("queriesTrain").hasArg().withDescription("XML file containing queries for trainning").create( "queriesTrain" );
		Option queriesTest = OptionBuilder.withArgName("queriesTest").hasArg().withDescription("XML file containing queries for testing").create( "queriesTest" );
		Option vectors = OptionBuilder.withArgName("vectors").hasArg().withDescription("directory with extracted feature vectors").create( "vectors" );
		Option model = OptionBuilder.withArgName("model").hasArg().withDescription("<baseline|svmrank|logistic>").create( "model" );
		Option modelFile = OptionBuilder.withArgName("filename").hasArg().withDescription("filename to save model").create("modelFilename");
		Option n_candidates = OptionBuilder.withArgName("candidates").hasArg().withDescription("number of candidates to retrieve per sense").create("candidates");
		
		options.addOption(queriesTrain);
		options.addOption(queriesTest);		
		options.addOption(vectors);
		options.addOption(model);
		options.addOption(n_candidates);
		
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
		
		/*
		else if (line.hasOption("test")) {
				test(args, line);
		}
		*/
			
		else if (line.hasOption("recall")) {
				recall(line);
		}
		
		/*
		else if (line.hasOption("vectors")) {
			
			//load queries
			tac.kbp.bin.Definitions.loadAll(args[2]);
			
			statisticsRecall(false);

			//read model from disk
			LogisticRegressionLingPipe regression = new LogisticRegressionLingPipe(Train.inputs, Train.outputs);
			regression.readModel(args[1]);
						
			//file to write output results
			FileWriter fstream = new FileWriter("results.txt");
			BufferedWriter out = new BufferedWriter(fstream);
			
			//apply the model to feature vectors
			for (KBPQuery query : Definitions.queriesTrain) {
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
		*/
		}
		
		//close indexes
		Definitions.searcher.close();
		if (!line.hasOption("recall")) 
			Definitions.documents.close();
	}
	
	static void recall(CommandLine line) throws Exception {
		
		tac.kbp.bin.Definitions.loaddRecall(line.getOptionValue("queries"), line.getOptionValue("candidates"));
		Train.statisticsRecall();
	}

	static void train(CommandLine line) throws Exception, IOException {
		
		tac.kbp.bin.Definitions.loadAll(line);
		
		System.out.println("\nProcessing training queries:");
		Train.process(Definitions.queriesTrain);
		
		System.out.println("\n\nProcessing test queries:");
		Train.process(Definitions.queriesTest);
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();
		
		System.out.println("\nGenerating features for training queries:");
		Train.generateFeatures(Definitions.queriesTrain);
		
		System.out.println("\nGenerating features for test queries:");
		Train.generateFeatures(Definitions.queriesTest);
		
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
		
		// SVMRank
		else if (line.getOptionValue("model").equalsIgnoreCase("svmrank")) {
			
			System.out.println();
			
			//TODO: catch stdoutput, stderror		
			SVMRank svmrank = new SVMRank();
			Runtime runtime = Runtime.getRuntime();
			String learn_arguments = "-c 3 svmrank-train.dat svmrank-trained-model.dat";
			String classify_arguments = "svmrank-test.dat svmrank-trained-model.dat svmrank-predictions";
			
			//Train a model
			System.out.println("Training SVMRank model: ");
			System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklLearn+' '+learn_arguments);
			
			//generate train features for SVMRank
			svmrank.svmRankFormat(Definitions.queriesTrain, Definitions.queriesGoldTrain,"svmrank-train.dat");
			
			//call SVMRank
			Process svmLearn = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklLearn+' '+learn_arguments);

			//Test the trained model
			System.out.println("Testing SVMRank model: ");
			System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
			
			//generate test features for SVMRank
			svmrank.svmRankFormat(Definitions.queriesTest, Definitions.queriesGoldTest,"svmrank-test.dat");
			
			//call SVMRank
			Process svmClassify = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
			
			//calculate accuracy
			String predictionsFilePath = "svmrank-predictions";
			String goundtruthFilePath = "svmrank-test.dat";
			SVMRankResults.results(predictionsFilePath,goundtruthFilePath);
 		}
		// Baseline
		else if (line.getOptionValue("model").equalsIgnoreCase("baseline")) {
			
			System.out.println("\n\nProcessing test queries:");
			Train.process(Definitions.queriesTest);
			
			System.out.println("\nGetting candidates from Lucene:");
			
			for (KBPQuery q: Definitions.queriesTest) {
				Train.retrieveCandidates(q);
			}
			
			//rank candidates according to Lucene score
			
			
			//produce answers based on lucene ranking
			
		}

	}
	
	/*
	static void test(String[] args, CommandLine line) throws Exception, IOException, ClassNotFoundException {
		
		tac.kbp.bin.Definitions.loadAll(line.getOptionValue("queries"));
		
		//load queries gold standard
		System.out.println(tac.kbp.bin.Definitions.queriesGoldTrain.size() + " gold standard queries loaded");

		LogisticRegressionLingPipe regression = new LogisticRegressionLingPipe();
		
		//read model from disk
		regression.readModel(line.getOptionValue("filename"));
		
		//load features vectors with features already extracted, filename is query name;
		//construct a KBPQuery with candidates and features for each candidate;
		//add it to queries collection
		Definitions.queriesTrain = new LinkedList<KBPQuery>();
		regression.loadVectors(args[2]);
		
		System.out.println(Definitions.queriesTrain.size() + " queries loaded from feature vectors");
		
		//file to write output results
		FileWriter fstream = new FileWriter("results.txt");
		BufferedWriter out = new BufferedWriter(fstream);
		
		//apply the model to feature vectors
		for (KBPQuery query : Definitions.queriesTrain) {
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
	*/

	
}