package tac.kbp.ranking;

import java.util.List;
import tac.kbp.queries.KBPQuery;
import com.aliasi.stats.Statistics;

public class NILDetector {
	
	public void train(List<KBPQuery> queries) {
		
		for (KBPQuery q : queries) {			
			double[] features = extract_features(q);
			
			System.out.print(features[0]);
			for (int i = 1; i < features.length; i++) {
				System.out.print(+' '+i+':'+features[0]);
			}
			System.out.println("# " + q.docid + q.gold_answer);
		}		
		//TODO: write SVMLib format to file		
		//TODO: train classifier
	}
	
	static double[] extract_features(KBPQuery q) {
		
		/* based on score 
		- ranking score
		- mean score
		- difference from mean score
		- standard deviation
		- Dixon's Q Test for Outliers
		- Grubb's Test for Outliers
		*/
		
		/* based on atomic features 
		- kldivergence
		- topic_cosine_similarity
		- average string similarities
		*/
		
		double[] features = new double[6];
		
		double[] scores = new double[q.candidatesRanked.size()];
		double[] kldivergence = new double[q.candidatesRanked.size()];
		double[] textual_cosine_similarity = new double[q.candidatesRanked.size()];
		double[] average_string_similarities = new double[q.candidatesRanked.size()];
		
		for (int i = 0; i < q.candidatesRanked.size(); i++) {
			scores[i] = q.candidatesRanked.get(i).conditionalProbabilities[1];
			kldivergence[i] = q.candidatesRanked.get(i).features.kldivergence;
			textual_cosine_similarity[i] = q.candidatesRanked.get(i).features.cosine_similarity;
			average_string_similarities[i] = q.candidatesRanked.get(i).features.average_similarities;			
		}
		
		/* ranking scores */
		double mean_scores = Statistics.mean(scores);
		double std_dvt_scores = Statistics.standardDeviation(scores);
		double diff_mean_scores = Math.abs(q.candidatesRanked.get(0).conditionalProbabilities[1]-mean_scores);
		double dixonTest_scores = (scores[0]-scores[1]) / (scores[0]-scores[scores.length]); 
		double grubbsTest_scores = (scores[0]-mean_scores) / std_dvt_scores;
		
		
		if (q.gold_answer.startsWith("NIL")) 
			features[0] = 1;
		else 
			features[0] = 0;
		
		features[1] = mean_scores;
		features[2] = std_dvt_scores;
		features[3] = diff_mean_scores;
		features[4] = dixonTest_scores;
		features[5] = grubbsTest_scores;
		
		return features;
		
		/*
		// kl-divergence
		double mean_kldivergence = Statistics.mean(kldivergence);
		double std_dvt_kldivergence = Statistics.standardDeviation(kldivergence);
		double diff_mean_kldivergence = Math.abs(q.candidatesRanked.get(0).features.kldivergence-mean_kldivergence);
		
		// cosine sim
		double mean_textual_cosine_similarity = Statistics.mean(textual_cosine_similarity);
		double std_dvt_textual_cosine_similarity = Statistics.standardDeviation(textual_cosine_similarity);
		double diff_mean_textual_cosine_similarity = Math.abs(q.candidatesRanked.get(0).features.cosine_similarity-mean_textual_cosine_similarity);		
		
		// avg string sim
		double mean_average_string_similarities = Statistics.mean(average_string_similarities);		
		double std_average_string_similarities = Statistics.standardDeviation(average_string_similarities);
		double diff_mean_string_similarities = Math.abs(q.candidatesRanked.get(0).features.average_similarities-mean_average_string_similarities);
		*/
	}
	
	public void classify(){
		
		//extract features from top candidate
		//load trained model
		//apply
		
	}
}