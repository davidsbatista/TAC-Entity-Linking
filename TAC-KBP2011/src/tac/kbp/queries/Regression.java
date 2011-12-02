package tac.kbp.queries;

import java.util.ArrayList;

import com.aliasi.matrix.DenseVector;
import com.aliasi.matrix.Vector;

import com.aliasi.stats.AnnealingSchedule;
import com.aliasi.stats.LogisticRegression;
import com.aliasi.stats.RegressionPrior;

public class Regression {
	
	static int[] OUTPUTS;
	static Vector[] INPUTS;
	
	public static void generateVectors(ArrayList<double[]> inputs, ArrayList<Integer> outputs) {
		
		OUTPUTS = new int[outputs.size()];
		int z=0;
		for (int i : outputs) {
			OUTPUTS[z] = i;
			z++;
		}
		
		INPUTS = new Vector[inputs.size()];
		int x=0;
		for (double[] d : inputs) {
			DenseVector dV = new DenseVector(d);
			INPUTS[x] = dV;
			x++;
		}
	}
	
    public static void calculate() {
        System.out.println("Computing Logistic Regression");
        LogisticRegression regression
            = LogisticRegression.estimate(INPUTS,
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

        /*
	System.out.println("\nInput Vector         Outcome Conditional Probabilities");
        for (Vector testCase : TEST_INPUTS) {
            double[] conditionalProbs = regression.classify(testCase);
            for (int i = 0; i < testCase.numDimensions(); ++i) {
                System.out.printf("%3.1f ",testCase.value(i));
            }
            for (int k = 0; k < conditionalProbs.length; ++k) {
                System.out.printf(" p(%d|input)=%4.2f ",k,conditionalProbs[k]);
            }
            System.out.println();
        }
       */
    }
}
