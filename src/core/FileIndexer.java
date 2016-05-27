package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

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
		// Reading the creationTime of the file.
		BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
		
		// Reading the contents of the file.
		ContentHandler handler = new BodyContentHandler();
        
        try(FileInputStream is = new FileInputStream(file)) {
        	Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, file.getCanonicalPath());
            Parser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();
            parser.parse(is, handler, metadata, context);
        }
        catch (SAXException | TikaException err) {
        	throw new IOException(err);
        }
        
        String fileAbsolutePath = file.getAbsolutePath();
		String fileContents = handler.toString();
        String fileName = file.getName();
        long fileCreatedDate = attr.creationTime().toMillis();
        long fileSize = attr.size();
        
        /* Abstract is considered to be the first 20 words from the document. */
        int maxTokens = 20;
        StringTokenizer fileContentsTokenizer = new StringTokenizer(fileContents);
        StringBuilder fileAbstractBuilder = new StringBuilder();
        for(int i = 0; i < maxTokens && fileContentsTokenizer.hasMoreTokens(); i++) {
        	fileAbstractBuilder.append(fileContentsTokenizer.nextToken());
        	fileAbstractBuilder.append(' ');
        }
        
		Document doc = new Document();
		doc.add(new TextField("content", fileContents, Field.Store.YES));
		doc.add(new TextField("abstract", fileAbstractBuilder.toString(), Field.Store.YES));
		doc.add(new StringField("filename", fileName, Field.Store.YES));
		doc.add(new StringField("fullpath", fileAbsolutePath,  Field.Store.YES));
		doc.add(new LongPoint("createdate", fileCreatedDate));
		doc.add(new LongPoint("sizeBytes", fileSize));
		
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
