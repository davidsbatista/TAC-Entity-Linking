package tac.kbp.utils.string;

import java.util.ArrayList;
import java.util.HashMap;

import tac.kbp.queries.KBPQuery;
import uk.ac.shef.wit.simmetrics.metrichandlers.MetricHandler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;

public class StringSimilarities {
	
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
}













