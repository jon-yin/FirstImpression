package driver;

import java.util.HashMap;

import logic.Retriever;
import logic.SearchLimited;
import twitter4j.Query.ResultType;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ArchiveDriver {
	
	public static final String FILEPATH = "C:\\Users\\hgfddfgh\\Documents\\ReadForProject\\TwitterDump";
	
	public static void main(String[] args) throws TwitterException {
		SearchLimited lim = Retriever.searchRL("mcdonalds", -1, null, null);
		System.out.println(lim);
		System.out.println(lim.getStatuses().size()); 
	}
}
