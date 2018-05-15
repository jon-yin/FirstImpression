package logic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import logic.TweetDownloader.VideoQuality;
import twitter4j.MediaEntity;
import twitter4j.MediaEntity.Variant;
import twitter4j.Status;

/**
 * An improved version of my original TweetDownloader which supports multithreading, a data limit, and reusability.
 * Will support zip file downloading in the future.
 * @author Jonathan Yin
 *
 */
public class TweetDownloaderImproved {
	
	private int threads;
	public static final String PHOTO_EXTENSION = ".jpg";
	public static final String GIF_EXTENSION = ".gif";
	public DownloadStyle style = DownloadStyle.SEPERATE_UNZIPPED;
	private String filepath;
	public static final int BUFFER_SIZE = 1024;
	private long limit;
	private long left;
	private Preferences prefs;

	/**
	 * Decides whether to download the text & media of tweets as a seperate text
	 * & img file or as an HTML file, right now, this only supports seperate
	 * file downloading.
	 * 
	 * @author Jonathan Yin
	 *
	 */
	public enum DownloadStyle {
		SEPERATE_UNZIPPED, SEPERATE_ZIPPED, HTML
	}

	public TweetDownloaderImproved(String filepath, int threads, long limit) {
		this.filepath = filepath;
		this.threads = threads;
		this.limit = limit;
	}

	/**
	 * Constructs a reusable downloader client for Status objects. Filepath is drawn either from preferences or in the cwd if 
	 * such a preference doesn't exist yet.
	 * @param threads Number of threads to run the downloader with (<= 1 is interpreted as single threaded)
	 * @param limit Limit in bytes of the amount of data to download from statuses.
	 */
	public TweetDownloaderImproved(int threads, long limit) {
		Preferences prefs = Preferences.userRoot();
		prefs = prefs.node(UtilityMethods.PREFERENCES_PATH);
		this.prefs = prefs;
		filepath = prefs.get("defaultpath", "./twitterdump");
		Path path = Paths.get(filepath);
		filepath = path.toAbsolutePath().normalize().toString();
		System.out.println(filepath);
		this.threads = threads;
		// service = Executors.newFixedThreadPool(threads);
		this.limit = limit;
	}

	/**
	 * Retrieves the URLs of the media (videos and images) present in this status.
	 * @param status tweet to retreive from
	 * @return A list of URLs linking to the media data.
	 */
	public List<URL> getMedia(Status status) {
		MediaEntity[] entities = status.getMediaEntities();
		List<URL> medias = new ArrayList<>();
		for (MediaEntity entity : entities) {
			if (entity.getType().equals("video")) {
				medias.add(downloadVideo(entity.getVideoVariants(), VideoQuality.HIGHEST));
			} else {
				medias.add(downloadImage(entity));
			}
		}
		return medias;
	}

	private URL downloadImage(MediaEntity entity) {
		String type = entity.getType();
		try {
			URL mediaURL = new URL(entity.getMediaURL());
			return mediaURL;
		} catch (MalformedURLException e) {
			System.err.println("Could not interpret Twitter Media URL.");
			return null;
		}

	}

	private URL downloadVideo(Variant[] variants, VideoQuality quality) {
		List<Variant> videos = new ArrayList<>();
		videos.addAll(Arrays.asList(variants));
		videos.sort(Comparator.comparing(Variant::getBitrate));
		if (videos.size() == 0)
			return null;
		URL desiredVideo = null;
		int index = 0;
		try {
			switch (quality) {
			case VERYLOW:
				desiredVideo = new URL(videos.get(0).getUrl());
				break;
			case LOW:
				index = videos.size() / 4;
				desiredVideo = new URL(videos.get(index).getUrl());
				break;
			case MEDIUM:
				index = videos.size() / 2;
				desiredVideo = new URL(videos.get(index).getUrl());
				break;
			case HIGH:
				index = (int) (videos.size() * (0.75));
				desiredVideo = new URL(videos.get(index).getUrl());
				break;
			case HIGHEST:
				desiredVideo = new URL(videos.get(videos.size() - 1).getUrl());
				break;
			default:
				System.err.println("Unrecognized Quality Option");
				return null;
			}
			return desiredVideo;
		} catch (MalformedURLException e) {
			System.out.println("Could not interpret Twitter Media URL");
			return null;
		}
	}

	/**
	 * Downloads data from a URL and places it into a byte array.
	 * @param source The URL to download data from.
	 * @return A byte array of the data retrieved from the URL.
	 */
	public byte[] downloadMediaFromURL(URL source) {
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			BufferedInputStream stream = new BufferedInputStream(source.openStream());
			ByteArrayOutputStream writeTo = new ByteArrayOutputStream();
			int read = 0;
			while ((read = stream.read(buffer)) != -1) {
				writeTo.write(buffer, 0, read);
			}
			stream.close();
			return writeTo.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not download media from " + source.toString());
			return null;
		}
	}

	private DownloadInfo checkTweetSize(Status status) {
		String body = status.getText();
		long totalLength = body.getBytes().length;
		List<URL> medias = getMedia(status);
		if (!(medias == null)) {
			List<byte[]> data = medias.stream().map(url -> downloadMediaFromURL(url)).collect(Collectors.toList());
			totalLength += data.stream().mapToLong(array -> array.length).sum();
			if (data.size() == medias.size()) {
				return new DownloadInfo(totalLength, body, data, medias);
			}
		}
		return null;
	}

	private long downloadTweets(List<Status> statuses, boolean mediaOnly) {
		left = limit;
		File rootpath = new File(filepath);
		String dirname = LocalDateTime.now().toString().replaceAll(":", "-");
		File rootdir = new File(rootpath, "/" + dirname);
		rootdir.mkdir();
		long totalSize = 0;
		for (Status status : statuses) {
			boolean download = false;
			DownloadInfo info = checkTweetSize(status);
			long size = info.getSize();
			if (mediaOnly)
			{
				size -= info.getBody().getBytes().length;
			}
			if (info != null && limit != -1) {
				synchronized (this) {
					if (size <= left) {
						left -= size;
						download = true;
					}
					
				}
			} else {
				download = true;
			}
			if (download) {
				String date = UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt()).toString();
				String windowsFriendly = date.replaceAll(":", "-");
				if (downloadFiles(rootdir, status.getUser().getScreenName() + windowsFriendly, info, null, mediaOnly))
				{
					totalSize += size;
				}
			}
			download = false;
		}
		return totalSize;
	}

	private long downloadTweetsTextOnly(List<Status> statuses) {
		left = limit;
		File rootpath = new File(filepath);
		String dirname = LocalDateTime.now().toString().replaceAll(":", "-");
		File rootdir = new File(rootpath, "/" + dirname);
		rootdir.mkdir();
		long totalSize = 0;
		boolean download = false;
		for (Status status : statuses) {
			long size = status.getText().getBytes().length;
			if (limit != -1) {
				synchronized (this) {
					if (size <= left) {
						left -= size;
						download = true;
					}
				}
			} else {
				download = true;
			}
			if (download) {
				String date = UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt()).toString();
				String windowsFriendly = date.replaceAll(":", "-");
				if (downloadFilesTextOnly(rootdir, status.getUser().getScreenName() + windowsFriendly, status.getText(),
						null)) {
					totalSize += size;
				}
			}
			download = false;
		}
		return totalSize;
	}

	private boolean downloadFilesTextOnly(File rootdir, String name, String text, ZipOutputStream zip) {
		try {
			// Ensure not just white space.
			if (!text.matches("\\s*")) {
				String bodyname = "/" + name + "-text.txt";
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(rootdir, bodyname)));
				writer.write(text);
				writer.close();
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean downloadFiles(File parent, String name, DownloadInfo info, ZipOutputStream zip, boolean mediaOnly) {
		String body = info.getBody();
		try {
			if (!body.matches("\\s*") && !mediaOnly) {
				String bodyname = "/" + name + "-text.txt";
				FileWriter stream = new FileWriter(new File(parent, bodyname));
				stream.write(body);
				stream.close();
			}
			List<URL> urls = info.getOriginalURLs();
			List<byte[]> data = info.getDatas();
			for (int i = 0; i < urls.size(); i++) {
				String extension = getExtensionFromURL(urls.get(i));
				String medianame = "/" + name + "-media" + i + extension;
				File media = new File(parent, medianame);
				BufferedOutputStream outputstream = new BufferedOutputStream(new FileOutputStream(media));
				outputstream.write(data.get(i));
				outputstream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Twitter media urls work by having an extension ex .mp4?tag=#. This method
	 * retrieves the proper media extension, ex .mp4.
	 * 
	 * @param url
	 *            The url to convert to an extension.
	 * @return Extension of the url.
	 */
	public String getExtensionFromURL(URL url) {
		String queryString = url.toString().substring(url.toString().lastIndexOf("."));
		if (queryString.contains("?")) {
			return queryString.substring(0, queryString.indexOf("?"));
		} else {
			return queryString;
		}
	}
	
	/**
	 * Generic method which splits a List of items into evenly partitioned sublists.
	 * @param items The total list to split data from.
	 * @param partitions The number of sublists to form
	 * @return A List of lists where each list contains relatively the same number of elements. Note that the number of sublists
	 * may be less than the numbe of partitions requested based on the size of the items list.
	 */
	public <T> List<List<T>> splitList(List<T> items, int partitions)
	{
		List<List<T>> split= new ArrayList<>();
		if (items.size() <= partitions)
		{
			for (int i = 0; i < items.size(); i++)
			{
				split.add(items.subList(i, i+1));
			}
		}
		else
		{
			for (int i = 0; i < partitions; i++)
			{
				List<T> splitList = items.subList(items.size() * i / partitions, items.size() * (i + 1) / partitions);
				split.add(splitList);
			}
		}
		return split;
	}
	
	/**
	 * Downloads the list of statuses into the directory filepath.
	 * @param statuses The tweets to download data from.
	 * @param mediaOnly Whether to include only media, not text
	 * @param textOnly Whether to include only text, not media (if this is true, then mediaOnly is ignored)
	 * @return The total number of bytes downloaded.
	 */
	public long startDownload(List<Status> statuses, boolean mediaOnly, boolean textOnly)
	{
		left = limit;
		if (threads <= 1)
		{
			if (textOnly)
			{
				return downloadTweetsTextOnly(statuses);
			}
			else
			{
				return downloadTweets(statuses,mediaOnly);
			}
		}
		else
		{
			ExecutorService service = Executors.newFixedThreadPool(threads);
			List<List<Status>> partitioned = splitList(statuses, threads);
			List<Future<Long>> results = new ArrayList<>();
			for (List<Status> partitionedWork : partitioned)
			{
				results.add(service.submit(new DownloadThread(partitionedWork, textOnly, mediaOnly)));
			}
			long totalSize = 0;
			for (Future<Long> result : results)
			{
				try {
					totalSize += result.get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			service.shutdown();
			return totalSize;
		}
	}

	public String getFilepath() {
		return filepath;
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	public int getThreads() {
		return threads;
	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public long getLimit() {
		return limit;
	}

	public void setLimit(long limit) {
		this.limit = limit;
	}
	
	public class DownloadThread implements Callable<Long>
	{
		
		private List<Status> statuses;
		private boolean textOnly;
		private boolean mediaOnly;
		
		public DownloadThread(List<Status> statuses, boolean textOnly, boolean mediaOnly)
		{
			this.statuses = statuses;
			this.textOnly = textOnly;
			this.mediaOnly = mediaOnly;
		}
		
		@Override
		public Long call() throws Exception {
			if (textOnly)
			{
				return downloadTweetsTextOnly(statuses);
			}
			else
			{
				return downloadTweets(statuses, mediaOnly);
			}
		}
		
	}

}
