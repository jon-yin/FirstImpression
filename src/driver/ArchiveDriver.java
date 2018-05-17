package driver;

import java.util.Set;

import debug.TweetDebugger;
import logic.TwitterStatistics;
import twitter4j.TwitterException;

public class ArchiveDriver {
	
	public static void main(String[] args) throws TwitterException {
		
		TwitterStatistics statistics = new TwitterStatistics();
		Set<Long> userIds = statistics.getFollowers("MeidocafeR");
		System.out.println(userIds.size());
		TweetDebugger debugger = new TweetDebugger();
		debugger.displayRateLimitStatus();
		
		
	}
}
