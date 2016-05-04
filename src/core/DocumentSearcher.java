package core;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;

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
	    return docs;
	}
}
