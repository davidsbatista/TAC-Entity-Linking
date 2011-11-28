package tac.kbp.utils.misc;

import java.io.*;

public final class JavaRunCommand {

	public static String run(String command) {

		String s = null;
		String output = new String();
		String errors = new String();

		try {
			Process p = Runtime.getRuntime().exec(command);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			// read the output from the command
			while ((s = stdInput.readLine()) != null) {
				output += s;
			}

			// read any errors from the attempted command
			while ((s = stdError.readLine()) != null) {
				errors += s;
			}
			
		} catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
		return output;
	}
}
