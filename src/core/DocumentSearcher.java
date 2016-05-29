package core;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
		public float score;
	}
	
	private FileIndexer _fileIndexer;
	
	public DocumentSearcher(FileIndexer fileIndexer) {
		if (fileIndexer == null) {
			throw new NullPointerException("FileIndexer cannot be null.");
		}
		_fileIndexer = fileIndexer;
	}
	
	private TextFragment[] getFragmentsWithHighlitedText(Query query, String fieldName, String fieldValue, int fragmentSize, int maxFragments, boolean merge) throws IOException, InvalidTokenOffsetsException {
		Analyzer analyzer = _fileIndexer.getAnalyzer();
		SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
		TokenStream stream = TokenSources.getTokenStream(fieldName, fieldValue, analyzer);
		QueryScorer scorer = new QueryScorer(query, fieldName);
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, fragmentSize);
		   
		Highlighter highlighter = new Highlighter(htmlFormatter, scorer);
		highlighter.setTextFragmenter(fragmenter);      
		highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
		TextFragment[] fragments = highlighter.getBestTextFragments(stream, fieldValue, merge, maxFragments);
		return fragments;
	}
	
	private ArrayList<TextFragment> getRelevantFragments(ArrayList<TextFragment> fragsList, String queryString) throws IOException {
		Analyzer analyzer = _fileIndexer.getAnalyzer();
		
		/* Get tokens from query. */
		Set<String> queryTokensSet = new HashSet<String>();
		TokenStream queryTokenStream = analyzer.tokenStream("content", new StringReader(queryString));
        CharTermAttribute queryCharTermAttribute = queryTokenStream.getAttribute(CharTermAttribute.class);
        queryTokenStream.reset();
		while (queryTokenStream.incrementToken()) {
			queryTokensSet.add(queryCharTermAttribute.toString());
		}
		queryTokenStream.close();
		
		class MappedFragment implements Comparable<MappedFragment> {
			public TextFragment fragment = null;
			public Set<String> tokens = new HashSet<String>();
			
			public MappedFragment(TextFragment fragment) {
				this.fragment = fragment;
			}
			
			@Override
		    public int compareTo(MappedFragment mf) {
		        return mf.tokens.size() - this.tokens.size();
		    }
		}
		
		/* For each fragment, map the fragment with it's unique tokens fragment */
		ArrayList<MappedFragment> mappedTokens = new ArrayList<MappedFragment>(fragsList.size());
		for (int index = 0; index < fragsList.size(); index++) {
			TextFragment fragment = fragsList.get(index);
			MappedFragment mp = new MappedFragment(fragment);
			TokenStream fragmentTokenStream = analyzer.tokenStream("content", new StringReader(fragment.toString()));
	        CharTermAttribute fragmentCharTermAttribute = fragmentTokenStream.getAttribute(CharTermAttribute.class);
	        fragmentTokenStream.reset();
			while (fragmentTokenStream.incrementToken()) {
				String fragmentToken = fragmentCharTermAttribute.toString();
				if (queryTokensSet.contains(fragmentToken)) {
					mp.tokens.add(fragmentToken);
				}
			}
			fragmentTokenStream.close();
			mappedTokens.add(mp);
		}
		
		/* Sort the mapped fragments by their tokens set size */
		Collections.sort(mappedTokens);
		
		ArrayList<TextFragment> newFragsList = new ArrayList<TextFragment>();
		
		/* Get only the first fragments which their tokens are contained completely
		 * into the query tokens. Then remove these tokens from the query tokens,
		 * so we don't get another fragment with common tokens.
		 */
		for (MappedFragment mappedFragment : mappedTokens) {
			if (queryTokensSet.containsAll(mappedFragment.tokens)) {
				queryTokensSet.removeAll(mappedFragment.tokens);
				newFragsList.add(mappedFragment.fragment);
			}
		}
		
		return newFragsList;
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
		BoostQuery boostedContentQuery = new BoostQuery(contentQuery, 1f);
		BoostQuery boostedAbstractQuery = new BoostQuery(abstractQuery, 0.6f);
		
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
	    	result.score = scoreDoc.score;
    		Document doc = result.document = searcher.doc(id);
    		String text = doc.get("content");
    		ArrayList<TextFragment> fragsList = new TextFragmentArrayList();
    		
    		int fragSize = Math.min(50, text.length());
    		int fragCount = text.length() / fragSize;
    		
    		try {
				TextFragment[] fragsArray = getFragmentsWithHighlitedText(query, "content", text, fragSize, fragCount, false);
				Stream.of(fragsArray).filter(frag -> frag != null && frag.getScore() > 0).forEachOrdered(fragsList::add);
				fragsList = getRelevantFragments(fragsList, queryString);
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
