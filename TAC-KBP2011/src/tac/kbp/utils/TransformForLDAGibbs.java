package tac.kbp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.xml.sax.SAXException;

import tac.kbp.kb.index.EntityParser;
import tac.kbp.kb.index.xml.Entity;
import tac.kbp.utils.Stemmer;

public class TransformForLDAGibbs {
	
	static Set<String> stop_words = new HashSet<String>();	
	static Stemmer stemmer = new Stemmer();
		
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
			  
			  System.out.println(stop_words.size() + " stopwords loaded");
			  
			}
		
			catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
			}
	}
	
	public static String stemm(String word) {
		
		word.toLowerCase();
		stemmer.add(word.toCharArray(), word.toCharArray().length);
		stemmer.stem();
				
		return stemmer.toString();	
	}

	public static String removeStopWords(String text) {
		
		StringBuffer words_only = new StringBuffer();				
		String text_cleaned = text.replaceAll("[^\\w|^\\s|[0-9]]", "");				
		String[] words = text_cleaned.split("[\\s]");
		
		for (int i = 0; i < words.length; i++) {
			if (!stop_words.contains(words[i].toLowerCase()) && words[i].length()>0) {
				words_only.append(words[i]+" ");
			}
		}
		
		return words_only.toString();		
	}
	
	
	public static void main(String[] args) throws IOException, SAXException {
		
		loadStopWords(args[1]);
		
		File dir = new File(args[0]);
		FilenameFilter only = new OnlyExt("xml"); 
		String fileList[] = dir.list(only);
		
		if (fileList.length == 0) {
			System.out.println("No XML files found");
			System.exit(0);
		}
		
		else {
			
			EntityParser parser = new EntityParser();
			Arrays.sort(fileList);			
			int num_docs = 0;
			
			//use buffering
			File aFile = new File("KB_one_file_documents.txt");					
			Writer output = new BufferedWriter(new FileWriter(aFile));
			
			for (int i=0; i < fileList.length; i++) {
				
				System.out.print("\nProcessing " + fileList[i]);
				
				for (Entity entity : parser.process(args[0]+fileList[i]).getEntities()) {
					
					String id = entity.getId().replaceAll("\\n", " ");
					String title = entity.getWiki_title().replaceAll("\\n", " ");
					String infoclass = entity.getInfobox_class().replaceAll("\\n", " ");
					String text = entity.getWiki_text().replaceAll("\\n", " ");					
					String text_no_stopwords = removeStopWords(text);
					
					StringBuffer contents = new StringBuffer();
					contents.append(title + " " + infoclass + " " + text_no_stopwords);
					output.write( id + " = " + contents.toString() + "\n");
					num_docs++;

				}
			}
			output.close();
			System.out.println(num_docs + " docs parsed");
		}		
	}
}