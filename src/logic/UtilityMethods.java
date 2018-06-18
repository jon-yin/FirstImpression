package logic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.logging.Logger;

import twitter4j.RateLimitStatus;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class UtilityMethods {
	
	public static final String PREFERENCES_PATH = "impression.first.my";
	public static Logger global = Logger.getGlobal();

	public static LocalDateTime convertDateToLocalDateTime(Date input)
	{
		return input.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
	
	public static double bytesToKB(long bytes)
	{
		return bytes / 1024.0;
	}
	
	public static double bytesToMB(long bytes)
	{
		return bytes / Math.pow(1024, 2);
	}
	
	public static int getRateLimit(Twitter twitter, String apicall)
	{
		RateLimitStatus status;
		try {
			status = twitter.getRateLimitStatus().get(apicall);
		} catch (TwitterException e) {
			e.printStackTrace();
			return 0;
		}
		return status == null ? 0 : status.getLimit();
	}
}
