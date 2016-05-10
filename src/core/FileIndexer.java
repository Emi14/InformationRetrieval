package core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import core.RomanianStopWords;

public class FileIndexer implements AutoCloseable {
	
	private Analyzer _analyzer = null;
	private Directory _indexDirectory = null;
	private IndexWriter _indexWriter = null;
	private IndexReader _indexReader = null;
	private boolean _needsCommit = false;
	private static Collection<Document> _indexedDocuments;
	 
	private static final String IndexerPath = "E:\\Cucu";
	
	public FileIndexer() throws IOException {
		this.createIndexWriter();
		this.readIndexedDocuments();
	}
	
	private void createIndexWriter() throws IOException {
		File file = new File(IndexerPath);
		if (!file.exists()) {
			file.mkdir();
		}
		
		// Use for now a RAM Directory
		_indexDirectory = new RAMDirectory();
		//_indexDirectory = FSDirectory.open(file.toPath());//new RAMDirectory();
		_analyzer = new RomanianAnalyzerWithoutDiacritics(RomanianStopWords.getStopList());
		IndexWriterConfig writerConfig = new IndexWriterConfig(_analyzer);
		_indexWriter = new IndexWriter(_indexDirectory, writerConfig);
		_indexWriter.commit();
		_indexReader = DirectoryReader.open(_indexDirectory);
	}
	
	private void readIndexedDocuments() throws IOException {
		this.updateReader();
		_indexedDocuments = new ArrayList<Document>();
		Query query = new MatchAllDocsQuery();
		IndexSearcher searcher = new IndexSearcher(_indexReader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(10);
	    searcher.search(query, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    for (int i = 0; i < hits.length; i++) {
	    	int documentId = hits[i].doc;
	    	_indexedDocuments.add(searcher.doc(documentId));
	    }
	}
	
	public Document addFileToIndex(File file) throws CorruptIndexException, IOException {
		String fileAbsolutePath = file.getAbsolutePath();
		try {
			Query query = new QueryParser("fullpath", _analyzer).parse(fileAbsolutePath);
			_indexWriter.deleteDocuments(query);
		}
		catch (ParseException err) {
			System.out.println("Error when deleting existing documents: " + err.getMessage());
		}
		
		Document doc = new Document();
		doc.add(new TextField("content", new FileReader(file)));
		doc.add(new StringField("filename", file.getName(), Field.Store.YES));
		doc.add(new StringField("fullpath", fileAbsolutePath,  Field.Store.YES));
		
		_indexWriter.addDocument(doc);
		_indexedDocuments.add(doc);
		_needsCommit = true;
		
		return doc;
	}
	
	public void updateReader() throws IOException {
		// Commit the changed in the indexWriter, 
		// and then recreate a DirectoryReader.
		if (!_needsCommit) {
			return;
		}
		_indexReader.close();
		_indexWriter.commit();
		_indexReader = DirectoryReader.open(_indexDirectory);
	}
	
	public Document[] getIndexedDocuments() {
		return _indexedDocuments.toArray(new Document[_indexedDocuments.size()]);
	}
	
	public Analyzer getAnalyzer() {
		return _analyzer;
	}
	
	public IndexReader getIndexReader() {
		return _indexReader;
	}
	
	@Override
	public void close() throws IOException {
		_indexWriter.close();
		_indexReader.close();
		_indexDirectory.close();
		_analyzer.close();
    }
}
