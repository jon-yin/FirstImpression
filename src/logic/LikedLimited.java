package logic;

public class LikedLimited extends UserTweetLimited {
	
	@Override
	public String toString()
	{
		String retStr;
		if (isLimited())
		{
			retStr =  "PARTIAL RESULTS FOR HANDLE " + getHandle() + " FAVORITES.\n";
			retStr += getStatuses() == null ? 0 : getStatuses().size() + " TWEETS RETRIEVED";
			return retStr;
		}
		else
		{
			retStr = "COMPLETE RESULTS FOR HANDLE " +getHandle() + " FAVORITES. ";
			retStr += getStatuses() == null ? 0 : getStatuses().size() + " TWEETS RETRIEVED";
			return retStr;
		}
	}
}
