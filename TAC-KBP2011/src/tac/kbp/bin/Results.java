package tac.kbp.bin;

import java.util.HashMap;

import tac.kbp.utils.misc.BigFile;

public class Results {

	HashMap<Integer, Double> predictions = new HashMap<Integer, Double>();	
	HashMap<Integer, Double> goundtruth = new HashMap<Integer, Double>();

	public void generateOutput(){
		
	}
	
	public void loadData(String predictionsFilePath, String goundtruthFilePath) throws Exception{
		
		BigFile predictionsFile = new BigFile(predictionsFilePath);
		BigFile goundtruthFile = new BigFile(goundtruthFilePath);
		int i=0;
		
		for (String line : predictionsFile) {
			predictions.put(i, Double.parseDouble(line));
			i++;
		}
		
		for (String line: goundtruthFile) {
			if (line.startsWith("#")) {
				//guardar o query id
			}
			
		}
		
		System.out.println("lines red: " + i);
	}
}
