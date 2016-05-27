package core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
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
	
	public class SearchResult {
		public TextFragment[] fragments;
		public Document document;
		public ScoreDoc scoreDoc;
		public int docId;
	}
	
	private FileIndexer _fileIndexer;
	
	public DocumentSearcher(FileIndexer fileIndexer) {
		if (fileIndexer == null) {
			throw new NullPointerException("FileIndexer cannot be null.");
		}
		_fileIndexer = fileIndexer;
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
	
	/**
	 * Searches on the 'content' field and additionally on 'abstract' field.
	 * The 'content' field is less boosted than 'abstract' field.
	 */
	public SearchResult[] search(String queryString) throws ParseException, IOException {
		_fileIndexer.updateReader();
		
		Analyzer analyzer = _fileIndexer.getAnalyzer();
		IndexReader indexReader = _fileIndexer.getIndexReader();
		
		Query contentQuery = new QueryParser("content", analyzer).parse(queryString);
		Query abstractQuery = new QueryParser("abstract", analyzer).parse(queryString);
		BoostQuery boostedContentQuery = new BoostQuery(contentQuery, 0.2f);
		BoostQuery boostedAbstractQuery = new BoostQuery(abstractQuery, 0.8f);
		
		BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
		booleanQueryBuilder.add(boostedContentQuery, Occur.MUST);
		booleanQueryBuilder.add(boostedAbstractQuery, Occur.SHOULD);	
		
		Query query = booleanQueryBuilder.build();
		
		int hitsPerPage = 10;
	    IndexSearcher searcher = new IndexSearcher(indexReader);
	    TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
	    searcher.search(booleanQueryBuilder.build(), collector);
	    
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    SearchResult[] searchResults = new SearchResult[hits.length];
	    
	    for (int i = 0; i < hits.length; i++) {
	    	SearchResult result = new SearchResult();
	    	ScoreDoc scoreDoc = result.scoreDoc = hits[i];
	    	int id = result.docId = scoreDoc.doc;
    		Document doc = result.document = searcher.doc(id);
    		String text = doc.get("content");
    		ArrayList<TextFragment> fragsList = new ArrayList<TextFragment>();
    		
    		int fragSize = Math.min(50, text.length());
    		int fragCount = text.length() / fragSize;
    		
    		try {
				TextFragment[] fragsArray = getFragmentsWithHighlitedText(query, "content", text, fragSize, fragCount);
				Stream.of(fragsArray).filter(frag -> frag != null && frag.getScore() > 0).forEachOrdered(fragsList::add);
			} catch (InvalidTokenOffsetsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		result.fragments = fragsList.toArray(new TextFragment[fragsList.size()]);
    		searchResults[i] = result;
	    }
	    
	    return searchResults;
	}
}
