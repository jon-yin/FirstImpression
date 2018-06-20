package driver;

import java.util.List;

import logic.Filterer;
import logic.Retriever;
import logic.TweetDownloaderImproved;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.Query.ResultType;

public class ArchiveDriver {
	
	public static final String FILEPATH = "C:\\Users\\hgfddfgh\\Documents\\ReadForProject\\TwitterDump";
	
	public static void main(String[] args) throws TwitterException {
		List<Status> statuses = Retriever.search("persona", 1000, "en", ResultType.popular);
		Filterer filter = new Filterer(statuses);
		filter.filter(Filterer.excludeRetweets())
		.filter(Filterer.atLeastLikes(500));
		List<Status> filtered = filter.getStatuses();
		for (Status status : filtered)
		{
			if (status.getFavoriteCount() < 500)
			{
				System.out.println("ERROR");
			}
		}
		TweetDownloaderImproved downloader = new TweetDownloaderImproved(FILEPATH, 10, -1);
		downloader.startDownload(filtered, false, false,false);
	}
}
