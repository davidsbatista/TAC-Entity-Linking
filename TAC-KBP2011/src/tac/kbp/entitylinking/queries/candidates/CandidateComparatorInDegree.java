package tac.kbp.entitylinking.queries.candidates;

import java.util.Comparator;

public class CandidateComparatorInDegree implements Comparator<Candidate> {
	    
		@Override
	    public int compare(Candidate c1, Candidate c2) {
			
			final int BEFORE = -1;
		    final int EQUAL = 0;
		    final int AFTER = 1;
		    int value = 0;
		    
		    Double inDegree1 = new Double(c1.features.inDegree);
		    Double inDegree2 = new Double(c2.features.inDegree);

		    if (inDegree1 == inDegree2 ) value = EQUAL;
		    if (inDegree1 < inDegree2) value = AFTER;
		    if (inDegree1 > inDegree2) value= BEFORE;
		    
		    return value;
	    }
}