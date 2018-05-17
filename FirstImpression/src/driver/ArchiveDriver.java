package driver;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import debug.TweetDebugger;
import logic.ConfigTwitter;
import logic.TweetDownloaderImproved;
import logic.TweetRetriever;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class ArchiveDriver {
	
	public static void main(String[] args) throws TwitterException {
		
		Twitter twitter = ConfigTwitter.getTwitter();
		twitter.set
		/*
		TweetRetriever retriever = new TweetRetriever();
		TweetDebugger debugger = new TweetDebugger();
		TweetDownloaderImproved mytest = new TweetDownloaderImproved(5, -1);
		List<Status> testStatuses = retriever.getAllTweets("jack",100);
		mytest.setLimit(-1);
		long startTime = System.nanoTime();
		mytest.setThreads(1);
		System.out.println(mytest.startDownload(testStatuses, false,false));
		System.out.println("Non-multithreaded-execution: " + (System.nanoTime()-startTime));
		mytest.setThreads(5);
		startTime = System.nanoTime();
		System.out.println(mytest.startDownload(testStatuses, false, false));
		System.out.println("Multithreaded-execution: " + (System.nanoTime()-startTime));
		*/
		
	}
}
