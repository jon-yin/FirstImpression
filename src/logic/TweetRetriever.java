package logic;

import java.util.List;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TweetRetriever {

	public static Twitter twitter = ConfigTwitter.getTwitter();
	
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
	
}
