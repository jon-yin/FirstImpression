package logic;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import twitter4j.MediaEntity;
import twitter4j.MediaEntity.Variant;
import twitter4j.Status;

public class TweetDownloader {

	public static int THREADS = 5;
	public static final String PHOTO_EXTENSION = ".jpg";
	public static final String GIF_EXTENSION = ".gif";
	public static final String FILEPATH = "C:\\Users\\hgfddfgh\\Documents\\ReadForProject\\TwitterDump\\";
	public static int BUFFER_SIZE = 1024;
	private ExecutorService service;

	public TweetDownloader() {
		service = Executors.newFixedThreadPool(THREADS);
	}

	public long downloadMedia(Status status, String directory) {
		//System.out.println("DOWNLOADING!");
		long totalSize = 0;
		MediaEntity[] entities = status.getMediaEntities();
		LocalDateTime datetime = UtilityMethods.convertDateToLocalDateTime(status.getCreatedAt());
		String windowsFriendly = datetime.toString();
		windowsFriendly = windowsFriendly.replaceAll(":", "-");
		String name = status.getUser().getScreenName() + windowsFriendly;
		for (MediaEntity entity : entities) {
			if (entity.getType().equals("video")) {
				totalSize += downloadVideo(entity.getVideoVariants(), VideoQuality.HIGHEST, name, directory);
			} else {
				totalSize += downloadImage(entity, name, directory);
			}
		}
		return totalSize;
	}

	public long downloadImage(MediaEntity entity, String name, String directory) {
		String type = entity.getType();
		try {
			URL mediaURL = new URL(entity.getMediaURL());
			if (type.equals("photo")) {
				return downloadMediaFromURL(mediaURL, PHOTO_EXTENSION, name, directory);
			} else {
				return downloadMediaFromURL(mediaURL, GIF_EXTENSION, name, directory);
			}
		} catch (MalformedURLException e) {
			System.err.println("Could not interpret Twitter Media URL.");
			return 0;
		}

	}

	public long downloadVideo(Variant[] variants, VideoQuality quality, String name, String directory) {
		List<Variant> videos = new ArrayList<>();
		videos.addAll(Arrays.asList(variants));
		videos.sort(Comparator.comparing(Variant::getBitrate));
		if (videos.size() == 0)
			return 0;
		URL desiredVideo = null;
		String type = null;
		int index = 0;
		try {
			switch (quality) {
			case VERYLOW:
				desiredVideo = new URL(videos.get(0).getUrl());
				type = videos.get(0).getContentType();
				break;
			case LOW:
				index = videos.size() / 4;
				desiredVideo = new URL(videos.get(index).getUrl());
				type = videos.get(index).getContentType();
				break;
			case MEDIUM:
				index = videos.size() / 2;
				desiredVideo = new URL(videos.get(index).getUrl());
				type = videos.get(index).getContentType();
				break;
			case HIGH:
				index = (int) (videos.size() * (0.75));
				desiredVideo = new URL(videos.get(index).getUrl());
				type = videos.get(index).getContentType();
				break;
			case HIGHEST:
				desiredVideo = new URL(videos.get(videos.size() - 1).getUrl());
				type = videos.get(videos.size() - 1).getContentType();
				break;
			default:
				System.err.println("Unrecognized Quality Option");
				return 0;
			}
			if (desiredVideo == null || type == null) {
				return 0;
			} else {
				String extension = "." + type.split("/")[1];
				return downloadMediaFromURL(desiredVideo, extension, name, directory);
			}
		} catch (MalformedURLException e) {
			System.out.println("Could not interpret Twitter Media URL");
			return 0;
		}
	}

	public long downloadMediaFromURL(URL source, String extension, String name, String directory) {
		//System.out.println("DOWNLOADING FOR REAL?");
		byte[] buffer = new byte[BUFFER_SIZE];
		long downloaded = 0;
		try {
			BufferedInputStream stream = new BufferedInputStream(source.openStream());
			ByteArrayOutputStream writeTo = new ByteArrayOutputStream();
			int read = 0;
			while ((read = stream.read(buffer)) != -1) {
				writeTo.write(buffer, 0, read);
				downloaded += read;
			}
			stream.close();
			FileOutputStream fileOut = new FileOutputStream(new File(directory + name + extension));
			fileOut.write(writeTo.toByteArray());
			fileOut.close();
			return downloaded;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not download media from " + source.toString());
			return 0;
		}
	}

	public class ListDownloader implements Callable<Long> {
		List<Status> myStatuses;
		String directory;

		public ListDownloader(List<Status> toProcess) {
			myStatuses = toProcess;
			//System.out.println(myStatuses.size());
		}

		@Override
		public Long call() throws Exception {
			long totalSize = 0;
			if (myStatuses.size() == 0) {
				directory = FILEPATH;
			} else {
				directory = FILEPATH + myStatuses.get(0).getUser().getScreenName() + "/";
				File newDirectory = new File(directory);
				if (!newDirectory.exists()) {
					newDirectory.mkdir();
				}
			}
			int i = 0;
			for (Status status : myStatuses) {
				totalSize += downloadMedia(status, directory);
			}
			return totalSize;
		}

	}

	public long downloadList(List<Status> statuses) {
		int totalSize = statuses.size();
		//System.out.println(statuses.size());
		long byteSize = 0;
		List<Future<Long>> futures = new ArrayList<>();
		if (totalSize >= THREADS) {
			for (int i = 0; i < THREADS; i++) {
				List<Status> splitList = statuses.subList(totalSize * i / THREADS, totalSize * (i + 1) / THREADS);
				futures.add(service.submit(new ListDownloader(splitList)));
			}
		}
		else
		{
			for (int i = 0; i < totalSize; i++)
			{
				List<Status> splitList = statuses.subList(i,i+1);
				futures.add(service.submit(new ListDownloader(splitList)));	
			}
		}
		for (Future<Long> future : futures)
		{
			try {
				byteSize += future.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return byteSize;
	}

	public enum VideoQuality {
		VERYLOW, LOW, MEDIUM, HIGH, HIGHEST;
	}

	public void shutdown() {
		service.shutdown();
	}

}
