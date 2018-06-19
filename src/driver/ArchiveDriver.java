package driver;

import java.util.List;

import logic.Retriever;
import logic.TweetDownloaderImproved;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ArchiveDriver {
	
	public static final String FILEPATH = "C:\\Users\\hgfddfgh\\Documents\\ReadForProject\\TwitterDump";
	
	public static void main(String[] args) throws TwitterException {
		List<Status> statuses = Retriever.search("Neptunia", 1000, null, null);
		TweetDownloaderImproved im = new TweetDownloaderImproved(FILEPATH, 10, -1);
		im.startDownload(statuses, true,false,false);
	}
}
