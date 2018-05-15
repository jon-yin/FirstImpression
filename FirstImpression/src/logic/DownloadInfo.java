package logic;

import java.net.URL;
import java.util.List;

public class DownloadInfo {
	private long size;
	private String body;
	private List<byte[]> datas;
	private List<URL> originalURLs;
	
	public DownloadInfo(long size, String body, List<byte[]> datas, List<URL> originalURLs) {
		super();
		this.size = size;
		this.body = body;
		this.datas = datas;
		this.originalURLs = originalURLs;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public List<byte[]> getDatas() {
		return datas;
	}
	public void setDatas(List<byte[]> datas) {
		this.datas = datas;
	}
	public List<URL> getOriginalURLs() {
		return originalURLs;
	}
	public void setOriginalURLs(List<URL> originalURLs) {
		this.originalURLs = originalURLs;
	}
	
	
	

}
