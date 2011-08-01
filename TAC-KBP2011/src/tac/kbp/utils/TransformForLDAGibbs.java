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
			
			//starts indexing the parsed entities
			for (int i=0; i < fileList.length; i++) {
				
				System.out.print("\nProcessing " + fileList[i]);
				
				//use buffering
				File aFile = new File("KB_one_file_documents.txt");					
				Writer output = new BufferedWriter(new FileWriter(aFile));
			    
				for (Entity entity : parser.process(args[0]+fileList[i]).getEntities()) {
					
					String id = entity.getId().replaceAll("\\n", " ");
					String title = entity.getWiki_title().replaceAll("\\n", " ");
					String infoclass = entity.getInfobox_class().replaceAll("\\n", " ");
					String text = entity.getWiki_text().replaceAll("\\n", " ");
					String text_no_stopwords = null;
					
					
					for (Iterator<String> iterator = stop_words.iterator(); iterator.hasNext();) {
						String stopword = (String) iterator.next();
						stopword = ".*"+stopword+".*";
						text_no_stopwords = text.replaceAll(stopword, " ");
					}
					
					/*
					StringBuffer stemmed = new StringBuffer();
					
					String[] words = text_no_stopwords.split(" ");
					String[] stemmead_words = new String[words.length];
					
					for (int j = 0; j < words.length; j++) {
						stemmead_words[j] =  stemm(words[j]);
					}
					
					for (int j = 0; j < words.length; j++) {
						stemmed.append(stemmead_words[j] + " ");
					}
					
					System.out.println(text_no_stopwords);
					System.out.println(stemmed.toString());
					*/
					
					StringBuffer contents = new StringBuffer();
					
					contents.append(title + " " + infoclass + " " + text_no_stopwords);
					output.write( id + " = " + contents.toString() + "\n");

				}
				output.close();
			}
		}		
	}
}