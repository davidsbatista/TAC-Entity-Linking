package tac.kbp.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TestStopWords {
	
	static String text = "Parker,_Florida Infobox Settlement Parker, Florida  Parker is a city in Bay County, Florida, United States. The population was 4,623 at the 2000 census. As of 2005, the population recorded by the U.S. Census Bureau is 4,672.   Geography  Parker is located at  (30.129552, -85.600875).  According to the United States Census Bureau, the city has a total area of 6.3 km² (2.4 mi²). 5.0 km² (1.9 mi²) of it is land and 1.3 km² (0.5 mi²) of it (20.16%) is water.   Demographics  As of the census of 2000, there were 4,623 people, 1,991 households, and 1,264 families residing in the city. The population density was 920.1/km² (2,381.7/mi²). There were 2,280 housing units at an average density of 453.8/km² (1,174.6/mi²). The racial makeup of the city was 81.70% White, 11.33% African American, 0.65% Native American, 2.66% Asian, 0.06% Pacific Islander, 0.93% from other races, and 2.66% from two or more races. Hispanic or Latino of any race were 2.51% of the population.  There were 1,991 households out of which 26.9% had children under the age of 18 living with them, 47.9% were married couples living together, 11.3% had a female householder with no husband present, and 36.5% were non-families. 28.8% of all households were made up of individuals and 8.8% had someone living alone who was 65 years of age or older. The average household size was 2.32 and the average family size was 2.84.  In the city the population was spread out with 22.3% under the age of 18, 9.7% from 18 to 24, 29.7% from 25 to 44, 22.8% from 45 to 64, and 15.5% who were 65 years of age or older. The median age was 38 years. For every 100 females there were 98.3 males. For every 100 females age 18 and over, there were 98.5 males.  The median income for a household in the city was $35,813, and the median income for a family was $43,929. Males had a median income of $28,455 versus $21,205 for females. The per capita income for the city was $18,660. About 10.1% of families and 12.2% of the population were below the poverty line, including 21.3% of those under age 18 and 4.6% of those age 65 or over.";
	static Set<String> stop_words = new HashSet<String>();
	
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
	
	public static void main(String[] args){
		
		StringBuffer words_only = new StringBuffer();
		
		loadStopWords(args[0]);
		
		String text_cleaned = text.replaceAll("[^\\w|^\\s|[0-9]]", "");
				
		String[] words = text_cleaned.split("[\\s]");
		
		System.out.println(words.length);
		
		for (int i = 0; i < words.length; i++) {
			if (!stop_words.contains(words[i].toLowerCase()) && words[i].length()>0) {
				words_only.append(words[i]+" ");
			}
		}
		
		System.out.println(words_only.toString());
		
	}
}
