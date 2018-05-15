package logic;

import java.time.LocalDate;

import twitter4j.Status;
import twitter4j.User;

public class FilterCriteria {
	private boolean allowRetweets;
	private boolean allowSensitive;
	private boolean allowUnverified;
	private boolean allowVerified;
	private boolean popular;
	
	public FilterCriteria()
	{
		allowRetweets=true;
		allowSensitive=true;
		allowUnverified=true;
		allowVerified=true;
	}

	public boolean isAllowRetweets() {
		return allowRetweets;
	}

	public void setAllowRetweets(boolean allowRetweets) {
		this.allowRetweets = allowRetweets;
	}

	public boolean isAllowSensitive() {
		return allowSensitive;
	}

	public void setAllowSensitive(boolean allowSensitive) {
		this.allowSensitive = allowSensitive;
	}

	public boolean isAllowUnverified() {
		return allowUnverified;
	}

	public void setAllowUnverified(boolean allowUnverified) {
		this.allowUnverified = allowUnverified;
	}

	public boolean isAllowVerified() {
		return allowVerified;
	}

	public void setAllowVerified(boolean allowVerified) {
		this.allowVerified = allowVerified;
	}
	
	public boolean isPopular() {
		return popular;
	}

	public void setPopular(boolean popular) {
		this.popular = popular;
	}

	public boolean accept(Status status)
	{
		User user = status.getUser();
		if (status.isRetweet())
		{
			if (!allowRetweets)
				return false;
		}
		if (status.isPossiblySensitive())
		{
			if (!allowSensitive)
				return false;
		}
		boolean verified = user.isVerified();
		if (verified)
		{
			if (!allowVerified)
				return false;
		}
		else
		{
			if (!allowUnverified)
				return false;
		}
		return true;
	}
	
}
