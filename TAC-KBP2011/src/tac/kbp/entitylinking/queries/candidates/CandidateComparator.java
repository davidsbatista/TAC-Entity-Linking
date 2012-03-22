package tac.kbp.queries.candidates;

import java.util.Comparator;

public class CandidateComparator implements Comparator<Candidate> {
    
	@Override
    public int compare(Candidate c1, Candidate c2) {
		
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    int value = 0;
	    
	    Double c1Prob = new Double(c1.getConditionalProbabilities()[1]);
	    Double c2Prob = new Double(c2.getConditionalProbabilities()[1]);

	    if (c1Prob == c2Prob ) value = EQUAL;
	    if (c1Prob < c2Prob) value = AFTER;
	    if (c1Prob > c2Prob) value= BEFORE;
	    
	    return value;
    }
}
