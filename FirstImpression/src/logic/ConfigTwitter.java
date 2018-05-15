package logic;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class ConfigTwitter {
	
	//Plug in your own twitter api key.
	public static TwitterFactory createFactory()
	{
		ConfigurationBuilder builder = new ConfigurationBuilder().setOAuthConsumerKey().
				setOAuthConsumerSecret().
				setOAuthAccessToken().
				setOAuthAccessTokenSecret().
				setTweetModeExtended(true);
		TwitterFactory factory = new TwitterFactory(builder.build());
		return factory;
	}
	
	public static Twitter getTwitter()
	{
		return createFactory().getInstance();
	}
}
