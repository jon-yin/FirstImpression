package logic;

import java.util.List;

import twitter4j.Status;

/**
 * Represents a list of statuses retrieved from twitter + whether their
 * @author Jonathan Yin
 *
 */
public abstract class StatusesLimited {
	
	private List<Status> statuses;
	private boolean isLimited;
	private long maxId;
	private int limit;

	/**
	 * Retrieves the statuses that have been retrieved by retriever.
	 * @return Statuses retrieved by retriever.
	 */
	public List<Status> getStatuses() {
		return statuses;
	}

	public void setStatuses(List<Status> statuses) {
		this.statuses = statuses;
	}

	/**
	 * Determines whether if the list of statuses is a complete result
	 * @return true if partial, false if complete
	 */
	public boolean isLimited() {
		return isLimited;
	}

	public void setLimited(boolean isLimited) {
		this.isLimited = isLimited;
	}

	/**
	 * Last MaxId to start requerying from again. This field is undefined for complete results. If this is 0, then statuses are empty.
	 * @return Last MaxId of partial results
	 */
	public long getMaxId() {
		return maxId;
	}

	public void setMaxId(long maxId) {
		this.maxId = maxId;
	}

	/**
	 * Remaining number of statuses to retrieve. This field is undefined for complete results.
	 * @return Number of statuses to retrieve.
	 */
	
	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}
	

}
