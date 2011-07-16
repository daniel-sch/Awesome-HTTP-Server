import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpRequest {
	
	public class InvalidHttpRequest extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	private static Pattern regex = Pattern.compile(
			"^([A-Z]+) ([a-zA-Z0-9/\\-_.~!#$%&'()*+,/:;=?@\\[\\]]+) HTTP/(\\d\\.\\d)\\n((?:[A-Za-z0-9\\-]+: .+\\n)*)$");
	private Map<String, String> headers;
	private String method;
	private String path;
	private String version;

	public HttpRequest(String request) throws InvalidHttpRequest {
		headers = new HashMap<String, String>();
		
		Matcher matcher = regex.matcher(request);
		if(! matcher.matches()) {
			throw new InvalidHttpRequest();
		}
		
		method = matcher.group(1);
		path = stripPath(matcher.group(2));
		version = matcher.group(3);
		
		for(String line: matcher.group(4).split("\n")) {
			String[] parts = line.split(": ", 2);
			if(parts.length != 2)
				continue;
			headers.put(parts[0], parts[1]);
		}
		
		if(version.equals("1.1") && !headers.containsKey("Host"))
			throw new InvalidHttpRequest();
	}

	public String getHeader(String type) {
		return headers.get(type);
	}

	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}
	
	public String getVersion() {
		return version;
	}
	
	private String stripPath(String path) {
		if(path.indexOf('?') != -1)
			path = path.substring(0, path.indexOf('?'));
		if(path.indexOf('#') != -1)
			path = path.substring(0, path.indexOf('#'));
		
		try {
			path = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return path;
	}
}
