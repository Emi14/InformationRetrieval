package core;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;

public class DocumentSearcher {
	private FileIndexer _fileIndexer;
	
	public DocumentSearcher(FileIndexer fileIndexer) {
		if (fileIndexer == null) {
			throw new NullPointerException("FileIndexer cannot be null.");
		}
		_fileIndexer = fileIndexer;
	}
	
	public Document[] search(String queryString) throws ParseException, IOException {
		return search(queryString, "content");
	}
	
	private TextFragment[] getFragmentsWithHighlitedText(Query query, String fieldName, String fieldValue, int fragmentSize, int maxFragments) throws IOException, InvalidTokenOffsetsException {
		Analyzer analyzer = _fileIndexer.getAnalyzer();
		SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
		TokenStream stream = TokenSources.getTokenStream(fieldName, fieldValue, analyzer);
		QueryScorer scorer = new QueryScorer(query, fieldName);
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, fragmentSize);
		   
		Highlighter highlighter = new Highlighter(htmlFormatter, scorer);
		highlighter.setTextFragmenter(fragmenter);      
		TextFragment[] fragments = highlighter.getBestTextFragments(stream, fieldValue, false, maxFragments);
		return fragments;
	}
	
	public Document[] search(String queryString, String where) throws ParseException, IOException {
		_fileIndexer.updateReader();
		
		Analyzer analyzer = _fileIndexer.getAnalyzer();
		IndexReader indexReader = _fileIndexer.getIndexReader();
		
		Query query = new QueryParser(where, analyzer).parse(queryString);
		int hitsPerPage = 10;
	    IndexSearcher searcher = new IndexSearcher(indexReader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
	    searcher.search(query, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    Document[] docs = new Document[hits.length];
	    for (int i = 0; i < hits.length; i++) {
	    	docs[i] = searcher.doc(hits[i].doc);
	    }
	    
    	for (int i = 0; i < hits.length; i++) {
    		int id = hits[i].doc;
    		Document doc = searcher.doc(id);
	    	String text = doc.get(where);
	    	TextFragment[] frags;
			try {
				frags = getFragmentsWithHighlitedText(query, "content", text /* text.trim().replaceAll("\n+", " ") */, 100, text.length() / 100);
				for (int j = 0; j < frags.length; j++) {
					if ((frags[j] != null) && (frags[j].getScore() > 0)) {
						System.out.println(frags[j].toString().trim().replaceAll("\n+", " "));
						System.out.println("+++++++++++");
					}
				}
			} catch (InvalidTokenOffsetsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	System.out.println("-----------");
    	}
	    
	    return docs;
	}
}
