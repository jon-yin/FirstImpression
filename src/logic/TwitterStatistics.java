package logic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.RateLimitStatusEvent;
import twitter4j.RateLimitStatusListener;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TwitterStatistics {

	public static Twitter twitter = ConfigTwitter.getTwitter();

	/**
	 * Returns the intersection of 2 user Id sets contained in UserLimited Objects
	 * @param set1 The first UserLimited set
	 * @param set2 The Second UserLimited set
	 * @return A Set of longs referring to userids that both sets have in common.
	 */
	public Set<Long> UserIntersection(UserLimited set1, UserLimited set2)
	{
		if (set1.getFollowers() == null)
		{
			return set2.getFollowers();
		}
		else if (set2.getFollowers() == null)
		{
			return set1.getFollowers();
		}
		else
		{
			Set<Long> clone = new HashSet<>();
			clone.addAll(set1.getFollowers());
			clone.retainAll(set2.getFollowers());
			return clone;
		}
	}
	
	/**
	 * Retrieves as many followers as possible by their IDs. For the purpose of
	 * this application, there isn't a need for the full User object.
	 * 
	 * @param handle
	 *            Screenname to retrieve Users from.
	 * @return A set containing unique
	 */
	public UserLimited getFollowers(String handle) {
		Set<Long> results = new HashSet<>();
		UserLimited limited = new UserLimited();
		limited.setFollowers(results);
		limited.setLimited(false);
		try {
			RateLimitStatus ratelimit = twitter.getRateLimitStatus().get("/followers/ids");
			int limit = ratelimit.getLimit();
			long nextCursor = -1;
			while (nextCursor != 0) {
				limited.setLastCursor(nextCursor);
				if (limit == 0) {
					limited.setLimited(true);
					break;
				}
				IDs page = twitter.getFollowersIDs(handle, nextCursor, 5000);
				nextCursor = page.getNextCursor();
				long[] data = page.getIDs();
				for (long id : data) {
					results.add(id);
				}
				limit--;
			}
			limited.setFollowers(results);
			return limited;
		} catch (TwitterException e) {
			UtilityMethods.global.warning("Rate Limited, partial results returned!");
			limited.setLimited(true);
			return limited;
		}
	}
	
	

	public UserLimited getFollowing(String handle) {
		Set<Long> results = new HashSet<>();
		UserLimited limited = new UserLimited();
		long nextCursor = -1;
		limited.setFollowers(results);
		limited.setLastCursor(nextCursor);
		limited.setLimited(false);
		try{
			RateLimitStatus limit = twitter.getRateLimitStatus().get("/friends/following/ids");
			int remaining = limit.getRemaining();
			while(nextCursor != 0)
			{
				limited.setLastCursor(nextCursor);
				if (remaining == 0)
				{
					limited.setLimited(true);
					break;
				}
				IDs following = twitter.getFriendsIDs(handle, nextCursor, 5000);
				nextCursor = following.getNextCursor();
				long[] ids = following.getIDs();
				for (long id: ids)
				{
					results.add(id);
				}
			}
			return limited;
		}
		catch (TwitterException e)
		{
			limited.setLimited(true);
			e.printStackTrace();
			return limited;
		}
	}

	public int getFollowersCount(User user) {
		return user.getFollowersCount();
	}

	public int getFollowersCount(int userID) {
		try {
			RateLimitStatus ratelimit = twitter.getRateLimitStatus().get("/users/show/:id");
			int remaining = ratelimit.getRemaining();
			if (remaining > 0) {
				User user = twitter.showUser(userID);
				return getFollowersCount(user);
			} else {
				System.out.println("Rate Limited try again in " + ratelimit.getRemaining() + " seconds");
				return -1;
			}
		} catch (TwitterException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public int getFollowingCount(User user) {
		return user.getFriendsCount();
	}

	public int getFollowingCount(int userID) {
		try {
			RateLimitStatus ratelimit = twitter.getRateLimitStatus().get("/users/show/:id");
			int remaining = ratelimit.getRemaining();
			if (remaining > 0) {
				User user = twitter.showUser(userID);
				return getFollowingCount(user);
			} else {
				System.out.println("Rate Limited try again in " + ratelimit.getRemaining() + " seconds");
				return -1;
			}
		} catch (TwitterException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Retrieves Statuses that have recieved a number of favorites that is
	 * considered an outlier. Inner outliers are as defined as being less than
	 * the lower outer fence: Q1 - 1.5*IQ or greater than the upper outer fence:
	 * Q3 + 1.5*IQ where Q1 and Q3 are quartiles 1 and 3 and IQ is the
	 * Interquartile range. More information here:
	 * itl.nist.gov/div898/handbook/prc/section1/prc16.htm
	 * 
	 * @param statuses
	 *            The list of tweets to examine.
	 * @return A list containing tweets whose like counts were considered
	 *         outliers by the above criteria.
	 */
	public List<Status> getLikeOutliers(List<Status> statuses) {
		Supplier<Stream<Integer>> supplier = () -> statuses.stream().mapToInt(Status::getFavoriteCount).boxed();
		List<Integer> Likes = supplier.get().collect(Collectors.toList());
		Collections.sort(Likes);
		double median = getMedian(Likes);
		double thirdQuartile = getMedian(
				supplier.get().filter(integer -> integer > median).collect(Collectors.toList()));
		double firstQuartile = getMedian(
				supplier.get().filter(integer -> integer < median).collect(Collectors.toList()));
		double IQR = thirdQuartile - firstQuartile;
		// System.out.println(IQR);
		List<Status> lowExtremes = statuses.stream()
				.filter(status -> status.getFavoriteCount() < (firstQuartile - 1.5 * IQR)).collect(Collectors.toList());
		List<Status> highExtremes = statuses.stream()
				.filter(status -> status.getFavoriteCount() < (thirdQuartile + 1.5 * IQR)).collect(Collectors.toList());
		lowExtremes.addAll(highExtremes);
		return lowExtremes;
	}

	/**
	 * Retrieves tweets that are considered particularly popular for this group
	 * of tweets. This is implemented by returning tweets that have greater or
	 * equal the average number of likes.
	 * 
	 * @param statuses
	 *            The tweets to retrieve popular tweets from.
	 * @return Tweets that are considered popular.
	 */
	public List<Status> getPopularLikedTweets(List<Status> statuses) {
		double average = retrieveAverageLikes(statuses, true);
		return statuses.stream().filter(status -> status.getFavoriteCount() >= average).collect(Collectors.toList());
	}

	private double getMedian(List<Integer> data) {
		if (data.size() % 2 == 1) {
			return data.get(data.size() / 2);
		} else {
			return (data.get(data.size() / 2 - 1) + data.get(data.size() / 2)) / 2.0;
		}
	}

	/**
	 * Retrieves the average number of likes for this list of tweets.
	 * 
	 * @param statuses
	 *            The list of statuses to retrieve from.
	 * @param allowOutliers
	 *            whether to allow outliers in the calculation of average likes
	 * @return The average number of likes a tweet has recieved.
	 */
	public double retrieveAverageLikes(List<Status> statuses, boolean allowOutliers) {
		List<Status> process = statuses;
		if (!allowOutliers) {
			List<Status> outliers = getLikeOutliers(statuses);
			process = process.stream().filter(status -> !(outliers.contains(status))).collect(Collectors.toList());
		}
		double average = process.stream().mapToDouble(Status::getFavoriteCount).sum() / process.size();
		return average;
	}

	/**
	 * Retrieves Statuses that have recieved a number of retweets that is
	 * considered an outlier. Outliers are as defined as being less than the
	 * lower outer fence: Q1 - 3*IQ or greater than the upper outer fence: Q3 +
	 * 3*IQ where Q1 and Q3 are quartiles 1 and 3 and IQ is the Interquartile
	 * range. More information here:
	 * itl.nist.gov/div898/handbook/prc/section1/prc16.htm
	 * 
	 * @param statuses
	 *            The list of tweets to examine.
	 * @return A list containing tweets whose like counts were considered
	 *         outliers by the above criteria.
	 */
	public List<Status> getRetweetOutliers(List<Status> statuses) {
		Stream<Integer> RetweetStream = statuses.stream().mapToInt(Status::getRetweetCount).boxed();
		List<Integer> Retweets = RetweetStream.collect(Collectors.toList());
		Collections.sort(Retweets);
		double median = getMedian(Retweets);
		double thirdQuartile = getMedian(
				RetweetStream.filter(integer -> integer > median).collect(Collectors.toList()));
		double firstQuartile = getMedian(
				RetweetStream.filter(integer -> integer < median).collect(Collectors.toList()));
		double IQR = thirdQuartile - firstQuartile;
		List<Status> lowExtremes = statuses.stream()
				.filter(status -> status.getRetweetCount() < (firstQuartile - 1.5 * IQR)).collect(Collectors.toList());
		List<Status> highExtremes = statuses.stream()
				.filter(status -> status.getRetweetCount() < (thirdQuartile + 1.5 * IQR)).collect(Collectors.toList());
		lowExtremes.addAll(highExtremes);
		return lowExtremes;
	}
}
