package tac.kbp.ranking;

import tac.kbp.queries.Features;

public class SVMRank {
	
	/*
	# query 1
	3 qid:1 1:1 2:1 3:0 4:0.2 5:0
	2 qid:1 1:0 2:0 3:1 4:0.1 5:1
	1 qid:1 1:0 2:1 3:0 4:0.4 5:0
	1 qid:1 1:0 2:0 3:1 4:0.3 5:0
	*/
	
	public void svmRankFormat(Features features) {
		
		double[] vector = features.inputVector();
		
		for (int i = 0; i < vector.length; i++) {
			
		}
		
	}

}
