package debug;


import logic.ConfigTwitter;
import logic.UtilityMethods;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TweetDebugger {
	
	public static Twitter twitter = ConfigTwitter.getTwitter();

	public void displayTweetLog(Status status)
	{
		System.out.println("Body:" + status.getText());
		System.out.println("Likes:" + status.getFavoriteCount());
		System.out.println("Retweets:" + status.getRetweetCount());
		System.out.println("Is a retweet? :" + status.isRetweet());
		System.out.println("Date Created: " + UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt()));
		System.out.println("\n\n\n");
	}
	
	public void displayRateLimitStatus()
	{
		try {
			System.out.println(twitter.getRateLimitStatus());
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void displayTweetGui(Status status)
	{
		
	}
	
	
}
