package logic;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class ConfigTwitter {
	
	public static TwitterFactory createFactory()
	{
		ConfigurationBuilder builder = new ConfigurationBuilder().setOAuthConsumerKey("5lb6tQyiJmaM5BvJ8BMNyvf3A").
				setOAuthConsumerSecret("B6WIVc0m8VhSrXMyrcq8ErLqk4ZvjPGL52oXkogeG0CIk5SJ7h").
				setOAuthAccessToken("996202580576489472-XJoeHClkzOfz8eubcMWdr1sKfA21nee").
				setOAuthAccessTokenSecret("7yC11G1BKmahQsYX1sRSPUpcSwdpC998SXVBZeLmAjTlT").
				setTweetModeExtended(true);
		TwitterFactory factory = new TwitterFactory(builder.build());
		return factory;
	}
	
	public static Twitter getTwitter()
	{
		return createFactory().getInstance();
	}
}
