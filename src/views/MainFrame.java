package views;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import core.FileIndexer;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MainFrame extends JFrame {
	
	private FileIndexer indexer;
	
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
	private JList<String> indexedList;
	private DefaultListModel<String> indexedListModel;
	private JPanel indexedPanel;
	private JLabel indexedDocumentsLabel;
	
	private JFileChooser importFileChooser;
	
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
 				String text = this._frame.searchTextField.getText();
				this._frame.indexer.updateReader();
				this._frame.indexer.search(text);
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
					this.addDocToIndexedList(doc);
				}
			} catch (IOException ex) {
				System.out.println("Unable to index the files: " + ex.getMessage());
			}
		}
	}
	
	private void addDocToIndexedList(Document doc) {
		indexedListModel.addElement(doc.get("filename") + " (" + doc.get("fullpath") + ")");
	}

	/**
	 * Create the frame.
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public MainFrame() throws IOException, ParseException {
		
		setTitle("InformationRetrieval");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 819, 507);
		
		this.addWindowListener(new MainFrameWindowAdapter(this));
		
		indexer = new FileIndexer();
		
		importFileChooser = new JFileChooser();
		FileFilter filter = new FileNameExtensionFilter("Text File","txt");
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
		splitPane.setResizeWeight(0.1);
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		rightMainPanel = new JPanel();
		splitPane.setRightComponent(rightMainPanel);
		
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
		searchPanel.add(searchLabel, BorderLayout.NORTH);
		
		indexedPanel = new JPanel();
		indexedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		leftMainPanel.add(indexedPanel, BorderLayout.CENTER);
		indexedPanel.setLayout(new BorderLayout(0, 0));
		
		indexedScrollPane = new JScrollPane();
		indexedScrollPane.setViewportBorder(null);
		indexedPanel.add(indexedScrollPane, BorderLayout.CENTER);
		
		indexedListModel = new DefaultListModel<String>();
		indexedList = new JList<String>(indexedListModel);
		Document[] indexedDocuments = indexer.getIndexedDocuments();
		for (Document doc: indexedDocuments) {
			this.addDocToIndexedList(doc);
		}
		indexedScrollPane.setViewportView(indexedList);

		indexedDocumentsLabel = new JLabel("Indexed documents");
		indexedDocumentsLabel.setLabelFor(indexedList);
		indexedPanel.add(indexedDocumentsLabel, BorderLayout.NORTH);
	}
}
