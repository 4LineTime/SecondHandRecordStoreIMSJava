
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

//refers to https://github.com/claraj/Java2545Examples/

public class MainGUI extends JFrame{

    private JTextField searchTextField;
    private JButton searchButton;
    private JTable recordTable;
    private JButton newRecordButton;
    private JButton newCustomerButton;
    private JComboBox statusComboBox;
    private JButton exitButton;
    private JButton settingsButton;
    private JPanel mainPanel;
    private InventoryDB db;
    private String windowTitle = "Used Record Inventory Management";

    MainGUI(InventoryDB db) {
        this.db = db;

        this.setContentPane(mainPanel);
        pack();
        setTitle(windowTitle);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
        setLocationRelativeTo(null);

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        settingsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //
                //
                Settings changeSettings = new Settings(MainGUI.this);

            }
        });

        newCustomerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewConsignorGUI newConsignorInput = new NewConsignorGUI(MainGUI.this);
            }
        });

        newRecordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewRecordGUI newRecordInput = new NewRecordGUI(MainGUI.this);

            }
        });
    }

    private void configureTable() {
        //Vector columnNames = db.getColumnNames();
        //Vector data = db.getRecords();

        //DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);
        //recordTable.setModel(tableModel);
    }
}
