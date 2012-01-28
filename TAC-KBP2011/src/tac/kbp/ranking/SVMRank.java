package tac.kbp.ranking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tac.kbp.queries.GoldQuery;
import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.KBPQueryComparator;
import tac.kbp.queries.candidates.Candidate;

public class SVMRank {
	
	/*
	# query 1
	3 qid:1 1:1 2:1 3:0 4:0.2 5:0
	2 qid:1 1:0 2:0 3:1 4:0.1 5:1
	1 qid:1 1:0 2:1 3:0 4:0.4 5:0
	1 qid:1 1:0 2:0 3:1 4:0.3 5:0
	*/
	
	public void svmRankFormat(List<KBPQuery> queries, HashMap<String, GoldQuery> queries_answers, String outputfile) throws IOException {
		
		FileWriter fstream = new FileWriter(outputfile);
		BufferedWriter out = new BufferedWriter(fstream);

		//to avoid: "ERROR: Query ID's in data file have to be in increasing order" from SVMRank
		//sort queries according to id  in increasing order
		Collections.sort(queries, new KBPQueryComparator());
		
		for (KBPQuery q : queries) {
			
			out.write("#" + q.query_id + " " + q.gold_answer + "\n");
			
			for (Candidate c : q.candidates) {
				
				double[] vector = c.features.featuresVector();
				
				if (queries_answers.get(q.query_id).answer.equalsIgnoreCase(c.entity.id)) {
					out.write("1"+" ");
				}
				
				else out.write("0"+" ");
				
				String[] query_parts = q.query_id.split("EL");			
				out.write("qid:"+Integer.parseInt(query_parts[1])+" ");
				
				for (int i = 0; i < vector.length; i++) {
					out.write((i+1)+":"+vector[i]+" ");
				}
				out.write("#" + c.entity.id);
				out.write("\n");
			}
		}
		out.close();
	}
	
	public void svmRankFormat(String queriesFilesDir, HashMap<String, GoldQuery> queries_answers, String outputfile) throws IOException {
		
		File dir = new File(queriesFilesDir);
		String[] files = dir.list();
		
		FileWriter fstream = new FileWriter(outputfile);
		BufferedWriter out = new BufferedWriter(fstream);
		
		if (files == null) {
		} else {
			System.out.println("found " + files.length + " files");
			
			java.util.Arrays.sort(files);
			
		    for (int i=0; i < files.length; i++) {
		        String filename = files[i];
		        readFile(filename,queries_answers,out);
		    }
		}
	}
	
	public void readFile(String filename,HashMap<String, GoldQuery> queries_answers, BufferedWriter out) throws IOException{
		
		//filename = queryID
		// each line candidate:
		//	 E0393717:0.1166139856018351,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,8763.0,0.0,0
		
		String query_id = filename.split("\\.")[0];
		
		FileInputStream fstream = new FileInputStream(filename);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;
		
		
		
		System.out.println("Processing " + query_id);
		
		out.write("#" + query_id + " " +  queries_answers.get(query_id).answer+"\n");
		
		while ((strLine = br.readLine()) != null)   {
			
			String[] line_parts = strLine.split(":");
			String candidate_id = line_parts[0];
			String[] features = line_parts[1].split(",");
			
			if (features[features.length-1].equalsIgnoreCase("1"))
				out.write("1 ");
			else out.write("0 ");
			
			out.write("qid:"+filename.split("EL")[1]+' ');
						
			for (int z = 0; z < features.length-1; z++) {
				out.write((z+1)+":"+features[z]+' ');	
			}
			out.write("#"+candidate_id+"\n");
		}
		br.close();
		in.close();
		fstream.close();
	}
	
	
}








