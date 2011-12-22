package tac.kbp.bin;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import tac.kbp.kb.index.spellchecker.SuggestWord;
import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.xml.ParseQueriesXMLFile;
import tac.kbp.ranking.LogisticRegressionLingPipe;
import tac.kbp.ranking.SVMRank;
import tac.kbp.ranking.SVMRankOutputResults;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		// create Options object
		Options options = new Options();

		// add options
		options.addOption("train", false, "train a ranking model");
		options.addOption("test", false,  "test a trained ranking model");
		options.addOption("baseline", false, "only uses Lucene to generate answers");
		options.addOption("recall", false, "used to tune the recall");
		options.addOption("svmresults", false, "output results from SVMRank output");		
		
		// add argument options
		Option queriesTrain = OptionBuilder.withArgName("queriesTrain").hasArg().withDescription("XML file containing queries for trainning").create( "queriesTrain" );
		Option queriesTest = OptionBuilder.withArgName("queriesTest").hasArg().withDescription("XML file containing queries for testing").create( "queriesTest" );
		//Option vectors = OptionBuilder.withArgName("vectors").hasArg().withDescription("directory with extracted feature vectors").create( "vectors" );
		Option model = OptionBuilder.withArgName("model").hasArg().withDescription("<baseline|svmrank|logistic>").create( "model" );
		Option modelFile = OptionBuilder.withArgName("filename").hasArg().withDescription("filename to save model").create("modelFilename");
		Option n_candidates = OptionBuilder.withArgName("candidates").hasArg().withDescription("number of candidates to retrieve per sense").create("candidates");
		Option directory = OptionBuilder.withArgName("dir").hasArg().withDescription("directory containning: svmrank-test.dat, svmrank-train.dat, svmrank-trained-model.dat").create("dir");
		
		options.addOption(queriesTrain);
		options.addOption(queriesTest);		
		options.addOption(directory);
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
			if (line.hasOption("train")) train(line);	
			else if (line.hasOption("recall")) recall(line);
			else if (line.hasOption("baseline")) baseline(line);
			else if (line.hasOption("svmresults")) svmresults(line);
			
			//close indexes
			if (line.hasOption("train") || line.hasOption("baseline") ) {
				Definitions.searcher.close();
				Definitions.documents.close();
			}
		}
	}
	
	static void recall(CommandLine line) throws Exception {
		
		tac.kbp.bin.Definitions.loaddRecall(line.getOptionValue("queries"), line.getOptionValue("candidates"));
		Train.statisticsRecall();
	}
	
	static void baseline(CommandLine line) throws Exception {
		
		/* Lucene Index */		
		Definitions.loadKBIndex();
		
		/* SpellChecker Index */
		Definitions.loadSpellCheckerIndex();
		
		/* REDIS connection */
		Definitions.connectionREDIS();
		
		System.out.println();
		
		/* Queries XML file */
		String queriesTrainFile = line.getOptionValue("queriesTrain");
		System.out.println("Loading queries from: " + queriesTrainFile);
		Definitions.queriesTrain = ParseQueriesXMLFile.loadQueries(queriesTrainFile);
		
		/* Queries answers file */
		Definitions.queriesAnswersTrain = Definitions.loadQueriesAnswers(queriesTrainFile);
		
		/* set the answer for queries*/
		for (KBPQuery q : Definitions.queriesTrain) {
			q.gold_answer = Definitions.queriesAnswersTrain.get(q.query_id).answer;
		}	
		
		/* Start processing queries: get alternative names */
		System.out.println("\n\nProcessing test queries:");
		Train.process(Definitions.queriesTrain,false,false);
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();
		
		System.out.println("\n\nGetting candidates from Lucene:");
			
		for (KBPQuery q: Definitions.queriesTrain) {			
			List<SuggestWord> suggested = Train.queryKB(q);
			q.suggestedwords = suggested;
			System.out.print("\n"+q.query_id + " \"" + q.name + '"' + "\t" + q.suggestedwords.size());
		}
		
		//produce answers based on lucene ranking
		String output = "results.txt";
		PrintStream out = new PrintStream( new FileOutputStream(output));
			
		for (KBPQuery q : Definitions.queriesTrain) {
			if (q.suggestedwords.size()==0) {
				out.println(q.query_id.trim()+"\tNIL");
			}
			else {
				out.println(q.query_id.trim()+"\t"+q.suggestedwords.get(0).eid);
			}
		}
		out.close();			
	}
	
	static void train(CommandLine line) throws Exception, IOException {
		
		/* Lucene Index */		
		Definitions.loadKBIndex();
		
		/* SpellChecker Index */
		Definitions.loadSpellCheckerIndex();
		
		/* Document Collection */
		Definitions.loadDocumentCollecion();
		
		System.out.println();
		
		/* REDIS connection */
		Definitions.connectionREDIS();
		
		System.out.println();
		
		/* Stop words */
		Definitions.loadStopWords();
		
		/* Stanford NER */
		Definitions.loadClassifier(Definitions.serializedClassifier);
		
		/* LDA Knowledge Base */
		System.out.println("Load KB LDA topics ...");
		Definitions.loadLDATopics(Definitions.kb_lda_topics, Definitions.kb_topics);
		
		/* Dictionary of name-entities based on the Knowledge Base */
		Definitions.buildDictionary();
		
		/* Train queries XML file */
		String queriesTrainFile = line.getOptionValue("queriesTrain");
		System.out.println("Loading queries from: " + queriesTrainFile);
		Definitions.queriesTrain = ParseQueriesXMLFile.loadQueries(queriesTrainFile);
		
		/* Queries answers file */
		Definitions.queriesAnswersTrain = Definitions.loadQueriesAnswers(queriesTrainFile);
		
		/* set the answer for queries*/
		for (KBPQuery q : Definitions.queriesTrain) {
			q.gold_answer = Definitions.queriesAnswersTrain.get(q.query_id).answer;
		}
		
		/* LDA Train Queries */
		Definitions.determineLDAFile(queriesTrainFile);
		
		System.out.println("\nProcessing training queries:");
		Train.process(Definitions.queriesTrain, true, true);
		
		System.out.println();
		//TODO: save all extracted features to disk
		
		/* Test queries XML file */
		String queriesTestFile = line.getOptionValue("queriesTest");
		System.out.println("\nLoading queries from: " + queriesTestFile);
		Definitions.queriesTest = ParseQueriesXMLFile.loadQueries(queriesTestFile);
		
		/* Queries answers file */
		Definitions.queriesAnswersTest = Definitions.loadQueriesAnswers(queriesTestFile);
		
		/* set the answer for queries*/
		for (KBPQuery q : Definitions.queriesTest) {
			q.gold_answer = Definitions.queriesAnswersTest.get(q.query_id).answer;
		}
		
		/* LDA Test Queries */
		Definitions.determineLDAFile(queriesTestFile);		
		System.out.println("\n\nProcessing test queries:");
		Train.process(Definitions.queriesTest, true, true);
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();
				
		System.out.println("\nGenerating features for training queries:");
		Train.generateFeatures(Definitions.queriesTrain);
		
		System.out.println("\nGenerating features for test queries:");
		
		/* LDA Test Queries */
		Definitions.determineLDAFile(queriesTestFile);
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
			svmrank.svmRankFormat(Definitions.queriesTrain, Definitions.queriesAnswersTrain,"svmrank-train.dat");
			
			//free memory for Train queries data
			Definitions.queriesTrain = null;
			
			//call SVMRank
			Process svmLearn = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklLearn+' '+learn_arguments);
			svmLearn.waitFor();

			
			//Test the trained model
			System.out.println("\nTesting SVMRank model: ");
			System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
			
			//generate test features for SVMRank
			svmrank.svmRankFormat(Definitions.queriesTest, Definitions.queriesAnswersTest,"svmrank-test.dat");
			
			//free memory for Test queries data
			Definitions.queriesTest = null;
			
			//call SVMRank
			Process svmClassify = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
			svmClassify.waitFor();
			
			//calculate accuracy
			String predictionsFilePath = "svmrank-predictions";
			String goundtruthFilePath = "svmrank-test.dat";
			SVMRankOutputResults.results(predictionsFilePath,goundtruthFilePath);
 		}
	}

	static void svmresults(CommandLine line) throws Exception {
		
		String path = line.getOptionValue("dir");
		
		String goundtruthFilePath = path+"/svmrank-test.dat";
		String predictionsFilePath = path+"/svmrank-predictions";		
		SVMRankOutputResults.results(predictionsFilePath, goundtruthFilePath);
	}
	
	
}




