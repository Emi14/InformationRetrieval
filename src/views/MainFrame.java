package views;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.DefaultListModel;
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

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import core.FileIndexer;

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

	/**
	 * Create the frame.
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public MainFrame() throws IOException, ParseException {
		
		try {
			indexer = new FileIndexer();
			indexer.addFileToIndex(new File("C:\\Users\\Botezatu\\Desktop\\mihai.txt"));
			indexer.addFileToIndex(new File("C:\\Users\\Botezatu\\Desktop\\mihai2.txt"));
			indexer.updateReader();
			indexer.search();
			indexer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		setTitle("InformationRetrieval");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 819, 507);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		filesMenu = new JMenu("Files");
		menuBar.add(filesMenu);
		
		importMenuItem = new JMenuItem("Import");
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
		searchPanel.add(searchTextField, BorderLayout.CENTER);
		searchTextField.setColumns(10);
		
		searchLabel = new JLabel("Search Documents");
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
			System.out.println(doc.get("filename"));
			indexedListModel.addElement(doc.get("filename"));
		}
		indexedScrollPane.setViewportView(indexedList);

		indexedDocumentsLabel = new JLabel("Indexed Documents");
		indexedDocumentsLabel.setLabelFor(indexedList);
		indexedPanel.add(indexedDocumentsLabel, BorderLayout.NORTH);
	}
}
