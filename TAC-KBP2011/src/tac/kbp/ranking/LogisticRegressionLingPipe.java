package tac.kbp.ranking;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import tac.kbp.bin.Definitions;
import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.candidates.Candidate;
import tac.kbp.queries.features.Features;

import com.aliasi.matrix.DenseVector;
import com.aliasi.matrix.Vector;
import com.aliasi.stats.AnnealingSchedule;
import com.aliasi.stats.LogisticRegression;
import com.aliasi.stats.RegressionPrior;
import com.aliasi.util.AbstractExternalizable;

public class LogisticRegressionLingPipe {
	
	public ArrayList<double[]> inputs = new ArrayList<double[]>();
	public ArrayList<Integer> outputs = new ArrayList<Integer>();	
	public ArrayList<String> eid = new ArrayList<String>();
	public LogisticRegression regression = null;
	
	public LogisticRegressionLingPipe(ArrayList<double[]> inputs, ArrayList<Integer> outputs) {
		super();
		this.inputs = inputs;
		this.outputs = outputs;
	}
	
	public LogisticRegressionLingPipe() {
		super();
	}
	
	public void loadVectors(String pathName) throws NumberFormatException, IOException {
		
		File dir = new File(pathName); 
		String fileList[] = dir.list();
		
		if (fileList.length == 0) {
			System.out.println("No vector files found");
			System.exit(0);
		}
		
		else {
			Arrays.sort(fileList);
			System.out.println(fileList.length + " files loaded");
			
			//starts parsing vector files			
			for (int i=0; i < fileList.length; i++) {
				if (fileList[i].equalsIgnoreCase("linear-regression"))
					continue;
				parseFile(pathName+"/"+fileList[i]);
			}
		}
	}

	//construct feature vectors 
	public void parseFile(String file) throws NumberFormatException, IOException{
		
		String[] splitted = file.split("\\/");		
		KBPQuery query = new KBPQuery(splitted[2].split("\\.")[0]);
		
		
		try{
			FileInputStream fstream = new FileInputStream(file);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			  
			while ((strLine = br.readLine()) != null)   {			
				String data[] = strLine.split(":");		
								
				String[] featuresArray = data[1].split(",");
				Features features = new Features(data[0], featuresArray);
				query.candidates.add(new Candidate(data[0], features));
				eid.add(features.eid);
				inputs.add(features.featuresVector());
				outputs.add(Integer.parseInt(featuresArray[featuresArray.length-1]));
			}
			br.close();
			in.close();
			fstream.close();
			Definitions.queriesTrain.add(query);
		}
		catch (Exception e) {
			System.out.println(e);
		}
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
    
    //use model to classify
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
    
    public void applyTrainedModelCandidate(Candidate c) {
   
    	double[] featuresVector = c.features.featuresVector();
    	DenseVector dV = new DenseVector(featuresVector); 
    	double[] conditionalProbs = regression.classify(dV);
		c.conditionalProbabilities = conditionalProbs;
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




