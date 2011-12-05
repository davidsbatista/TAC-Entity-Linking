package tac.kbp.ranking;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import tac.kbp.queries.Candidate;
import tac.kbp.queries.Features;
import tac.kbp.queries.KBPQuery;
import tac.kbp.utils.Definitions;

import com.aliasi.matrix.DenseVector;
import com.aliasi.matrix.Vector;
import com.aliasi.stats.AnnealingSchedule;
import com.aliasi.stats.LogisticRegression;
import com.aliasi.stats.RegressionPrior;
import com.aliasi.util.AbstractExternalizable;

public class LogisticRegressionLingPipe {
	
	public ArrayList<double[]> inputs = new ArrayList<double[]>();
	public ArrayList<Integer> outputs = new ArrayList<Integer>();
	public LogisticRegression regression = null;
		
	public void generateFeatures() throws Exception {
		
		for (KBPQuery q : Definitions.queries) {
			
			ArrayList<double[]> tmp_input = new ArrayList<double[]>();
			ArrayList<Integer> tmp_output = new ArrayList<Integer>();
			
			System.out.print("Extracting features from candidates for query " + q.query_id);
			
			// file to where feature vectors are going to be written
			PrintStream out = new PrintStream( new FileOutputStream(q.query_id+".txt"));
			boolean foundCorrecEntity = false;
			
			for (Candidate c : q.candidates) {
				System.out.print(".");
				c.extractFeatures(q);
				if (c.features.correct_answer) {
					foundCorrecEntity = true;
				}
				
				//write feature vector to file
				double[] vector = c.features.inputVector();
				int output = c.features.output();

				//first field of line is candidate identifier;
				out.print(c.entity.id+":");
				
				for (int i = 0; i < vector.length; i++) {
					out.print(vector[i] + ",");
				}
				out.println(output);

				// structures holding all the generated features vectors + outputs: to be passed to LogisticRegression
				inputs.add(c.features.inputVector());
				outputs.add(c.features.output());
			}
			
			//if correct entity is not part of the retrieved entities and correct answer is not NIL, 
			//retrieve entity from KB and calculate features 
			
			if ( !foundCorrecEntity && !(Definitions.queriesGold.get(q.query_id).answer.startsWith("NIL")) ) {

				System.out.println("getting correct candidate from KB!");
				QueryParser queryParser = new QueryParser(org.apache.lucene.util.Version.LUCENE_30,"id", new WhitespaceAnalyzer());
				ScoreDoc[] scoreDocs = null;
				String queryS = "id:" + Definitions.queriesGold.get(q.query_id).answer;
				
				TopDocs docs = tac.kbp.utils.Definitions.searcher.search(queryParser.parse(queryS), 1);				
				scoreDocs = docs.scoreDocs;
				
				if (docs.totalHits != 0) {
					Document doc = tac.kbp.utils.Definitions.searcher.doc(scoreDocs[0].doc);
					
					//extract features
					Candidate c = new Candidate(doc,scoreDocs[0].doc);
					c.features.lucene_score = scoreDocs[0].score; 
					c.extractFeatures(q);
					
					double[] vector = c.features.inputVector();
					int output = c.features.output();

					//first field of line is candidate identifier;
					out.print(c.entity.id+":");
					
					for (int i = 0; i < vector.length; i++) {
						out.print(vector[i] + ",");
					}
					out.println(output);
				}
			}
			System.out.println();
			out.close();
		}
	}
	
	public void loadVectors(String pathName) {
		
		File dir = new File(pathName); 
		String fileList[] = dir.list();
		
		if (fileList.length == 0) {
			System.out.println("No vector files found");
			System.exit(0);
		}
		
		else {
			
			//starts parsing vector files
			System.out.println(fileList.length + " files loaded");
			
			for (int i=0; i < fileList.length; i++) {
				parseFile(pathName+"/"+fileList[i]);
			}
		}
	}

	public void parseFile(String file){
		
		try{
			  FileInputStream fstream = new FileInputStream(file);
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  
			  while ((strLine = br.readLine()) != null)   {
				  String[] candidate_features = strLine.split(",");
				  Features features = new Features(candidate_features);				  
				  //features.eid = file.split("\\.")[0];
				  inputs.add(features.inputVector());
				  outputs.add(Integer.parseInt(candidate_features[candidate_features.length-1]));
			  }
			  in.close();			  
			}
		
			catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
			}
	}
			
	public void writeVectors(String query_id, ArrayList<double[]> input, ArrayList<Integer> output) throws FileNotFoundException {	
		int z=0;
		PrintStream out = new PrintStream( new FileOutputStream(query_id+".txt"));
		
		for (double[] vector : input) {
			for (int i = 0; i < vector.length; i++) {
				out.print(vector[i] + ",");
			}
			out.println(output.get(z));
			z++;
		}
		out.close();
	}
	
    public void trainModel() {
    	
    	int[] OUTPUTS = new int[this.outputs.size()];
		int z=0;
		for (int i : outputs) {
			OUTPUTS[z] = i;
			z++;
		}
		
		Vector[] INPUTS = new Vector[this.inputs.size()];
		int x=0;
		for (double[] d : inputs) {
			DenseVector dV = new DenseVector(d);
			INPUTS[x] = dV;
			x++;
		}
    	
        System.out.println("Computing Logistic Regression");        
        regression = LogisticRegression.estimate(INPUTS,
                                          OUTPUTS,
                                          RegressionPrior.noninformative(),
                                          AnnealingSchedule.inverse(.05,100),
                                          null, // reporter with no feedback
                                          0.000000001, // min improve
                                          1, // min epochs
                                          5000); // max epochs
        
        Vector[] betas = regression.weightVectors();        
        for (int outcome = 0; outcome < betas.length; ++outcome) {
            System.out.print("Outcome=" + outcome);
            for (int i = 0; i < betas[outcome].numDimensions(); ++i)
                System.out.printf(" %6.2f",betas[outcome].value(i));
            System.out.println();
        }
    }
    
    public Vector[] toLingpipe(ArrayList<double[]> input){
  	
    	Vector[] INPUTS = new Vector[this.inputs.size()];
		int x=0;
		for (double[] d : inputs) {
			DenseVector dV = new DenseVector(d);
			INPUTS[x] = dV;
			x++;
		}
		return INPUTS;
    }
    
    public void applyTrainedModel() {
    	Vector[] input = toLingpipe(inputs);
    	
    	for (Vector candidade : input) {
            
    		double[] conditionalProbs = regression.classify(candidade);
            
    		for (int i = 0; i < candidade.numDimensions(); ++i)
                System.out.printf("%3.1f ",candidade.value(i));
            
    		for (int k = 0; k < conditionalProbs.length; ++k)
                System.out.printf(" p(%d|input)=%4.2f ",k,conditionalProbs[k]);
    		
    		System.out.println();
         }
    	
    }

    public void writeModel(String file) throws IOException{
    	
    	FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
    	regression.compileTo(oos);
    }

    public void readModel(String file) throws IOException, ClassNotFoundException {
    	
    	Object object = AbstractExternalizable.readObject(new File(file));
    	this.regression = (LogisticRegression) object;
    	
    }
}




