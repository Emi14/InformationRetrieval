package core;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.tartarus.snowball.ext.RomanianStemmer;

public class RomanianAnalyzerWithoutDiacritics extends StopwordAnalyzerBase {
	
	public RomanianAnalyzerWithoutDiacritics() {
		super(RomanianAnalyzer.getDefaultStopSet());
	}
	
	public RomanianAnalyzerWithoutDiacritics(CharArraySet set){
		super(set);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName)
    {
        final Tokenizer source = new StandardTokenizer();
        
        this.getFullStopWordsSet();

        TokenStream tokenStream = source;
        tokenStream = new StandardFilter(tokenStream);
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, getFullStopWordsSet());
        tokenStream = new SnowballFilter(tokenStream, new RomanianStemmer()); // stemmer - flexionar forms (!)
        tokenStream = new ASCIIFoldingFilter(tokenStream); // replacing diacritics (!)

        return new TokenStreamComponents(source, tokenStream);
    }
    
    public CharArraySet getFullStopWordsSet() {
    	
    	CharArraySet stopWords = CharArraySet.copy(getStopwordSet());
    	CharArraySet stopWordsWithoutDiacritics = getStopwordSet();
    	java.util.Iterator<Object> iterator = stopWordsWithoutDiacritics.iterator();
    	while (iterator.hasNext()) {
    		 char[] stopWord = (char[]) iterator.next();
    		 char[] output = new char[stopWord.length];
    		 ASCIIFoldingFilter.foldToASCII(stopWord, 0, output, 0, stopWord.length);
    		 stopWords.add(String.valueOf(output));
    	}
    	
    	System.out.println(stopWords.toString());
    	
    	return stopWords;
    }
}
