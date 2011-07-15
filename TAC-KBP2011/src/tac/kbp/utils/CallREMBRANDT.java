package tac.kbp.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

public class CallREMBRANDT {
	
	public static void main(String[] args) {
		
		HttpClient httpclient = new HttpClient();
		
		PostMethod postMethod = new PostMethod("http://agatha.inesc-id.pt:80/Rembrandt/api/rembrandt?");
		
		NameValuePair slg = new NameValuePair("slg", "pt");
		NameValuePair lg = new NameValuePair("lg", "pt");
		NameValuePair format = new NameValuePair("format", "dsb");
		NameValuePair key = new NameValuePair("api_key","db924ad035a9523bcf92358fcb2329dac923bf9c");
		NameValuePair sentence = new NameValuePair("db","Hoje está um dia de calor em Lisboa");
		
		postMethod.addParameter(slg);
		postMethod.addParameter(lg);
		postMethod.addParameter(format);
		postMethod.addParameter(key);
		postMethod.addParameter(sentence);
		
        postMethod.setDoAuthentication( false );
		
		BufferedReader br = null;
		
		try	{
			
			int returnCode = httpclient.executeMethod(postMethod);
			
			HttpClientParams params =  httpclient.getParams();		
			
			if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {		    	  
				System.err.println("The Post method is not implemented by this URI");
				// still consume the response body
				String response = postMethod.getResponseBodyAsString();
				System.out.println("erro: \n"+response);
			}
			
			else {
				
				System.out.println(postMethod.getName());
				System.out.println(postMethod.getPath());
				System.out.println(postMethod.getQueryString());
					
				String response = postMethod.getResponseBodyAsString();
				System.out.println(response);
		        
				/*
				
				br = new BufferedReader(new InputStreamReader(postMethod.getResponseBodyAsStream()));
		    	
				String readLine;
		    	
		    	while(((readLine = br.readLine()) != null)) {
		    		System.err.println(readLine);
		    	  }
		    	*/    	
		      }
		    
		} catch (Exception e) {
		      System.err.println(e);
		    } finally {
		    	
		      postMethod.releaseConnection();
		      if(br != null) try { br.close(); } catch (Exception fe) {}
		    }
	}
}
