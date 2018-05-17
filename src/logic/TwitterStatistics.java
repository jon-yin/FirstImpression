package logic;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ratelimit.FollowersLimited;
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
	 * Retrieves as many followers as possible by their IDs. For the purpose of
	 * this application, there isn't a need for the full User object.
	 * 
	 * @param handle
	 *            Screenname to retrieve Users from.
	 * @return A set containing unique
	 */
	public FollowersLimited getFollowers(String handle) {
		Set<Long> results = new HashSet<>();
		FollowersLimited limited = new FollowersLimited();
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
			System.out.println("Rate Limited, partial results");
			limited.setLimited(true);
			return limited;
		}
	}

	public FollowersLimited getFollowing(String handle) {
		Set<Long> results = new HashSet<>();
		FollowersLimited limited = new FollowersLimited();
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
	 * Retrieves all tweets (that Twitter will allow me to retrieve) from a
	 * specified handle name it is sorted in reverse chronological order.;
	 * 
	 * @param handle
	 *            Twitter Account Name to retrieve from
	 * @return As many tweets as retrievable based on Twitter's API (3200) or
	 *         less based on number of tweets handle has made.
	 */
	public List<Status> getAllTweets(String handle) {
		try {
			ResponseList<Status> status = twitter.getUserTimeline(handle);
			long maxId = status.get(status.size() - 1).getId();
			// Must retrieve last 3200 tweets through 16 pages of 200 tweets
			// each
			// For sake of flexibility, we simply retrieve tweets until twitter
			// refuses to allow new tweets
			while (true) {
				Paging paging = new Paging();
				paging.setCount(200);
				// System.out.println(paging.getMaxId());
				// System.out.println(maxId);
				paging.setMaxId(maxId);
				ResponseList<Status> newStatuses = twitter.getUserTimeline(handle, paging);
				// System.out.println("SIZE: " + newStatuses.size());
				long tempMaxId = newStatuses.get(newStatuses.size() - 1).getId();
				if (tempMaxId == maxId) {
					break;
				} else {
					maxId = tempMaxId;
				}
				status.addAll(newStatuses);
			}
			return status;
		} catch (TwitterException e) {
			System.out.println("Error occured");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Variation of getAllTweets which also accepts a limit to the number of
	 * tweets retrieved.
	 * 
	 * @param handle
	 *            Twitter Account Name to retrieve from
	 * @param limit
	 * @return
	 */
	public List<Status> getAllTweets(String handle, int limit) {
		if (limit <= 0) {
			return null;
		}
		List<Status> firstStatuses = null;
		try {
			firstStatuses = twitter.getUserTimeline(handle);
			long maxId = firstStatuses.get(firstStatuses.size() - 1).getId();
			if (limit <= 20) {
				limit -= 1;
				return firstStatuses.subList(0, limit);
			} else {
				// Retrieved 20 tweets so subtract 20 from limit
				limit -= 20;
				while (limit > 0) {
					Paging paging = new Paging();
					int count = limit;
					if (count > 200) {
						count = 200;
					}
					paging.setCount(count);
					paging.setMaxId(maxId);
					List<Status> statuses = twitter.getUserTimeline(handle, paging);
					long tempMaxId = statuses.get(statuses.size() - 1).getId();
					if (tempMaxId == maxId) {
						break;
					} else {
						maxId = tempMaxId;
					}
					limit -= count;
					/**
					 * System.out.println("Total Retrieved: " +
					 * firstStatuses.size() + " tweets.");
					 * System.out.println("Retrieved in this iteration " +
					 * statuses.size() + " tweets"); System.out.println("LIMIT:
					 * " + limit);
					 */
					firstStatuses.addAll(statuses);
				}
				return firstStatuses;
			}

		} catch (TwitterException e) {
			e.printStackTrace();
			System.err.println("Could not access Twitter API");
			return null;
		}
	}

	/*
	 * public List<Status> getAllTweetsTest(String query) { Query searchQuery =
	 * new Query("from:"+query); searchQuery.setLang("en");
	 * searchQuery.setCount(100); QueryResult result; try { result =
	 * twitter.search(searchQuery); List<Status> statuses = result.getTweets();
	 * //long maxId = statuses.get(statuses.size() - 1).getId();
	 * //searchQuery.setMaxId(maxId - 1); //result =
	 * twitter.search(searchQuery); statuses = result.getTweets();
	 * 
	 * return statuses; } catch (
	 * 
	 * TwitterException e) {
	 * System.err.println("Something went wrong with accessing tweets");
	 * e.printStackTrace(); return null; } }
	 */

	/**
	 * Filters out unwanted tweets as specified in the criteria given.
	 * 
	 * @param criteria
	 *            The criteria used to reject/accept tweets.
	 * @param statuses
	 *            The List of tweets to filter.
	 * @return Filtered List of tweets based off the FilterCriteria
	 */
	public List<Status> filterOut(FilterCriteria criteria, List<Status> statuses) {
		Stream<Status> stream = statuses.stream();
		return stream.filter(status -> criteria.accept(status)).collect(Collectors.toList());
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
