package tac.kbp.ranking;

import java.util.*;

public class RankAggregation<T> {

	@SuppressWarnings({"unchecked"})
	public List<T> sort ( final Map<T,Double> scores , final boolean reverse ) {
		Comparator comparator = new Comparator() {  
			public int compare(Object key1, Object key2) {
				Comparable value1 = (Comparable) scores.get(key1);
				Comparable value2 = (Comparable) scores.get(key2);
				int c = reverse ? -value1.compareTo(value2) : value1.compareTo(value2);
				if (0 != c) return c;
				Integer h1 = key1.hashCode(), h2 = key2.hashCode();
				return reverse ? -h1.compareTo(h2) : h1.compareTo(h2);
			}
		};
		List<T> result = new ArrayList<T>();
		result.addAll(scores.keySet());
		Collections.sort(result,comparator);
		return result;
	}

	public Map<T,Double> normalize ( Map<T,Double> scores ) {
		Map<T,Double> result = new HashMap<T,Double>();
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for ( Double score : scores.values() ) {
			if ( score < min ) min = score;
			if ( score > max ) max = score;
		}
		for ( T obj : scores.keySet() ) {
			double score = scores.get(obj);
			if ( ( max - min ) == 0.0 ) result.put( obj , 0.0 ); else
			result.put( obj , ( score - min ) / ( max - min ) );
		}
		return result;
	}

	@SuppressWarnings({"unchecked"})
	public Map<T,Double>[] processMap ( Map<T,Map<String,Double>> maps ) {
		Map<String,Integer> metricToID = new HashMap<String,Integer>();
		for ( Map<String,Double> aux : maps.values() ) {
			for ( String metric : aux.keySet() ) {
				Integer id = metricToID.get(metric);
				if ( id == null ) metricToID.put(metric,metricToID.size());
			}
		}
		HashMap<T,Double> newMaps[] = (HashMap<T,Double>[])( new HashMap[metricToID.size()] );
		for ( int i = 0; i < newMaps.length; i++ ) newMaps[i] = new HashMap<T,Double>();
		for ( T obj : maps.keySet() ) {
			Map<String,Double> aux = maps.get(obj);
			for ( String metric : aux.keySet() ) {
				newMaps[metricToID.get(metric)].put(obj,aux.get(metric));
			}
		}
		return newMaps;
	}

	public Map<T,Double> reciprocalRankFusion ( Map<T,Map<String,Double>> maps ) {
		return reciprocalRankFusion ( processMap(maps) );
	}

	public Map<T,Double> bordaFusion ( Map<T,Map<String,Double>> maps ) {
		return bordaFusion ( processMap(maps) );
	}

	public Map<T,Double> combSUM ( Map<T,Map<String,Double>> maps ) {
		return combSUM ( processMap(maps) );
	}

	public Map<T,Double> combMNZ ( Map<T,Map<String,Double>> maps ) {
		return combMNZ ( processMap(maps) );
	}

	@SuppressWarnings({"unchecked"})
	public Map<T,Double> reciprocalRankFusion ( Map<T,Double> maps[] ) {
		Map<T,Double> result = new HashMap<T,Double>();		
		List<T> sorted [] = (List<T>[]) ( new ArrayList[maps.length] );
		for ( int i = 0; i < maps.length; i++ ) sorted[i] = sort( maps[i] , true );
		for ( T obj : maps[0].keySet() ) {
			double score = 0;
			for ( List<T> list : sorted ) {
				for ( int i = 0; i < list.size(); i++ ) {
					if (list.get(i).equals(obj)) {
						score += 1.0 / ( i + 60.0 );
						break;
					}
				}
			}
			result.put(obj,score);
		}
		return result;
	}

	@SuppressWarnings({"unchecked"})
	public Map<T,Double> bordaFusion ( Map<T,Double> maps[] ) {
		Map<T,Double> result = new HashMap<T,Double>();		
		List<T> sorted [] = (List<T>[]) ( new ArrayList[maps.length] );
		for ( int i = 0; i < maps.length; i++ ) sorted[i] = sort( maps[i] , true );
		for ( T obj : maps[0].keySet() ) {
			double score = 0;
			for ( List<T> list : sorted ) {
				for ( int i = 0; i < list.size(); i++ ) {
					if (list.get(i).equals(obj)) {
						score += (list.size() - i - 1);
						break;
					}
				}
			}
			result.put(obj,score);
		}
		return result;
	}

	public Map<T,Double> combSUM ( Map<T,Double> maps[] ) {
		Map<T,Double> result = new HashMap<T,Double>();
		for ( int i = 0; i < maps.length; i++ ) maps[i] = normalize(maps[i]);
		for ( T obj : maps[0].keySet() ) {
			double score = 0;
			for ( int i = 0; i < maps.length; i++ ) {
				score += maps[i].get(obj);
			}
			result.put(obj,score);
		}
		return result;
	}

	public Map<T,Double> combMNZ ( Map<T,Double> maps[] ) {
		Map<T,Double> result = new HashMap<T,Double>();
		for ( int i = 0; i < maps.length; i++ ) maps[i] = normalize(maps[i]);
		for ( T obj : maps[0].keySet() ) {
			double score = 0.0;
			double nonZero = 0.0;
			for ( int i = 0; i < maps.length; i++ ) {
				score += maps[i].get(obj);
				nonZero += maps[i].get(obj) != 0.0 ? 1.0 : 0.0;
			}
			result.put(obj, score * nonZero);
		}
		return result;
	}

	// THIS MAIN METHOD JUST CONTAINS SOME DEBUG CODE !!!
	public static void main ( String args[] ) throws Exception {
		System.out.println("Testing rank aggregation");
		RankAggregation<String> rag = new RankAggregation<String>();
		Map<String,Map<String,Double>> scores = new HashMap<String,Map<String,Double>>();
		scores.put("Object1" , new HashMap<String,Double>());
		scores.get("Object1").put("Metric 1", 1.0);
		scores.get("Object1").put("Metric 2", 1.0);
		scores.get("Object1").put("Metric 3", 0.5);
		scores.get("Object1").put("Metric 4", 0.5);

		scores.put("Object2" , new HashMap<String,Double>());
		scores.get("Object2").put("Metric 1", 0.5);
		scores.get("Object2").put("Metric 2", 0.3);
		scores.get("Object2").put("Metric 3", 0.1);
		scores.get("Object2").put("Metric 4", 0.1);

		scores.put("Object3" , new HashMap<String,Double>());
		scores.get("Object3").put("Metric 1", 0.0);
		scores.get("Object3").put("Metric 2", 0.0);
		scores.get("Object3").put("Metric 3", 0.0);
		scores.get("Object3").put("Metric 4", 0.2);

		Map<String,Double>[] aux = rag.processMap(scores);

		Map<String,Double> resultSUM = rag.combMNZ(scores);
		for ( String key : resultSUM.keySet() ) System.out.println(key + " -> " + resultSUM.get(key));
		Map<String,Double> resultMNZ = rag.combMNZ(scores);
		for ( String key : resultMNZ.keySet() ) System.out.println(key + " -> " + resultMNZ.get(key));
		Map<String,Double> resultRR = rag.reciprocalRankFusion(scores);
		for ( String key : resultRR.keySet() ) System.out.println(key + " -> " + resultRR.get(key));
		Map<String,Double> resultB = rag.bordaFusion(scores);
		for ( String key : resultB.keySet() ) System.out.println(key + " -> " + resultB.get(key));
	}

}
