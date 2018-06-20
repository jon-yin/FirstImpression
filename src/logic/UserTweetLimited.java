package logic;

public abstract class UserTweetLimited extends StatusesLimited {

private String handle;
	
	public String getHandle() {
		return handle;
	}



	public void setHandle(String handle) {
		this.handle = handle;
	}
}
