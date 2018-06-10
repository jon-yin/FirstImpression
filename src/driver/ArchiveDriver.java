package driver;

import java.util.List;

import logic.TweetDownloaderImproved;
import logic.TweetRetriever;
import twitter4j.Status;
import twitter4j.TwitterException;

public class ArchiveDriver {
	
	public static void main(String[] args) throws TwitterException {
		TweetDownloaderImproved downloader = new TweetDownloaderImproved(1, -1);
		TweetRetriever retriever = new TweetRetriever();
		List<Status> tweets = retriever.getAllTweets("MeidocafeR", 200);
		//downloader.startDownload(tweets, false, false, true);
		downloader.startDownload(tweets, true, false, true);
		//downloader.startDownload(tweets, false, true, true);
		
		
	}
}
