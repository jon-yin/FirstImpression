package logic;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.Query.ResultType;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class Retriever {

	public static Twitter twitter = ConfigTwitter.getTwitter();
	public static final int MAX_TWEETS = 3200;

	/**
	 * Retrieves all tweets (that Twitter will allow me to retrieve) from a
	 * specified handle name it is sorted in reverse chronological order.;
	 * 
	 * @param handle
	 *            Twitter Account Name to retrieve from
	 * @return As many tweets as retrievable based on Twitter's API (3200) or
	 *         less based on number of tweets handle has made or ratelimit.
	 */
	public static TimelineLimited getAllTweetsRL(String handle) {
		return getAllTweetsRL(handle, MAX_TWEETS);
	}

	/**
	 * Variation of getAllTweets which also accepts a limit to the number of
	 * tweets retrieved.
	 * 
	 * @param handle
	 *            Twitter Account Name to retrieve from
	 * @param limit
	 *            Limit to number of tweets retrieved, note that this method may
	 *            return less tweets than specified.
	 * @return List of statuses that from specified handle's timeline up to
	 *         limit tweets.
	 */
	public static TimelineLimited getAllTweetsRL(String handle, int limit) {
		if (limit <= 0) {
			return null;
		}
		int ratelimit = UtilityMethods.getRateLimit(twitter, "/statuses/user_timeline");
		TimelineLimited results = new TimelineLimited();
		results.setHandle(handle);
		results.setLimit(limit);
		List<Status> firstStatuses = null;
		try {
			if (ratelimit == 0) {
				results.setLimit(limit);
				results.setLimited(true);
				return results;
			}
			firstStatuses = twitter.getUserTimeline(handle);
			long maxId = firstStatuses.get(firstStatuses.size() - 1).getId();
			if (limit <= 20) {
				limit -= 1;
				results.setLimited(false);
				results.setStatuses(firstStatuses.subList(0, limit));
				return results;
			} else {
				limit -= 20;
				results.setStatuses(firstStatuses);
				results.setLimit(limit);
				while (limit > 0) {
					System.out.println("LIMIT: " + limit);
					System.out.println("ARRAYLIST: " + results.getStatuses().size());
					Paging paging = new Paging();
					int count = limit;
					if (count > 200) {
						count = 200;
					}
					paging.setCount(count);
					paging.setMaxId(maxId);
					results.setMaxId(maxId);
					results.setLimit(limit);
					if (ratelimit == 0) {
						results.setLimited(true);
						UtilityMethods.warn("Incomplete Results");
						return results;
					}
					List<Status> statuses = twitter.getUserTimeline(handle, paging);
					long tempMaxId = statuses.get(statuses.size() - 1).getId();
					if (tempMaxId == maxId) {
						results.setLimited(false);
						break;
					}
					ratelimit--;
					maxId = tempMaxId;
					limit -= statuses.size();
					/**
					 * System.out.println("Total Retrieved: " +
					 * firstStatuses.size() + " tweets.");
					 * System.out.println("Retrieved in this iteration " +
					 * statuses.size() + " tweets"); System.out.println("LIMIT:
					 * " + limit);
					 */
					firstStatuses.addAll(statuses);
				}
				return results;
			}

		} catch (TwitterException e) {
			UtilityMethods.warn("Incomplete Results");
			return results;
		}
	}

	/**
	 * Attempts to complete retrieving tweets from a specified handle's
	 * timeline.
	 * 
	 * @param limited
	 *            Incomplete results obtained from a failed call to
	 *            {@link Retriever#getAllTweetsRL(String, int)} or
	 *            {@link Retriever#getAllTweetsRL(String)}
	 * @return An attempt at completing the previous failed call. The results
	 *         returned may or may not be complete.
	 */
	public static TimelineLimited completeTweets(TimelineLimited limited) {
		if (limited == null) {
			return null;
		}
		if (!limited.isLimited()) {
			return limited;
		} else {
			if (limited.getStatuses() == null) {
				return getAllTweetsRL(limited.getHandle(), limited.getLimit());
			} else {
				int ratelimit = UtilityMethods.getRateLimit(twitter, "/statuses/user_timeline");
				long maxId = limited.getMaxId();
				int limit = limited.getLimit();
				try {
					while (limit > 0) {
						Paging paging = new Paging();
						int count = limit > 200 ? 200 : limit;
						paging.setCount(count);
						paging.setMaxId(maxId);
						limited.setLimit(limit);
						limited.setMaxId(maxId);
						if (ratelimit == 0) {
							return limited;
						}
						List<Status> statuses = twitter.getUserTimeline(limited.getHandle(), paging);
						if (maxId == statuses.get(statuses.size() - 1).getId()) {
							// Obtained as many tweets that we can, this is a
							// complete list.
							limited.setLimited(false);
							return limited;
						}
						limited.getStatuses().addAll(statuses);
						maxId = statuses.get(statuses.size() - 1).getId();
						limit -= statuses.size();
						ratelimit--;
					}
					limited.setLimited(false);
					return limited;
				} catch (TwitterException e) {
					e.printStackTrace();
					return limited;
				}
			}
		}
	}

	/**
	 * Retrieves liked tweets of the specified user. Up to limit number of
	 * tweets.
	 * 
	 * @param handle
	 *            The handle from which to retrieve liked tweets from.
	 * @param limit
	 *            The number of tweets to retrieve up to. -1 specifies to
	 *            retrieve as many as possible.
	 * @return List of liked tweets from this handle, up to limit.
	 */
	public static LikedLimited getLikedTweetsRL(String handle, int limit) {
		int ratelimit = UtilityMethods.getRateLimit(twitter, "/favorites/list");
		LikedLimited likedLim = new LikedLimited();
		long maxId = -1;
		Paging paging = new Paging();
		List<Status> results = new ArrayList<>();
		likedLim.setHandle(handle);
		likedLim.setStatuses(results);
		boolean unlimited = limit == -1;
		try {
			while (limit > 0 || unlimited) {
				int count = unlimited ? 200 : (limit > 200 ? 200 : limit);
				paging.setCount(count);
				paging.setMaxId(maxId);
				likedLim.setMaxId(maxId);
				likedLim.setLimit(limit);
				if (ratelimit == 0) {
					likedLim.setLimited(true);
					return likedLim;
				}
				List<Status> status = twitter.getFavorites(handle, paging);
				if (status.size() == 0) {
					break;
				}
				results.addAll(status);
				int last = status.size() - 1;
				maxId = status.get(last).getId() - 1;
				limit -= status.size();
				ratelimit--;
			}
			likedLim.setLimited(false);
			return likedLim;
		} catch (TwitterException e) {
			e.printStackTrace();
			return likedLim;
		}
	}

	public static LikedLimited completeResults(LikedLimited lim) {
		if (lim == null || !lim.isLimited()) {
			return lim;
		}
		int ratelimit = UtilityMethods.getRateLimit(twitter, "/favorites/list");
		List<Status> results = lim.getStatuses();
		String handle = lim.getHandle();
		long maxId = lim.getMaxId();
		int limit = lim.getLimit();
		Paging paging = new Paging();
		boolean unlimited = limit < 0;
		try {
			while (limit > 0 || unlimited) {
				int count = unlimited ? 200 : (limit > 200 ? 200 : limit);
				paging.setCount(count);
				paging.setMaxId(maxId);
				lim.setMaxId(maxId);
				lim.setLimit(limit);
				if (ratelimit == 0) {
					return lim;
				}
				List<Status> status = twitter.getFavorites(handle, paging);
				if (status.size() == 0) {
					break;
				}
				results.addAll(status);
				int last = status.size() - 1;
				maxId = status.get(last).getId() - 1;
				limit -= status.size();
				ratelimit--;
			}
			lim.setLimited(false);
			return lim;
		} catch (TwitterException e) {
			e.printStackTrace();
			return lim;
		}
	}

	/**
	 * Performs a search using twitter's search API, for full syntax of how to
	 * formulate a twitter search query,
	 * 
	 * @see <a
	 *      href=https://developer.twitter.com/en/docs/tweets/search/api-reference/get-search-tweets.html></a>
	 * @param query
	 *            The query to execute.
	 * @param max
	 *            Max number of tweets to return, note that rate limit
	 *            enforcement or quantity of search results may result in less
	 *            than this number being returned. -1 means as many as possible.
	 * @param lang
	 *            ISO 639-1 language code to search tweets from. null indicates
	 *            that all languages are acceptable.
	 * @param type
	 *            Query.MIXED or Query.POPULAR or Query.RECENT, null will
	 *            default to MIXED
	 * @return List of statuses that are returned from this query search.
	 */
	public static SearchLimited searchRL(String query, int max, String lang, ResultType type) {
		int limit = UtilityMethods.getRateLimit(twitter, "/search/tweets");
		SearchLimited results = new SearchLimited();
		boolean unlimited = max == -1;
		results.setLang(lang);
		results.setQuery(query);
		results.setType(type);
		List<Status> collectiveResults = new ArrayList<>();
		results.setStatuses(collectiveResults);
		Query search = new Query(query);
		search.setResultType(type);
		search.setLang(lang);
		long maxID = -1;
		try {
			while (max > 0 || unlimited) {
				// Logic is as follows, if unlimited, retrieve 100 at a time
				// otherwise
				// if max (tweets left)> 100 set to 100 (this is twitter's
				// limit)
				int count = unlimited ? 100 : (max > 100 ? 100 : max);
				search.setCount(count);
				search.setMaxId(maxID);
				results.setLimit(max);
				results.setMaxId(maxID);
				if (limit == 0) {
					// Can't search anymore, this is a limited set of results
					results.setLimited(true);
					return results;
				}
				QueryResult result = twitter.search(search);
				if (result.getTweets().size() == 0) {
					// No more results to retrieve, break. This is a complete
					// set of results.
					break;
				}
				collectiveResults.addAll(result.getTweets());
				maxID = result.getTweets().get(result.getTweets().size() - 1).getId() - 1;
				max -= result.getTweets().size();
				limit--;
			}
			results.setLimited(false);
			return results;
		} catch (TwitterException e) {
			e.printStackTrace();
			results.setLimited(true);
			return results;
		}

	}

	/**
	 * Attempts to complete an incomplete result from retrieving tweets from a
	 * search query.
	 * 
	 * @param limited
	 *            The partial result which was returned from search or
	 *            completeTweets
	 * @return A SearchLimited object which attempted to complete retrieving
	 *         results from the original query. It may or may not be complete
	 *         based on rate limit status.
	 */
	public static SearchLimited completeTweets(SearchLimited limited) {
		if (limited == null || !limited.isLimited()) {
			return limited;
		} else {
			int ratelimit = UtilityMethods.getRateLimit(twitter, "/search/tweets");
			// Remaining tweets to fetch from this query
			int max = limited.getLimit();
			long maxID = limited.getMaxId();
			boolean unlimited = max < 0;
			List<Status> results = limited.getStatuses();
			Query query = new Query(limited.getQuery());
			query.setLang(limited.getLang());
			query.setResultType(limited.getType());
			try {
				while (max > 0 || unlimited) {
					int count = unlimited ? 100 : (max > 100 ? 100 : max);
					query.setCount(count);
					limited.setMaxId(maxID);
					limited.setLimit(max);
					if (ratelimit == 0) {
						return limited;
					}
					QueryResult result = twitter.search(query);
					List<Status> retrieved = result.getTweets();
					if (retrieved.size() == 0) {
						// Retrieved all possible tweets, break;
						break;
					}
					int lastindex = retrieved.size() - 1;
					results.addAll(retrieved);
					maxID = retrieved.get(lastindex).getId() - 1;
					max -= retrieved.size();
					ratelimit--;
				}
				limited.setLimited(false);
				return limited;
			} catch (TwitterException e) {
				return limited;
			}

		}
	}

	/**
	 * This method is a simpler version of {@link #getAllTweetsRL(String)}.
	 * Although still rate limit compliant, this will return a List<Status>
	 * instead of an intermediary TimelineLimited object. If limited, the
	 * results cannot be completed.
	 * 
	 * @param handle
	 *            The handle to retrieve results from.
	 * @return List of statuses from that handle.
	 */
	public static List<Status> getAllTweets(String handle) {
		TimelineLimited lim = getAllTweetsRL(handle);
		return lim == null ? null : lim.getStatuses();
	}

	/**
	 * This method is a simpler version of {@link #getAllTweets(String, int)}.
	 * Although still rate limit compliant, this will return a List<Status>
	 * instead of an intermediary TimelineLimited object. If limited, the
	 * results cannot be completed with this method.
	 * 
	 * @param handle
	 *            The handle to retrieve result from.
	 * @param limit
	 *            The max limit of statuses from that handle
	 * @return List of statuses from that handle up to the limit number.
	 */
	public static List<Status> getAllTweets(String handle, int limit) {
		TimelineLimited lim = getAllTweetsRL(handle, limit);
		return lim == null ? null : lim.getStatuses();
	}

	/**
	 * This method is a simpler version of
	 * {@link #searchRL(String, int, String, ResultType)}. Although still rate
	 * limit compliant, this will return a List<Status> instead of an
	 * intermediary SearchLimited object. If limited, the results cannot be
	 * completed with this method.
	 * 
	 * @param query
	 *            The query to execute. See @see <a
	 *            href=https://developer.twitter.com/en/docs/tweets/search/api-reference/get-search-tweets.html>
	 *            </a>
	 * @param max
	 *            Max number of tweets to return, note that rate limit
	 *            enforcement or quantity of search results may result in less
	 *            than this number being returned. -1 means as many as possible.
	 * @param lang
	 *            ISO 639-1 language code to search tweets from. null indicates
	 *            that all languages are acceptable.
	 * @param type
	 *            Query.MIXED or Query.POPULAR or Query.RECENT, null will
	 *            default to MIXED
	 * @return List of statuses that are returned from this query search.
	 * @return
	 */
	public static List<Status> search(String query, int max, String lang, ResultType type) {
		SearchLimited lim = searchRL(query, max, lang, type);
		return lim == null ? null : lim.getStatuses();
	}

	/**
	 * This method is a simpler version of
	 * {@link #getLikedTweetsRL(String, int)}. Although still rate limit
	 * compliant, this will return a List<Status> instead of an intermediary
	 * LikedLimited object. If limited, the results cannot be completed with
	 * this method.
	 * @param handle The username to retrieve liked tweets from.
	 * @param limit The maximum number of tweets to retrieve. -1 means no limit.
	 * @return List of tweets liked by the account with this username. 
	 */
	public static List<Status> getLikedTweets(String handle, int limit) {
		LikedLimited lim = getLikedTweetsRL(handle, limit);
		if (lim == null)
		{
			return null;
		}
		return lim.getStatuses();
	}

	/**
	 * Method to convert a List of absolute urls to tweets to Status objects.
	 * 
	 * @param urls
	 *            The list of urls to process.
	 * @param immutable
	 *            If true, then a copy of urls will be used and the original
	 *            list of urls will not be modified (this is less efficient). If
	 *            false, the supplied list of urls will be modified.
	 * @return List of hydrated (full) tweets from urls.
	 */
	public static List<Status> urlToStatus(List<String> urls, boolean immutable) {
		List<Status> statuses = new ArrayList<>();
		List<String> data;
		if (immutable) {
			data = new ArrayList<>();
			data.addAll(urls);
		} else {
			data = urls;
		}
		int lookups = UtilityMethods.getRateLimit(twitter, "/statuses/lookup");
		data.replaceAll((url) -> url.substring(url.lastIndexOf("/") + 1, url.length()));
		int iterations = 0;
		if (lookups < data.size() / 100.0) {
			UtilityMethods.global.warning("Ratelimited results, only " + lookups * 100 + " results will be returned");
			iterations = lookups;
		} else {
			iterations = (int) (Math.ceil(urls.size() / 100.0));
		}
		for (int i = 0; i < iterations; i++) {
			int startindex = i * 100;
			int endindex = (i + 1) * 100;
			if (endindex >= data.size()) {
				endindex = data.size();
			}
			List<String> strings = data.subList(startindex, endindex);
			long[] ids = new long[strings.size()];
			for (int j = 0; j < ids.length; j++) {
				ids[j] = Long.parseLong(strings.get(j));
			}
			try {
				statuses.addAll(twitter.lookup(ids));
			} catch (TwitterException e) {
				e.printStackTrace();
			}
		}
		return statuses;
	}

}
