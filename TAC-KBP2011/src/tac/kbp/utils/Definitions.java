package tac.kbp.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import tac.kbp.queries.GoldStandardQuery;

public class Definitions {
	
	public static HashMap<String, String> docslocations = new HashMap<String, String>();
	public static HashMap<String, GoldStandardQuery> queriesGold = new HashMap<String, GoldStandardQuery>();
	public static String named_entities_supportDoc = "/collections/TAC-2011/named-entities-Stanford-CRF-XML";
	public static String serializedClassifier = "/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz";
	public static Set<String> stop_words = new HashSet<String>();

	public static void loadDocsLocations(String filename) throws Exception {
		
		BigFile file = new BigFile(filename);
		String[] parts;
		
		for (String line : file) {		
			parts = line.split(".sgm");			
			docslocations.put(parts[0], parts[1]);
		}
		
	}
	
	public static void loadStopWords(String file) { 
		
		try{
			  FileInputStream fstream = new FileInputStream(file);
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  
			  while ((strLine = br.readLine()) != null)   {				  
				  stop_words.add(strLine.trim());
			  }
			  
			  in.close();
			  
			}
		
			catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
			}
	}
	
	public static void loadGoldStandard(String filename) throws IOException {
		
		BufferedReader input;
		
		if (filename.contains("2009") || filename.contains("2010/trainning")) {
			
			try {
				
				input = new BufferedReader(new FileReader(filename));
				String line = null;
		        
				while (( line = input.readLine()) != null){
		          String[] contents = line.split("\t");	          
		          GoldStandardQuery gold = new GoldStandardQuery(contents[0], contents[1], contents[2]);
		          queriesGold.put(contents[0], gold);
		        }
		        
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();    	
			}
			
		}
		
		else {
			
			try {
				
				input = new BufferedReader(new FileReader(filename));
				String line = null;
		        
				while (( line = input.readLine()) != null){
		          String[] contents = line.split("\t");	          
		          GoldStandardQuery gold = new GoldStandardQuery(contents[0], contents[1], contents[2], contents[3], contents[4]);
		          queriesGold.put(contents[0], gold);
		        }
		        
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();    	
			}
			
		}

	}
}
