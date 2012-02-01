package tac.kbp.bin;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import tac.kbp.kb.index.spellchecker.SuggestWord;
import tac.kbp.queries.GoldQuery;
import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.candidates.Candidate;
import tac.kbp.queries.candidates.CandidateComparatorInDegree;
import tac.kbp.queries.candidates.CandidateComparatorOutDegree;
import tac.kbp.queries.xml.ParseQueriesXMLFile;
import tac.kbp.ranking.LogisticRegressionLingPipe;
import tac.kbp.ranking.NILDetector;
import tac.kbp.ranking.SVMRank;
import tac.kbp.ranking.SVMRankOutputResults;
import tac.kbp.utils.misc.BigFile;

public class Main {
	
	public static void main(String[] args) throws Exception {
		
		// create Options object
		Options options = new Options();

		// add options
		options.addOption("train", false, "train a ranking model");
		options.addOption("graph", false,  "generate results based only on outLinks and inLinks");
		options.addOption("baseline", false, "only uses Lucene to generate answers");
		options.addOption("recall", false, "used to tune the recall");
		options.addOption("svmresults", false, "output results from SVMRank output");
		options.addOption("extract", false, "extract features from a queries XML file");
		options.addOption("svmrankformat", false, "converts extracted features into one file for SVMRank");
		options.addOption("trainnil", false, "trains a NIL Detector - SVM");
		
		// add argument options
		Option queriesTrain = OptionBuilder.withArgName("queriesTrain").hasArg().withDescription("XML file containing queries for trainning").create( "queriesTrain" );
		Option queriesTest = OptionBuilder.withArgName("queriesTest").hasArg().withDescription("XML file containing queries for testing").create( "queriesTest" );
		Option queries = OptionBuilder.withArgName("queries").hasArg().withDescription("directory with extracted feature").create( "queries" );
		Option model = OptionBuilder.withArgName("model").hasArg().withDescription("<baseline|svmrank|logistic>").create( "model" );
		Option n_candidates = OptionBuilder.withArgName("candidates").hasArg().withDescription("number of candidates to retrieve per sense").create("candidates");
		Option directory = OptionBuilder.withArgName("dir").hasArg().withDescription("directory containning: svmrank-test.dat, svmrank-train.dat, svmrank-trained-model.dat").create("dir");
		//Option basePath = OptionBuilder.withArgName("basePath").hasArg().withDescription("base directory path").create("basePath");
		
		options.addOption(queriesTrain);
		options.addOption(queriesTest);		
		options.addOption(directory);
		options.addOption(model);
		options.addOption(n_candidates);
		options.addOption(queries);
		//options.addOption(basePath);
		
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
			else if (line.hasOption("svmrankformat")) svmrankformat(line);
			else if (line.hasOption("graph")) graph(line);
			else if (line.hasOption("extract")) extract(line);
			else if (line.hasOption("trainnil")) trainNIL(line);
			
			//close indexes
			if (line.hasOption("train")) {
				Definitions.searcher.close();
				Definitions.documents.close();
			}
				
			if (line.hasOption("baseline") ) {
				Definitions.searcher.close();
			}
		}
	}
	
	static void trainNIL(CommandLine line) throws Exception {
		
		// LOAD all queries and extract candidates from KB
		
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
		//Definitions.loadClassifier(Definitions.serializedClassifier);
		
		/* LDA Knowledge Base */
		if (Definitions.topicalSimilarities) {
			System.out.println("Load KB LDA topics ...");
			Definitions.loadLDATopics(Definitions.kb_lda_topics, Definitions.kb_topics);
		}
		
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
		if (Definitions.topicalSimilarities) {
			Definitions.determineLDAFile(queriesTrainFile);
		}
		
		System.out.println("\nProcessing training queries:");
		Train.process(Definitions.queriesTrain, Definitions.topicalSimilarities, true);
		
		System.out.println();
		
		
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
		Train.process(Definitions.queriesTest, Definitions.topicalSimilarities, true);
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();
		
		//TRAINNING
		Train.generateFeatures(Definitions.queriesTrain);
		Train.generateFeatures(Definitions.queriesTest);
		
		SVMRank svmrank = new SVMRank();
		svmrank.svmRankFormat(Definitions.queriesTrain, Definitions.queriesAnswersTrain,"svmrank-train.dat");
		svmrank.svmRankFormat(Definitions.queriesTest, Definitions.queriesAnswersTest,"svmrank-test.dat");
		
		Runtime runtime = Runtime.getRuntime();
		String learn_arguments = "-c 3 svmrank-train.dat svmrank-trained-model.dat";
				
		//Train a model
		System.out.println("Training SVMRank model: ");
		System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklLearn+' '+learn_arguments);
		Process svmRankLearn = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklLearn+' '+learn_arguments);
		svmRankLearn.waitFor();
		
		//apply the trained model to the queries in TrainingQueries in order to get ranking scores and used them for training the NIL Detector
		System.out.println("\nApplying trained model to TrainingQueries to get ranking scores for trainning the NIL Detector");
		String classify_arguments = "svmrank-train.dat svmrank-trained-model.dat svmrank-predictions_training_set";
		System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
		Process svmRankClassify = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
		svmRankClassify.waitFor();
		
		// gather ranking scores from SVMRankOutputResults
		String predictionsFilePath = "svmrank-predictions_training_set";
		String goundtruthFilePath = "svmrank-train.dat";
		SVMRankOutputResults outputresults = new SVMRankOutputResults(); 
		List<KBPQuery> TrainningQueriesScore = outputresults.results(predictionsFilePath,goundtruthFilePath);
		
		for (int i = 0; i < Definitions.queriesTrain.size(); i++) {
			
			// check if queries are the same before getting top-ranked candidate
			if  (TrainningQueriesScore.get(i).query_id.equalsIgnoreCase(Definitions.queriesTrain.get(i).query_id)) {
				
				//create ranked list in queriesTrain with the contents from TrainningQueriesScore
				Definitions.queriesTrain.get(i).candidatesRanked = new ArrayList<Candidate>(TrainningQueriesScore.get(i).candidatesRanked);
			}
		}
		
		System.out.println("extracting features for NIL detector");
		
		//Train NIL-Detector using the ranking scores
		NILDetector SVMNILDetector = new NILDetector();
		SVMNILDetector.train(Definitions.queriesTrain,"NIL_train.dat");
		
		//Test the trained Ranking model on the Test queries
		System.out.println("\nTesting SVMRank model: ");
		classify_arguments = "svmrank-test.dat svmrank-trained-model.dat svmrank-predictions_test_set";
		System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
		svmRankClassify = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
		svmRankClassify.waitFor();
		
		//gather ranking scores from SVMRankOutputResults
		predictionsFilePath = "svmrank-predictions_test_set";
		goundtruthFilePath = "svmrank-test.dat";
		SVMRankOutputResults outputTestresults = new SVMRankOutputResults();
		List<KBPQuery> TestQueriesScore = outputTestresults.results(predictionsFilePath,goundtruthFilePath);

		for (int i = 0; i < Definitions.queriesTest.size(); i++) {
			// check if queries are the same before getting top-ranked candidate
			if  (TestQueriesScore.get(i).query_id.equalsIgnoreCase(Definitions.queriesTest.get(i).query_id)) {				
				//create ranked list in queriesTrain with the contents from TrainningQueriesScore
				Definitions.queriesTest.get(i).candidatesRanked = new ArrayList<Candidate>(TestQueriesScore.get(i).candidatesRanked);
			}
		}
		
		System.out.println("extracting features for NIL detector");

		//Test the trained model
		SVMNILDetector.classify(Definitions.queriesTest,"NIL_test.dat");
		
		//gather results from NIL_predictions
		
		//read "NIL_predictions" line per line
		HashMap<Integer, Double> NIL_predictions = new HashMap<Integer, Double>();
		
		BigFile predictionsFile = new BigFile("NIL_predictions");
		int i=0;
		
		for (String prediction : predictionsFile) {
			NIL_predictions.put(i, Double.parseDouble(prediction));
			i++;
		}
		
		//read NIL_test.dat line by line: get last #EL11051 E0081133
		BigFile queries = new BigFile("NIL_test.dat");
		i=0;
		
		String output = ("results-SVMRank.txt");		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (String query: queries) {
			//System.out.print(NIL_predictions.get(i));
			out.print(query.split("\\#")[1].split("\\s")[0]);
			if (NIL_predictions.get(i)>=1) {
				out.println("\tNIL");
			}
			else {
				String answer = query.split("\\#")[1].split("\\s")[1];
				out.println("\t"+answer);
			}
			i++;
		}
		
		out.close();
	}
	
	static void svmrankformat(CommandLine line) throws IOException {
		
		// directory with features files
		// answers files
		
		String featuresDir = line.getOptionValue("dir");
		String queries = line.getOptionValue("queries");
		SVMRank svmrank = new SVMRank();
		
		//generate train features for SVMRank
		System.out.println("Writing extracted features to SVMRank format:");
		System.out.println("loading features from: " + featuresDir);
		System.out.println("loading answers from: " + queries);
		
		/* Queries answers file */
		HashMap<String, GoldQuery> answers = Definitions.loadQueriesAnswers(queries);
		
		svmrank.svmRankFormat(featuresDir, answers,"svmrank-train.dat");
	}
	
	static void extract(CommandLine line) throws Exception {
		
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
		//Definitions.loadClassifier(Definitions.serializedClassifier);
		
		/* LDA Knowledge Base */
		if (Definitions.topicalSimilarities) {
			System.out.println("Load KB LDA topics ...");
			Definitions.loadLDATopics(Definitions.kb_lda_topics, Definitions.kb_topics);
		}
		
		/* Dictionary of name-entities based on the Knowledge Base */
		Definitions.buildDictionary();
		
		
		/* Train queries XML file */
		String queriesTrainFile = line.getOptionValue("queriesTrain");
		
		if (queriesTrainFile!=null) {
			
			System.out.println("Loading queries from: " + queriesTrainFile);
			Definitions.queriesTrain = ParseQueriesXMLFile.loadQueries(queriesTrainFile);
			
			/* Queries answers file */
			Definitions.queriesAnswersTrain = Definitions.loadQueriesAnswers(queriesTrainFile);
			
			/* set the answer for queries*/
			for (KBPQuery q : Definitions.queriesTrain) {
				q.gold_answer = Definitions.queriesAnswersTrain.get(q.query_id).answer;
			}
			
			/* LDA Train Queries */
			if (Definitions.topicalSimilarities) {
				Definitions.determineLDAFile(queriesTrainFile);
			}
			
			System.out.println("\nProcessing training queries:");
			Train.process(Definitions.queriesTrain, true, true);
		}
		
		System.out.println();
		
		/* Test queries XML file */
		String queriesTestFile = line.getOptionValue("queriesTest");
		
		if (queriesTrainFile!=null) {
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
			Train.generateFeatures(Definitions.queriesTrain);
		}
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();		
		
	}
	
	
	static void recall(CommandLine line) throws Exception {
		
		tac.kbp.bin.Definitions.loadRecall(line.getOptionValue("candidates"));
		
		Definitions.basePath = line.getOptionValue("basePath");		
		System.out.println("using as base path: " + Definitions.basePath);
		
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
		
		System.out.println("\nProcessing queries:");		
		Train.process(Definitions.queriesTest, false, true);
		
		for (KBPQuery q : Definitions.queriesTest) {
			Train.retrieveCandidates(q);
		}
		
		Train.statisticsRecall(Definitions.queriesTest);
	}
	
	
	static void graph(CommandLine line) throws Exception {
		
		//use in-link and out-link measures to rank candidates and find out NIL
		
		/* Lucene Index */		
		Definitions.loadKBIndex();
		
		/* SpellChecker Index */
		Definitions.loadSpellCheckerIndex();
		
		/* Document Collection */
		Definitions.loadDocumentCollecion();
		
		/* Dictionary of name-entities based on the Knowledge Base */
		Definitions.buildDictionary();
		
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
		
		/* REDIS connection */
		Definitions.connectionREDIS();
		
		Train.process(Definitions.queriesTest, false, true);
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();

		// retrieve candidates and calculate in- and outDegree
		int count = 0;
		
		for (KBPQuery q : Definitions.queriesTest) {
			Train.retrieveCandidates(q);
			
			for (Candidate c : q.candidates) {
				c.linkDisambiguation(q);
			}
			
			System.out.println("(" + count + "/" + Definitions.queriesTest.size() + ")\n");
			count++;
		}
		
		// sort according to inDegree
		for (KBPQuery q : Definitions.queriesTest) {
			q.candidatesRanked = new ArrayList<Candidate>(q.candidates);
			Collections.sort(q.candidatesRanked, new CandidateComparatorInDegree());
		}
		generateOutputIn("results-inDegree.txt", Definitions.queriesTest);
		generateOutputBoth("results-ranked-by-inDegree_-NIL-if_any_Degree_is_0.txt", Definitions.queriesTest);
		
		
		// sort according to outDegree
		for (KBPQuery q : Definitions.queriesTest) {
			q.candidatesRanked = new ArrayList<Candidate>(q.candidates);
			Collections.sort(q.candidatesRanked, new CandidateComparatorOutDegree());
		}
		generateOutputOut("results-outDegree.txt", Definitions.queriesTest);
		generateOutputBoth("results-ranked-by-outDegree_-NIL-if_any_Degree_is_0.txt", Definitions.queriesTest);
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
		String queriesTestFile = line.getOptionValue("queriesTest");
		System.out.println("Loading queries from: " + queriesTestFile);
		Definitions.queriesTest = ParseQueriesXMLFile.loadQueries(queriesTestFile);
		
		/* Queries answers file */
		Definitions.queriesAnswersTest = Definitions.loadQueriesAnswers(queriesTestFile);
		
		/* set the answer for queries*/
		for (KBPQuery q : Definitions.queriesTest) {
			q.gold_answer = Definitions.queriesAnswersTest.get(q.query_id).answer;
		}	
		
		/* Start processing queries: get alternative names */
		System.out.println("\n\nProcessing test queries:");
		Train.process(Definitions.queriesTest,false,false);
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();
		
		System.out.println("\n\nGetting candidates from Lucene:");
			
		for (KBPQuery q: Definitions.queriesTest) {			
			List<SuggestWord> suggested = Train.queryKB(q);
			q.suggestedwords = suggested;
			System.out.print("\n"+q.query_id + " \"" + q.name + '"' + "\t" + q.suggestedwords.size());
		}
		
		//produce answers based on lucene ranking
		String output = "results.txt";
		PrintStream out = new PrintStream( new FileOutputStream(output));
			
		for (KBPQuery q : Definitions.queriesTest) {
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
		
		Definitions.basePath = line.getOptionValue("basePath");		
		System.out.println("using as base path: " + Definitions.basePath);
		
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
		//Definitions.loadClassifier(Definitions.serializedClassifier);
		
		/* LDA Knowledge Base */
		if (Definitions.topicalSimilarities) {
			System.out.println("Load KB LDA topics ...");
			Definitions.loadLDATopics(Definitions.kb_lda_topics, Definitions.kb_topics);
		}
		
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
		if (Definitions.topicalSimilarities) {
			Definitions.determineLDAFile(queriesTrainFile);
		}
		
		System.out.println("\nProcessing training queries:");
		Train.process(Definitions.queriesTrain, Definitions.topicalSimilarities, true);
		
		System.out.println();
		
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
		Train.process(Definitions.queriesTest, Definitions.topicalSimilarities, true);
		
		//close REDIS connection
		Definitions.binaryjedis.disconnect();
		
		
		//TRAINING
		System.out.println("\nGenerating features for training queries:");
		Train.generateFeatures(Definitions.queriesTrain);
				
		SVMRank svmrank = new SVMRank();		
		
		//generate train features for SVMRank
		System.out.println("Writing extracted features to SVMRank format:");
		svmrank.svmRankFormat(Definitions.queriesTrain, Definitions.queriesAnswersTrain,"svmrank-train.dat");
		
		//free memory of Train queries data
		Definitions.queriesTrain = null;
		
		//TEST
		System.out.println("\nGenerating features for test queries:");
		Train.generateFeatures(Definitions.queriesTest);
		
		//generate test features for SVMRank
		svmrank.svmRankFormat(Definitions.queriesTest, Definitions.queriesAnswersTest,"svmrank-test.dat");
		
		//free memory of Test queries data
		Definitions.queriesTest = null;
		
		
		// SVMRank
		if (line.getOptionValue("model").equalsIgnoreCase("svmrank")) {
			
			System.out.println();
			
			//TODO: catch stdoutput, stderror			
			Runtime runtime = Runtime.getRuntime();
			String learn_arguments = "-c 3 svmrank-train.dat svmrank-trained-model.dat";
			String classify_arguments = "svmrank-test.dat svmrank-trained-model.dat svmrank-predictions";
			
			//Train a model
			System.out.println("Training SVMRank model: ");
			System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklLearn+' '+learn_arguments);
			
			//call SVMRank
			Process svmLearn = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklLearn+' '+learn_arguments);
			svmLearn.waitFor();
			
			//Test the trained model
			System.out.println("\nTesting SVMRank model: ");
			System.out.println(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
			
			//call SVMRank
			Process svmClassify = runtime.exec(Definitions.SVMRankPath+Definitions.SVMRanklClassify+' '+classify_arguments);
			svmClassify.waitFor();
			
			//calculate accuracy
			String predictionsFilePath = "svmrank-predictions";
			String goundtruthFilePath = "svmrank-test.dat";
			SVMRankOutputResults output = new SVMRankOutputResults();
			output.results(predictionsFilePath,goundtruthFilePath);
 		}
		
		
		// LambdaMART
		else if (line.getOptionValue("model").equalsIgnoreCase("svmrank")) {
			//TODO: add code to use LambdaMART ranking model
		}
		
		//TODO: train a logistic regression model
		else if (line.getOptionValue("model").equalsIgnoreCase("logistic")) {
					
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
	}

	
	static void svmresults(CommandLine line) throws Exception {
			
		String path = line.getOptionValue("dir");
		String goundtruthFilePath = path+"/svmrank-test.dat";
		String predictionsFilePath = path+"/svmrank-predictions";
		SVMRankOutputResults output = new SVMRankOutputResults();
		output.results(predictionsFilePath, goundtruthFilePath);
	}

	
	static void generateOutputIn(String output, List<KBPQuery> queries) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (KBPQuery q : queries) {
			
			if (q.candidatesRanked.size()==0 || q.candidatesRanked.get(0).features.inDegree == 0) {
				out.println(q.query_id.trim()+"\tNIL");
			}
			
			else {
				out.println(q.query_id.trim()+"\t"+q.candidatesRanked.get(0).entity.id);
			}
		}
		out.close();		
	}
	
	static void generateOutputOut(String output, List<KBPQuery> queries) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (KBPQuery q : queries) {
			
			if (q.candidatesRanked.size()==0 || q.candidatesRanked.get(0).features.outDegree == 0) {
				out.println(q.query_id.trim()+"\tNIL");
			}
			
			else {
				out.println(q.query_id.trim()+"\t"+q.candidatesRanked.get(0).entity.id);
			}
		}
		out.close();		
	}
	
	static void generateOutputBoth(String output, List<KBPQuery> queries) throws FileNotFoundException {
		
		PrintStream out = new PrintStream( new FileOutputStream(output));
		
		for (KBPQuery q : queries) {
			
			if (q.candidatesRanked.size() == 0 || q.candidatesRanked.get(0).features.outDegree == 0 || q.candidatesRanked.get(0).features.inDegree == 0) {
				out.println(q.query_id.trim()+"\tNIL");
			}
			
			else {
				out.println(q.query_id.trim()+"\t"+q.candidatesRanked.get(0).entity.id);
			}
		}
		out.close();		
	}

	
}




