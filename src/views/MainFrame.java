package views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.stream.Stream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.pdfbox.debugger.ui.ExtensionFileFilter;

import core.DocumentSearcher;
import core.FileIndexer;

public class MainFrame extends JFrame {
	
	private FileIndexer indexer;
	private DocumentSearcher searcher;
	
	private static final long serialVersionUID = 110418166857720605L;
	private JPanel contentPane;
	private JMenuBar menuBar;
	private JMenuItem importMenuItem;
	private JMenu filesMenu;
	private JSplitPane splitPane;
	private JPanel rightMainPanel;
	private JPanel leftMainPanel;
	private JTextField searchTextField;
	private JLabel searchLabel;
	private JPanel searchPanel;
	private JScrollPane indexedScrollPane;
	private JPanel indexedPanel;
	private JLabel indexedDocumentsLabel;
	
	private JFileChooser importFileChooser;
	private JTree indexedTree;
	private JLabel resultsLabel;
	private JScrollPane resultDocumentPane;
	private JTree resultsTree;
	
	private class ImportAction implements ActionListener {
		private MainFrame _frame;
		public ImportAction(MainFrame frame) {
			if (frame == null) {
				throw new NullPointerException("Main frame needs to be passed");
			}
			this._frame = frame;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			this._frame.importFiles();
		}
	}
	
	private class MainFrameWindowAdapter extends WindowAdapter {
		private MainFrame _frame;
		public MainFrameWindowAdapter(MainFrame frame) {
			if (frame == null) {
				throw new NullPointerException("Main frame needs to be passed");
			}
			this._frame = frame;
		}
		
		public void windowClosing(WindowEvent evt) {	
			try {
				this._frame.indexer.close();
			} catch (IOException ex) {
				System.out.println("Unable to close the indexer: " + ex.getMessage());
			}
		}
	}
	
	private class SearchKeyAdapter extends KeyAdapter {
		private MainFrame _frame;
		public SearchKeyAdapter(MainFrame frame) {
			if (frame == null) {
				throw new NullPointerException("Main frame needs to be passed");
			}
			this._frame = frame;
		}
		@Override
		public void keyPressed(KeyEvent e) {
			// Allow only enter to trigger the search.
			if (e.getKeyCode() != KeyEvent.VK_ENTER) {
				return;
			}
 			try {
 				this._frame.clearTree(resultsTree);
 				String text = this._frame.searchTextField.getText();
				Document[] docs = this._frame.searcher.search(text);
				for (int i = 0; i < docs.length; i++) {
					this._frame.addDocToResultsTree(docs[i]);
			    }
			} catch (IOException e1) {
				System.out.println("Unable to update the indexer's reader: " + e1.getMessage());
			} catch (ParseException e2) {
				System.out.println("Unable to parse the query: " + e2.getMessage());
			}
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame frame = new MainFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void importFiles() {
		if (importFileChooser.showOpenDialog(importMenuItem) == JFileChooser.APPROVE_OPTION) {
			File[] selectedFiles = importFileChooser.getSelectedFiles();
			try {
				for (File selectedFile: selectedFiles) {
					Document doc = this.indexer.addFileToIndex(selectedFile);
					this.addDocToIndexedTree(doc);
				}
			} catch (IOException ex) {
				System.out.println("Unable to index the files: " + ex.getMessage());
			}
		}
	}
	
	private void clearTree(JTree tree) {
		DefaultTreeModel model = (DefaultTreeModel)this.resultsTree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
		while(!model.isLeaf(root))
    	{
    		model.removeNodeFromParent((MutableTreeNode)model.getChild(root,0));
    	}
	}
	
	private void addDocToIndexedTree(Document doc) {
		JTree tree = this.indexedTree;
		DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
		String title = doc.get("filename") + " (" + doc.get("fullpath") + ")";
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(title);
		root.add(newNode);
		model.reload(root);
	}
	
	private void addDocToResultsTree(Document doc) {
		JTree tree = this.resultsTree;
		DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
		String templateHtml = "<html><div style=\"color: #1a0dab\"><b>%1$s</b></div><div style=\"color: #006621\"><i>%2$s</i></div><br/></html>";
		String title = String.format(templateHtml, doc.get("filename"), doc.get("fullpath"));
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(title);
		root.add(newNode);
		model.reload(root);
	}
	
	private static void setUIFont (FontUIResource f) {
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get (key);
			if (value != null && value instanceof FontUIResource) {
				UIManager.put (key, f);
			}
		}
    }

	private String makeFileFilterTitle(String prefix, String[] extensions) {
		return prefix + " (" + Stream.of(extensions)
					  .map(value -> String.join("", "*.", value))
					  .reduce((t, u) -> t + ", " + u).get() + ")";
	}

	/**
	 * Create the frame.
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public MainFrame() throws IOException, ParseException {
		setUIFont(new FontUIResource("Dialog", Font.PLAIN, 14));
		
		setTitle("InformationRetrieval");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 640, 480);
		
		this.addWindowListener(new MainFrameWindowAdapter(this));
		
		indexer = new FileIndexer();
		searcher = new DocumentSearcher(indexer);
		
		importFileChooser = new JFileChooser();
		String[] extensions = new String[]{ "txt", "doc", "docx", "pdf" };
		FileFilter filter = new ExtensionFileFilter(extensions, makeFileFilterTitle("Documents", extensions));
		importFileChooser.setFileFilter(filter);
		importFileChooser.setDialogTitle("Add Documents");
		importFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		importFileChooser.setMultiSelectionEnabled(true);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		filesMenu = new JMenu("Files");
		menuBar.add(filesMenu);
		
		importMenuItem = new JMenuItem("Import");
		importMenuItem.addActionListener(new ImportAction(this));
		importMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK));
		filesMenu.add(importMenuItem);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		rightMainPanel = new JPanel();
		rightMainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		splitPane.setRightComponent(rightMainPanel);
		rightMainPanel.setLayout(new BorderLayout(0, 0));
		
		resultsLabel = new JLabel("Results");
		resultsLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
		rightMainPanel.add(resultsLabel, BorderLayout.NORTH);
		
		resultDocumentPane = new JScrollPane();
		rightMainPanel.add(resultDocumentPane, BorderLayout.CENTER);
		resultDocumentPane.setPreferredSize(new Dimension(220, 3));
		
		resultsTree = new JTree();
		resultsTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Results")));
		resultsTree.setBorder(new EmptyBorder(5, 5, 5, 5));
		resultsTree.setRootVisible(false);
		resultDocumentPane.setViewportView(resultsTree);
		DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) resultsTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        renderer.setBackgroundSelectionColor(null);
        renderer.setBorderSelectionColor(null);
        
		leftMainPanel = new JPanel();
		splitPane.setLeftComponent(leftMainPanel);
		leftMainPanel.setLayout(new BorderLayout(0, 0));
		
		searchPanel = new JPanel();
		searchPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		leftMainPanel.add(searchPanel, BorderLayout.NORTH);
		searchPanel.setLayout(new BorderLayout(0, 0));
		
		searchTextField = new JTextField();
		searchTextField.addKeyListener(new SearchKeyAdapter(this));
		searchPanel.add(searchTextField, BorderLayout.CENTER);
		searchTextField.setColumns(10);
		
		searchLabel = new JLabel("Search in documents");
		searchLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
		searchPanel.add(searchLabel, BorderLayout.NORTH);
		
		indexedPanel = new JPanel();
		indexedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		leftMainPanel.add(indexedPanel, BorderLayout.CENTER);
		indexedPanel.setLayout(new BorderLayout(0, 0));
		
		indexedScrollPane = new JScrollPane();
		indexedScrollPane.setViewportBorder(null);
		indexedPanel.add(indexedScrollPane, BorderLayout.CENTER);
		
		indexedTree = new JTree(new DefaultMutableTreeNode("Imported files"));
		indexedTree.setBorder(new EmptyBorder(5, 5, 5, 5));
		indexedTree.setRootVisible(false);
		
		indexedScrollPane.setViewportView(indexedTree);
		
		Document[] indexedDocuments = indexer.getIndexedDocuments();
		for (Document doc: indexedDocuments) {
			this.addDocToIndexedTree(doc);
		}

		indexedDocumentsLabel = new JLabel("Indexed documents");
		indexedDocumentsLabel.setBorder(new EmptyBorder(0, 0, 5, 5));
		indexedPanel.add(indexedDocumentsLabel, BorderLayout.NORTH);
	}
}
