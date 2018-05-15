package logic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class UtilityMethods {
	
	public static final String PREFERENCES_PATH = "impression.first.my";

	public static LocalDateTime convertDateToLocalDateTime(Date input)
	{
		return input.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}
	
	public static double bytesToKB(long bytes)
	{
		return bytes / 1024.0;
	}
	
	public static double bytesToMB(long bytes)
	{
		return bytes / Math.pow(1024, 2);
	}
}
