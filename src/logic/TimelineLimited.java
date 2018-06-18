package logic;

public class TimelineLimited extends StatusesLimited {
	private String handle;
	
	
	
	public String getHandle() {
		return handle;
	}



	public void setHandle(String handle) {
		this.handle = handle;
	}



	public String toString()
	{
		if (isLimited())
		{
			return "PARTIAL RESULTS FOR HANDLE " + handle;
		}
		else
		{
			return "COMPLETE RESULTS FOR HANDLE " + handle;
		}
	}

}
