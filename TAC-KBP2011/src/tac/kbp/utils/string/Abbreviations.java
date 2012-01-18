package tac.kbp.utils.string;

public class Abbreviations {
	
	String shortForm = null;
	String longForm = null;
	
	public Abbreviations(String shortForm,String longForm) {
		this.shortForm = shortForm;
		this.longForm = longForm;
	}
	
	public String getShortForm() {
		return shortForm;
	}
	
	public void setShortForm(String shortForm) {
		this.shortForm = shortForm;
	}
	
	public String getLongForm() {
		return longForm;
	}
	
	public void setLongForm(String longForm) {
		this.longForm = longForm;
	}
	
}
