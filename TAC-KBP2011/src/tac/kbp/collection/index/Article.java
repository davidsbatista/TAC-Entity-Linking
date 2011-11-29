package tac.kbp.collection.index;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Article {
	
	public String text = null;
	public String genre = null;
	public String doc_id = null;
	
	public Article(String file) throws IOException {
		
		String filename = Main.docslocations.get(file).trim()+"/"+file+".sgm";
		this.doc_id = file;

		String article = readFileAsString(filename);
		this.text = tac.kbp.utils.string.StringUtils.removeTags(article);
		
		/* 
		document genre:		
		bc - broadcast conversation transcripts
		bn - broadcast news transcripts
		cts - conversational telephone speech transcripts
		nw - newswire text
		wb - weblog text
		*/
		
		if (filename.contains("/bc"))
			this.genre = "broadcast conversation transcripts";
		
		if (filename.contains("/bn"))
			this.genre = "broadcast news transcripts";
				
		if (filename.contains("/cts"))
			this.genre = "conversational telephone speech transcripts";
						
		if (filename.contains("/nw"))
			this.genre = "newswire text";

		if (filename.contains("/wb"))
			this.genre = "weblog text";			
	}
	
	private static String readFileAsString(String filePath) throws java.io.IOException{
	    byte[] buffer = new byte[(int) new File(filePath).length()];
	    BufferedInputStream f = null;
	    try {
	        f = new BufferedInputStream(new FileInputStream(filePath));
	        f.read(buffer);
	    } finally {
	        if (f != null) try { f.close(); } catch (IOException ignored) { }
	    }
	    
	    f.close();
	    
	    return new String(buffer);
	}

	public String getText() {
		return text;
	}

	public String getGenre() {
		return genre;
	}

	public String getDoc_id() {
		return this.doc_id;
	}
}
