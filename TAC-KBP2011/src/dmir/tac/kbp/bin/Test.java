package dmir.tac.kbp.bin;

public class Test {
	
	
	public static void main(String[] args) {
		
		String query_id = "EL_0001";
		
		String[] query_parts;
		
		if (query_id.startsWith("EL_"))
			query_parts = query_id.split("EL_");
		
		else
			query_parts = query_id.split("EL");
		
		
		System.out.println(Integer.parseInt(query_parts[1]));
	}
}
