import java.text.CharacterIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


public class HttpUtils {
	public static String formatDate(Date d) {
		SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
		return df.format(d);
	}
	
	public static Date parseDate(String text) {
		String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
		SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
		try {
			return format.parse(text);
		} catch (ParseException e) {
			return null;
		}
	}
	
	// from http://snippets.dzone.com/posts/show/91 
	public static String join(List<String> s, String delimiter) {
		if (s.isEmpty())
			return "";
		Iterator<String> iter = s.iterator();
		StringBuilder buffer = new StringBuilder(iter.next());
		while (iter.hasNext())
			buffer.append(delimiter).append(iter.next());
		return buffer.toString();
	}

	// from http://www.javapractices.com/topic/TopicAction.do?Id=96
	public static String escapeHtml(String aText) {
		final StringBuilder result = new StringBuilder();
		final StringCharacterIterator iterator = new StringCharacterIterator(aText);
		char character =  iterator.current();
		while (character != CharacterIterator.DONE ){
			if (character == '<') {
				result.append("&lt;");
			}
			else if (character == '>') {
				result.append("&gt;");
			}
			else if (character == '\"') {
				result.append("&quot;");
			}
			else if (character == '\'') {
				result.append("&#039;");
			}
			else if (character == '&') {
				result.append("&amp;");
			}
			else {
				result.append(character);
			}
			character = iterator.next();
		}
		return result.toString();
	}
}
