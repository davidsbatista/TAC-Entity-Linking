package tac.kbp.utils.misc;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author dsbatista
 *
 */

public class OnlyExt implements FilenameFilter { 
	
	String ext; 
	
	public OnlyExt(String ext) { 
		this.ext = "." + ext; 
	} 
	
	public boolean accept(File dir, String name) { 
		return name.endsWith(ext); 
	} 
}
