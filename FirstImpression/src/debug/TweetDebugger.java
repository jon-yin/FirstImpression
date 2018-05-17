package debug;


import logic.UtilityMethods;
import twitter4j.Status;

public class TweetDebugger {

	public void displayTweetLog(Status status)
	{
		System.out.println("Body:" + status.getText());
		System.out.println("Likes:" + status.getFavoriteCount());
		System.out.println("Retweets:" + status.getRetweetCount());
		System.out.println("Is a retweet? :" + status.isRetweet());
		System.out.println("Date Created: " + UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt()));
		System.out.println("\n\n\n");
	}
	
	public void displayTweetGui(Status status)
	{
		
	}
	
	
}
