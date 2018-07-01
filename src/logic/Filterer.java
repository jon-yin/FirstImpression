package logic;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import twitter4j.Status;

public class Filterer {
	private Stream<Status> statuses;
	
	public Filterer (List<Status> statuses)
	{
		this.statuses = statuses.stream();
	}
	
	public static Predicate<Status> getSensitive()
	{
		return (status) -> status.isPossiblySensitive();
	}
	
	public static Predicate<Status> excludeSensitive()
	{
		return getSensitive().negate();
	}
	
	public static Predicate<Status> getRetweets()
	{
		return (status) -> status.isRetweet();
	}
	
	public static Predicate<Status> excludeRetweets()
	{
		return getRetweets().negate();
	}
	
	public static Predicate<Status>  getVerified()
	{
		return (status) -> status.getUser().isVerified();
	}
	
	public static Predicate<Status> excludeVerified()
	{
		return getVerified().negate();
	}
	
	public static Predicate<Status> getBetween(LocalDateTime lower, LocalDateTime upper)
	{
		return (status) -> {
			LocalDateTime time = UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt());
			return time.compareTo(lower) > 0 && time.compareTo(upper) < 0;
		};
	}
	
	public static Predicate<Status> excludeBetween(LocalDateTime lower, LocalDateTime upper)
	{
		return getBetween(lower,upper).negate();
	}
	
	public static Predicate<Status> getUsers(List<String> users)
	{
		return (status) -> {
			String handle = status.getUser().getName();
			return users.contains(handle);
		};
	}
	
	public static Predicate<Status> excludeUsers(List<String> users)
	{
		return getUsers(users).negate();
	}
	
	public static Predicate<Status> exactLikes(int likes)
	{
		return status -> status.getFavoriteCount() == likes;
	}
	
	public static Predicate<Status> atLeastLikes(int likes)
	{
		Predicate<Status> below = status -> status.getFavoriteCount() > likes;
		return below.or(exactLikes(likes));
	}
	
	public static Predicate<Status> atMostLikes(int likes)
	{
		Predicate<Status> above = status -> status.getFavoriteCount() < likes;
		return above.or(exactLikes(likes));
	}
	
	public static Predicate<Status> exactRetweets(int retweets)
	{
		return status -> status.getRetweetCount() == retweets;
	}
	
	public static Predicate<Status> atLeastRetweets(int retweets)
	{
		Predicate<Status> below = status -> status.getRetweetCount() > retweets;
		return below.or(exactRetweets(retweets));
	}
	
	public static Predicate<Status> atMostRetweets(int retweets)
	{
		Predicate<Status> above = status -> status.getRetweetCount() < retweets;
		return above.or(exactRetweets(retweets));
	}
	
	public Filterer filter(Predicate<Status> pred)
	{
		statuses = statuses.filter(pred);
		return this;
	}
	
	public List<Status> collectStatuses()
	{
		return statuses.collect(Collectors.toList());
	}

	public void setStatuses(List<Status> statuses) {
		this.statuses = statuses.stream();
	}
	
}
