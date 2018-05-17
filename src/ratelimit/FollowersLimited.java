package ratelimit;

import java.util.Set;

/**
 * Followers/following may have many results so being rate limited is a legitimate concern. This class is designed to
 * warn for partial results due to rate limiting as well as cache results.
 * @author Jonathan Yin
 *
 */
public class FollowersLimited {

	private Set<Long> followers;
	private long lastCursor;
	private boolean limited;
	
	public Set<Long> getFollowers() {
		return followers;
	}
	public void setFollowers(Set<Long> followers) {
		this.followers = followers;
	}
	public long getLastCursor() {
		return lastCursor;
	}
	public void setLastCursor(long lastCursor) {
		this.lastCursor = lastCursor;
	}
	public boolean isLimited() {
		return limited;
	}
	public void setLimited(boolean limited) {
		this.limited = limited;
	}
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		if (limited)
		{
			builder.append("Rate Limited- partial results: ");
		}
		else
		{
			builder.append("Complete results: ");
		}
		builder.append("\n" + "Last Cursor: " + lastCursor);
		return builder.toString();
	}
}
