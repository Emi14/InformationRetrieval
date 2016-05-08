package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.lucene.analysis.util.CharArraySet;

public class RomanianStopWords {
	
	private static CharArraySet _stopWords = new CharArraySet(0, true);

	static {
		try (
			InputStream inputStream = RomanianStopWords.class.getResourceAsStream("stopwords_ro.txt");
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
		    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				_stopWords.add(line);
		    }
		} catch (IOException e) {
			System.out.println("Unable to read the resource file stopwords_ro.txt: " + e.getMessage());
		}
	}
	
	static CharArraySet getStopList() {
		return _stopWords;
	}
}
