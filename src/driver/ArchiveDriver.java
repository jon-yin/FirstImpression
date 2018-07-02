package driver;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import logic.Retriever;
import twitter4j.Status;

public class ArchiveDriver {
	
	public static final String FILEPATH = "C:\\Users\\hgfddfgh\\Documents\\ReadForProject\\TwitterDump";
	
	public static void main(String[] args) throws Exception {
		Retriever.getAllTweets("realDonaldTrump", 200);
		
		
	}
}
