package core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.lucene.analysis.util.CharArraySet;

/**
 * Romanian Stopwords, using the two types of diactritics.
 * Example: ş, ș.
 */
public class RomanianStopWords {
	
	private static CharArraySet _stopWords = new CharArraySet(0, true);

	static {
		readIntoStopWords("stopwords_ro1.txt");
		readIntoStopWords("stopwords_ro2.txt");
	}
	
	static private void readIntoStopWords(String fileName) {
		try (
			InputStream inputStream = RomanianStopWords.class.getResourceAsStream(fileName);
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
