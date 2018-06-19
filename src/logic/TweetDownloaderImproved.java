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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import twitter4j.MediaEntity;
import twitter4j.MediaEntity.Variant;
import twitter4j.Status;

/**
 * An improved version of my original TweetDownloader which supports
 * multithreading, a data limit, and reusability. Will support zip file
 * downloading in the future.
 * 
 * @author Jonathan Yin
 *
 */
public class TweetDownloaderImproved {

	private int threads;
	public static final String PHOTO_EXTENSION = ".jpg";
	public static final String GIF_EXTENSION = ".gif";
	private String filepath;
	public static final int BUFFER_SIZE = 1024;
	private long limit;
	private long left;
	private VideoQuality quality;
	private AtomicInteger finished;
	private HashSet<Long> downloaded;
	private int numStatuses;

	/**
	 * This is used to determine what quality to download videos from, quality
	 * is determined by bitrate.
	 * 
	 * @author Jonathan Yin
	 *
	 */
	public enum VideoQuality {
		/**
		 * Smallest bitrate
		 */
		VERYLOW,
		/**
		 * 25th percentile in bitrate
		 */
		LOW,
		/**
		 * 50th percentile in bitrate
		 */
		MEDIUM,
		/**
		 * 75th percentile in bitrate
		 */
		HIGH,
		/**
		 * Best bitrate
		 */
		HIGHEST;
	}

	/**
	 * Constructs a reusable downloader client for Status objects.
	 * 
	 * @param filepath
	 *            The directory to dump the downloaded files
	 * @param threads
	 *            The number of threads to run the downloader with (fewer or
	 *            equal to 1 is considered single threaded)
	 * @param limit
	 *            Limit in bytes of amount of data to download, -1 is considered
	 *            as no limit.
	 * @param quality
	 *            The quality to download videos from.
	 */
	public TweetDownloaderImproved(String filepath, int threads, long limit, VideoQuality quality) {
		this.filepath = filepath;
		this.threads = threads;
		this.limit = limit;
		this.quality = quality;
	}

	/**
	 * Constructs a reusable downloader client for Status objects. Quality
	 * defaults to the highest resolution possible.
	 * 
	 * @param filepath
	 *            The directory to dump the downloaded files
	 * @param threads
	 *            The number of threads to run the downloader with (fewer or
	 *            equal to 1 is considered single threaded)
	 * @param limit
	 *            Limit in bytes of amount of data to download, -1 is considered
	 *            as no limit.
	 */
	public TweetDownloaderImproved(String filepath, int threads, long limit) {
		this.filepath = filepath;
		this.threads = threads;
		this.limit = limit;
		quality = VideoQuality.HIGHEST;
	}

	/**
	 * Constructs a reusable downloader client for Status objects. Filepath is
	 * drawn either from preferences or in the cwd if such a preference doesn't
	 * exist yet. Quality defaults to the highest resolution possible.
	 * 
	 * @param threads
	 *            Number of threads to run the downloader with ( fewer or equal
	 *            to 1 is interpreted as single threaded)
	 * @param limit
	 *            Limit in bytes of the amount of data to download from
	 *            statuses.
	 */
	public TweetDownloaderImproved(int threads, long limit) {
		filepath = (".");
		Path path = Paths.get(filepath);
		filepath = path.toAbsolutePath().normalize().toString();
		// System.out.println(filepath);
		this.threads = threads;
		this.limit = limit;
		quality = VideoQuality.HIGHEST;
	}

	/**
	 * Constructs a reusable downloader client for Status objects. Filepath is
	 * drawn either from preferences or in the cwd if such a preference doesn't
	 * exist yet.
	 * 
	 * @param threads
	 *            Number of threads to run the downloader with (fewer or equal
	 *            to 1 is interpreted as single threaded)
	 * @param limit
	 *            Limit in bytes of the amount of data to download from
	 *            statuses.
	 * @param quality
	 *            The video resolution quality to download videos at.
	 */
	public TweetDownloaderImproved(int threads, long limit, VideoQuality quality) {
		filepath = ".";
		Path path = Paths.get(filepath);
		filepath = path.toAbsolutePath().normalize().toString();
		// System.out.println(filepath);
		this.threads = threads;
		this.limit = limit;
		this.quality = quality;
	}

	/**
	 * Constructs a reusable downloader client for Status objects. Filepath is
	 * drawn either from preferences or in the cwd if such a preference doesn't
	 * exist yet. This will run single-threadly though this can be altered with
	 * setThreads.
	 * 
	 * @param limit
	 *            Limit in bytes of the amount of data to download from
	 *            statuses.
	 */
	public TweetDownloaderImproved(long limit) {
		;
		filepath = ".";
		Path path = Paths.get(filepath);
		filepath = path.toAbsolutePath().normalize().toString();
		// System.out.println(filepath);
		this.threads = 1;
		this.limit = limit;
		quality = VideoQuality.HIGHEST;
	}

	/**
	 * Retrieves the URLs of the media (videos and images) present in this
	 * status.
	 * 
	 * @param status
	 *            tweet to retreive from
	 * @return A list of URLs linking to the media data.
	 */
	public List<URL> getMedia(Status status) {
		MediaEntity[] entities = status.getMediaEntities();
		List<URL> medias = new ArrayList<>();
		for (MediaEntity entity : entities) {
			if (entity.getType().equals("video")) {
				medias.add(downloadVideo(entity.getVideoVariants(), quality));
			} else {
				medias.add(downloadImage(entity));
			}
		}
		return medias;
	}

	private URL downloadImage(MediaEntity entity) {
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
	 * 
	 * @param source
	 *            The URL to download data from.
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
		String body = UtilityMethods.getDisplayText(status);
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

	private long downloadTweets(List<Status> statuses, boolean mediaOnly, ZipOutputStream zstream, File dest) {
		// left = limit;
		long totalSize = 0;
		for (Status status : statuses) {
			signal();
			if (status.isRetweet()) {
				status = status.getRetweetedStatus();
			}
			synchronized (downloaded) {
				//Can save time by not downloading data to determine size.
				if (downloaded.contains(status.getId()))
				{
					continue;
				}
			}
			DownloadInfo info = checkTweetSize(status);
			long size = info.getSize();
			if (mediaOnly) {
				size -= info.getBody().getBytes().length;
			}
			synchronized (downloaded) {
				if (!downloaded.contains(status.getId())) {
					if (limit != -1) {
						if (size <= left) {
							left -= size;
							downloaded.add(status.getId());
						} else {
							downloaded.add(status.getId());
							continue;
						}
					} else {
						downloaded.add(status.getId());
					}
				} else {
					continue;
				}
			}

			String date = UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt()).toString();
			if (downloadFiles(dest, status.getUser().getScreenName() + status.getId(), info, zstream, mediaOnly)) {
				totalSize += size;
			}
		}
		return totalSize;
	}

	private long downloadTweetsTextOnly(List<Status> statuses, ZipOutputStream zstream, File dest) {
		left = limit;
		long totalSize = 0;
		for (Status status : statuses) {
			signal();
			if (status.isRetweet()) {
				status = status.getRetweetedStatus();
			}
			String body = UtilityMethods.getDisplayText(status);
			long size = body.getBytes().length;
			synchronized (downloaded) {
				if (!downloaded.contains(status.getId())) {
					if (limit != -1) {
						if (size <= left) {
							left -= size;
							downloaded.add(status.getId());
						} else {
							// Don't download, go to next iteration.
							downloaded.add(status.getId());
							continue;
						}
					} else {
						downloaded.add(status.getId());
					}
				} else {
					// Don't download, go to next iteration.
					continue;
				}
			}

			String date = UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt()).toString();

			if (downloadFilesTextOnly(dest, status.getUser().getScreenName() + status.getId(), body, zstream)) {
				totalSize += size;
			}
		}
		return totalSize;
	}

	private boolean downloadFilesTextOnly(File rootdir, String name, String text, ZipOutputStream zip) {
		try {
			// Ensure not just white space.
			if (!text.matches("\\s*")) {
				// Non zipped version
				if (zip == null) {
					String bodyname = "/" + name + "-text.txt";
					BufferedWriter writer = new BufferedWriter(new FileWriter(new File(rootdir, bodyname)));
					writer.write(text);
					writer.close();
				} else {
					String bodyname = name + "-text.txt";
					return writeZipEntry(zip, bodyname, text.getBytes(), false);
				}
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
				if (zip == null) {
					String bodyname = "/" + name + "-text.txt";
					FileWriter stream = new FileWriter(new File(parent, bodyname));
					stream.write(body);
					stream.close();
				} else {
					String bodyname = name + "-text.txt";
					if (!writeZipEntry(zip, bodyname, body.getBytes(), false)) {
						return false;
					}
				}
			}
			List<URL> urls = info.getOriginalURLs();
			List<byte[]> data = info.getDatas();
			for (int i = 0; i < urls.size(); i++) {
				String extension = getExtensionFromURL(urls.get(i));
				if (zip == null) {
					String medianame = "/" + name + "-media" + i + extension;
					File media = new File(parent, medianame);
					BufferedOutputStream outputstream = new BufferedOutputStream(new FileOutputStream(media));
					outputstream.write(data.get(i));
					outputstream.flush();
					outputstream.close();
				} else {
					String medianame = name + "-media" + i + extension;
					writeZipEntry(zip, medianame, data.get(i), false);
				}
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
	 * Generic method which splits a List of items into evenly partitioned
	 * sublists.
	 * 
	 * @param items
	 *            The total list to split data from.
	 * @param partitions
	 *            The number of sublists to form
	 * @return A List of lists where each list contains relatively the same
	 *         number of elements. Note that the number of sublists may be less
	 *         than the numbe of partitions requested based on the size of the
	 *         items list.
	 */
	public <T> List<List<T>> splitList(List<T> items, int partitions) {
		List<List<T>> split = new ArrayList<>();
		if (items.size() <= partitions) {
			for (int i = 0; i < items.size(); i++) {
				split.add(items.subList(i, i + 1));
			}
		} else {
			for (int i = 0; i < partitions; i++) {
				List<T> splitList = items.subList(items.size() * i / partitions, items.size() * (i + 1) / partitions);
				split.add(splitList);
			}
		}
		return split;
	}

	/**
	 * Downloads the list of statuses into the directory filepath.
	 * 
	 * @param statuses
	 *            The tweets to download data from.
	 * @param mediaOnly
	 *            Whether to include only media, not text
	 * @param textOnly
	 *            Whether to include only text, not media (if this is true, then
	 *            mediaOnly is ignored)
	 * @return The total number of bytes downloaded.
	 */
	public long startDownload(List<Status> statuses, boolean mediaOnly, boolean textOnly, boolean zip) {
		// Make the directory/Zipstream
		String dirname = LocalDateTime.now().toString().replaceAll(":", "-");
		// a zip was requested, make a zipoutputstream to contain the data.
		File destFile = new File(filepath, "/" + dirname);
		// Create hashset to ensure we don't download duplicate tweets.
		downloaded = new HashSet<>();
		finished = new AtomicInteger();
		numStatuses=statuses.size();
		ZipOutputStream stream = null;
		if (zip) {
			// Create a zip stream
			try {
				stream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destFile + ".zip")));
				stream.setLevel(9);
			} catch (IOException ex) {
				ex.printStackTrace();
				return 0;
			}
		} else {
			// Create a directory
			destFile.mkdir();
		}
		left = limit;
		if (threads <= 1) {
			long size;
			if (textOnly) {
				size = downloadTweetsTextOnly(statuses, stream, destFile);
			} else {
				size = downloadTweets(statuses, mediaOnly, stream, destFile);
			}
			if (zip) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return size;
		} else {
			ExecutorService service = Executors.newFixedThreadPool(threads);
			List<List<Status>> partitioned = splitList(statuses, threads);
			List<Future<Long>> results = new ArrayList<>();
			for (List<Status> partitionedWork : partitioned) {
				results.add(service.submit(new DownloadThread(partitionedWork, textOnly, mediaOnly, stream, destFile)));
			}
			long totalSize = 0;
			for (Future<Long> result : results) {
				try {
					totalSize += result.get();
					// Shouldn't happen.
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			service.shutdown();
			// Remember to close the ZipOutputStream (only if zip was enabled)
			if (zip) {
				try {
					stream.close();
				} catch (IOException exception) {
					exception.printStackTrace();
				}
			}
			return totalSize;
		}
	}
	
	private void signal()
	{
		int val = finished.incrementAndGet();
		UtilityMethods.info(val+ "/"+numStatuses+" finished");
	}
	
	/**
	 * Gets the directory where downloaded data will be delivered to
	 * 
	 * @return directory as String
	 */
	public String getFilepath() {
		return filepath;
	}

	/**
	 * Changes the directory where downloaded data will be delivered to
	 * 
	 * @param filepath
	 *            New directory to dump data.
	 */
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	/**
	 * Gets the number of threads to run when starting a download
	 * 
	 * @return number of threads to run per download
	 */
	public int getThreads() {
		return threads;
	}

	/**
	 * Changes the number of threads to run when starting a download. (fewer or
	 * equal to 1 will be singlethreaded)
	 * 
	 * @param threads
	 *            The new number of threads to run per download
	 */
	public void setThreads(int threads) {
		this.threads = threads;
	}

	/**
	 * The max number of bytes allowable to download per startDownload
	 * 
	 * @return max number of bytes to download
	 */

	public long getLimit() {
		return limit;
	}

	/**
	 * Changes the max number of bytes to download per download, -1 is
	 * considered no limit.
	 * 
	 * @param limit
	 *            The new max number of bytes to download.
	 */

	public void setLimit(long limit) {
		this.limit = limit;
	}

	/**
	 * Gets the preferred video quality to download from.
	 * 
	 * @return The video quality that future videos will be downloaded from.
	 */
	public VideoQuality getQuality() {
		return quality;
	}

	/**
	 * Changes the quality from which to download videos from.
	 * 
	 * @param quality
	 *            The new quality to download videos from.
	 */
	public void setQuality(VideoQuality quality) {
		this.quality = quality;
	}
	

	// Writes a zip entry in a way that is concurrency safe. (lock each entry
	// write). Assumes that name is well formatted to zip directory + files
	// within directories
	private boolean writeZipEntry(ZipOutputStream stream, String name, byte[] data, boolean isDirectory) {
		try {
			synchronized (stream) {
				if (isDirectory) {
					stream.putNextEntry(new ZipEntry(name));
					stream.closeEntry();
					return true;
				} else {
					stream.putNextEntry(new ZipEntry(name));
					stream.write(data);
					stream.closeEntry();
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public class DownloadThread implements Callable<Long> {

		private List<Status> statuses;
		private boolean textOnly;
		private boolean mediaOnly;
		private ZipOutputStream zipStream;
		private File dest;

		public DownloadThread(List<Status> statuses, boolean textOnly, boolean mediaOnly, ZipOutputStream stream,
				File dest) {
			this.statuses = statuses;
			this.textOnly = textOnly;
			this.mediaOnly = mediaOnly;
			this.zipStream = stream;
			this.dest = dest;
		}

		@Override
		public Long call() throws Exception {
			if (textOnly) {
				return downloadTweetsTextOnly(statuses, zipStream, dest);
			} else {
				return downloadTweets(statuses, mediaOnly, zipStream, dest);
			}
		}

	}

}
