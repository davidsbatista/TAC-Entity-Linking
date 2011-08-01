package tac.kbp.utils;

import java.io.IOException;

import org.xml.sax.SAXException;

public class TestStemmer {
	
public static void main(String[] args) throws IOException, SAXException {
		
		Stemmer stemmer = new Stemmer(); 
		
		String word = "speaking";
		
		word.toLowerCase();
		stemmer.add(word.toCharArray(), word.toCharArray().length);
		stemmer.stem();
				
		System.out.println(stemmer.toString());
	}
}
