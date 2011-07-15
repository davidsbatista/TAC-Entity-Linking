package tac.kbp.utils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class CallWebService {
	
	public static void main(String[] args) throws IOException {
			
		URL url = new URL("http://agatha.inesc-id.pt:80/Rembrandt/api/rembrandt?slg=pt&lg=pt&format=dsb&api_key=db924ad035a9523bcf92358fcb2329dac923bf9c&db=Hoje está um dia de calor em Lisboa");
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		httpCon.setDoOutput(true);
		httpCon.setRequestMethod("POST");
		OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
		System.out.println(httpCon.getResponseCode());
		System.out.println(httpCon.getResponseMessage());
		out.close();	  
	 }	
}