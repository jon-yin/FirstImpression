package logic;

import java.io.IOException;
import java.util.Properties;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.OAuth2Token;
import twitter4j.conf.ConfigurationBuilder;

public class ConfigTwitter {
	
	private static String CONSUMER_KEY;
	private static String CONSUMER_SECRET;
	public static Twitter twitter;
	
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
	
	//Plug in your own consumer twitter api key.
	public static TwitterFactory createFactory()
	{
		ConfigurationBuilder builder = new ConfigurationBuilder().
				setApplicationOnlyAuthEnabled(true)
				.setOAuthConsumerKey(CONSUMER_KEY)
				.setOAuthConsumerSecret(CONSUMER_SECRET)
				;
		TwitterFactory factory = new TwitterFactory(builder.build());
		// Obtain a bearer token to signify that this is application-only.
		try {
			OAuth2Token authorization = factory.getInstance().getOAuth2Token();
			builder = new ConfigurationBuilder().
					setApplicationOnlyAuthEnabled(true).
					setTweetModeExtended(true)
					.setOAuthConsumerKey(CONSUMER_KEY)
					.setOAuthConsumerSecret(CONSUMER_SECRET)
					.setOAuth2TokenType(authorization.getTokenType())
					.setOAuth2AccessToken(authorization.getAccessToken())
					;
		} catch (TwitterException e) {
			e.printStackTrace();
		}
		factory = new TwitterFactory(builder.build());
		return factory;
	}
	
	public static Twitter getTwitter()
	{
		if (twitter == null)
		{
			twitter = createFactory().getInstance();
		}
		return twitter;
	}
}
