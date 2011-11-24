package tac.kbp.queryexpansion;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.BinaryJedis;

import tac.kbp.queries.KBPQuery;
import uk.ac.shef.wit.simmetrics.metrichandlers.MetricHandler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;

public class QueryExpansion {
	
	static List<KBPQuery> queries = null;
	static List<EntityString> entities =  new LinkedList<EntityString>();
 
	private static String host;
	private static int port;

	public static void readFile(String filename) {
		
		try {
			FileInputStream fstream = new FileInputStream(filename);	
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
		
			while ((strLine = br.readLine()) != null) {
				String[] data = strLine.split("\t");
				
				EntityString entity = new EntityString(data[0], data[1], data[2]);
				entities.add(entity);
			}
			
			in.close();
			} 
		
		catch (Exception e) {
			// Catch exception if any
			System.out.println(e);
			System.err.println("Error: " + e.getMessage());
		
		}
	}
	
	public static HashMap<String, Float> compareStrings(String str1, String str2) {

		/*
		 * ArrayList<String> metricStrings = MetricHandler.GetMetricsAvailable();
		 * System.out.println(metricStrings);
		 */
		
		/*
		levensthein similarity
		jar-winkler similarity
		jaccardi similarity
		soft jaccardi similarity
		soft TF-IDF
		*/

		String[] metrics = {"DiceSimilarity","JaccardSimilarity", "Jaro","JaroWinkler","Levenshtein"};

		// now create each metric in an ArrayList
		final ArrayList<AbstractStringMetric> testMetricArrayList = new ArrayList<AbstractStringMetric>();

		for (String metricString : metrics) {
			testMetricArrayList.add(MetricHandler.createMetric(metricString));
		}

		HashMap<String, Float> similarities = new HashMap<String, Float>();
		
		for (int i = 0; i < testMetricArrayList.size(); i++) {
			final AbstractStringMetric metric = testMetricArrayList.get(i);
			similarities.put(metric.getShortDescriptionString(), metric.getSimilarity(str1, str2));
		}
		
		return similarities;
	}
	
	public static void usage(){
		System.out.println("Usage: java QueryExpansion.class kb-entities queries");
		System.out.println();
	}
	
	private static String cleanString(String sense) {
		
		/*
		'Du Wei'
		'Du Wei(footballer)']
		[u'Du_wei'
		*/
		
		String cleaned =  sense.replace("[u'","").replace("']", "").replace("u'", "").replace("[","").
				replace("'","").replace("['", "").trim().replace("_", " ");
		
		return cleaned;
	}

	private static void getSenses(BinaryJedis binaryjedis, KBPQuery query) {
		
		try {

			byte[] queryStringbyteArray = query.name.getBytes("UTF-8");
			byte[] queryStringLowbyteArray = query.name.toLowerCase().getBytes("UTF-8");
			
			byte[] acronyms = binaryjedis.get(queryStringLowbyteArray);
			byte[] senses = binaryjedis.get(queryStringbyteArray);
			
			if (acronyms != null) {						
				String acr = new String(acronyms, "UTF8");
				String[] acronymsArray = acr.split(",\\s");
				
				for (int i = 0; i < acronymsArray.length; i++) {
					
					String cleaned = cleanString(acronymsArray[i]);
					
					if (cleaned.compareToIgnoreCase(query.name) != 0) {
						query.alternative_names.add(cleaned);
					}
											
				}
			}
			
			if (senses != null) {
				String ses = new String(senses, "UTF8");
				String[] sensesArray = ses.split(",\\s");
				
				for (int i = 0; i < sensesArray.length; i++) {
					
					String cleaned = cleanString(sensesArray[i]);
					
					if (cleaned.compareToIgnoreCase(query.name) != 0) {
						query.alternative_names.add(cleaned);
					}		
				}
			}
			
		}
		
		catch (Exception e) {
				// Catch exception if any
				System.out.println(e);
				System.err.println("Error: " + e.getMessage());
			}
		}

	public static void nameSimilarities() {
		
		for (Iterator<EntityString> interatorE = entities.iterator(); interatorE.hasNext();) {
			EntityString entity = interatorE.next();
		
			for (Iterator<KBPQuery> iteratorQ = queries.iterator(); iteratorQ.hasNext();) {
				KBPQuery query = iteratorQ.next();
				
				HashMap<String, Float> similarities =  compareStrings(query.name,entity.string_name);
				
				Set<String> keys = similarities.keySet();
								
				for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
					
					String key = iterator.next();
					System.out.println(key + ":" + similarities.get(key));					
				}
			}
		}
	}
	
	public static void main(final String[] args) {
		
		if (args.length < 2) {
			usage();
		}
		
		else {
						
			port = 6379;
			host = "agatha";
			BinaryJedis binaryjedis = new BinaryJedis(host, port);
			
			queries = tac.kbp.queries.xml.ParseXML.loadQueries(args[1]);
			System.out.println(queries.size() + " queries loaded");
			
			for (Iterator<KBPQuery> iterator = queries.iterator(); iterator.hasNext();) {
				KBPQuery query = iterator.next();
				getSenses(binaryjedis, query);
			}
		}
	}
}













