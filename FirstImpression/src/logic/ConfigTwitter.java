package logic;

import java.io.IOException;
import java.util.Properties;

import org.omg.CORBA.portable.InputStream;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class ConfigTwitter {
	
	private static String CONSUMER_KEY;
	private static String CONSUMER_SECRET;
	
	static
	{
		Properties props = new Properties();
		try {
			props.load(ConfigTwitter.class.getClassLoader().getResourceAsStream("apikey.props"));
			CONSUMER_KEY = props.getProperty("oath.consumer", null);
			CONSUMER_SECRET = props.getProperty("oath.secret",null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	//Plug in your own twitter api key.
	public static TwitterFactory createFactory()
	{
		ConfigurationBuilder builder = new ConfigurationBuilder().
				setApplicationOnlyAuthEnabled(true).
				setTweetModeExtended(true)
				.setOAuthConsumerKey(CONSUMER_KEY)
				.setOAuthConsumerSecret(CONSUMER_SECRET);
		TwitterFactory factory = new TwitterFactory(builder.build());
		return factory;
	}
	
	public static Twitter getTwitter()
	{
		return createFactory().getInstance();
	}
}
