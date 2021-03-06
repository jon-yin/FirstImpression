package logic;

public class TimelineLimited extends UserTweetLimited {
	
	@Override
	public String toString()
	{
		String retStr;
		if (isLimited())
		{
			retStr =  "PARTIAL RESULTS FOR HANDLE " + getHandle() + " TIMELINE.\n";
			retStr += getStatuses() == null ? 0 : getStatuses().size() + " TWEETS RETRIEVED";
			return retStr;
		}
		else
		{
			retStr = "COMPLETE RESULTS FOR HANDLE " +getHandle() + " TIMELINE. ";
			retStr += getStatuses() == null ? 0 : getStatuses().size() + " TWEETS RETRIEVED";
			return retStr;
		}
	}
}
