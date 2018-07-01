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
		List<String> urls = new ArrayList<>();
		urls.add("https://twitter.com/a_broken_fan/status/1011466277225787392");
		List<Status> st = Retriever.urlToStatus(urls, false);
		String text = st.get(0).getText();
		System.out.println(text);
		
		
	}
}
