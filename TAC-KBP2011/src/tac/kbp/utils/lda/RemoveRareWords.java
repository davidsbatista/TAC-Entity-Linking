package tac.kbp.utils.lda;

import java.io.*;
import java.util.*;

public class RemoveRareWords {

	public static int numWords = 20000;
	public static Map<String,Integer> words = new HashMap<String,Integer>();
	public static String dummyToken = "DUMMYTOKEN";

	public static void buildWordTable ( Reader inReader ) throws Exception {
		BufferedReader in = new BufferedReader(inReader);
		String line = in.readLine();
		while ( (line = in.readLine()) != null ) {
			String tokens[] = line.split(" +");
			for ( String token : tokens ) {
				Integer count = words.get(token);
				if (count == null) count = new Integer(0);
				count++;
				words.put(token,count);
			}
		}
		List<String> wordList = new ArrayList<String>();
		wordList.addAll(words.keySet());

		Collections.sort (wordList, new Comparator() {  
	            public int compare(Object o1, Object o2) {  
	                String s1 = (String) o1;  
	                String s2 = (String) o2;  
	                return (words.get(s1) > words.get(s2) ? -1 : words.get(s1) < words.get(s2) ? + 1 : 0);  
	            }
	        });
		for ( int i = numWords ; i < wordList.size(); i++ ) words.remove(wordList.get(i));
		in.close();
	}

	public static void filterDocuments ( Reader inReader, Writer outWriter ) throws Exception {
		PrintWriter out = new PrintWriter(outWriter);
		BufferedReader in = new BufferedReader(inReader);
		String line = in.readLine();
		out.println(line);
		while ( (line = in.readLine()) != null ) {
			String tokens[] = line.split(" +");
			boolean first = true;
			for ( String token : tokens ) {
				if ( words.containsKey(token) ) { out.print( ( first ? "" : " " ) + token); first = false; }
			}
			out.println(" " + dummyToken);
		}
		in.close();
		out.close();
	}
	

	public static void main ( String args[] ) throws Exception {
		if ( args.length < 2 || args.length > 3 ) {
			System.err.println("Usage : RemoveRareWords input-file output-file [num-words]");
			System.exit(1);
		}
		Reader in1 = new FileReader(new File(args[0]));
		Reader in2 = new FileReader(new File(args[0]));
		Writer out = new FileWriter(new File(args[1]));
		if (args.length==3) numWords = new Integer(args[2]).intValue();
		System.out.println("Building word table...");
		buildWordTable(in1);
		System.out.println("Final word table with " + words.size() + " tokens...");
		System.out.println("Filtering documents...");
		filterDocuments(in2,out);
		System.out.println("Done!");
	}

}
