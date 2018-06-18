package logic;

import twitter4j.Query.ResultType;

public class SearchLimited extends StatusesLimited {
	private String query;
	private ResultType type;
	private String lang;
	
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
	
	public ResultType getType() {
		return type;
	}

	public void setType(ResultType type) {
		this.type = type;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	@Override
	public String toString()
	{
		if (isLimited())
		{
			return "PARTIAL RESULTS FOR SEARCH QUERY: " + query;
		}
		else
		{
			return "COMPLETE RESULTS FOR SEARCH QUERY: " + query;
		}
	}
	
}
