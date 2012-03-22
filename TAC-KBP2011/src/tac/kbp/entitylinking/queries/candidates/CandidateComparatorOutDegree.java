package tac.kbp.queries.candidates;

import java.util.Comparator;

public class CandidateComparatorOutDegree implements Comparator<Candidate> {
	    
		@Override
	    public int compare(Candidate c1, Candidate c2) {
			
			final int BEFORE = -1;
		    final int EQUAL = 0;
		    final int AFTER = 1;
		    int value = 0;
		    
		    Double outDegree1 = new Double(c1.features.outDegree);
		    Double outDegree2 = new Double(c2.features.outDegree);

		    if (outDegree1 == outDegree2 ) value = EQUAL;
		    if (outDegree1 < outDegree2) value = AFTER;
		    if (outDegree1 > outDegree2) value= BEFORE;
		    
		    return value;
	    }
}