import java.sql.*;
import java.util.*;
import java.util.Date;
import java.time.*;
import java.time.LocalDateTime;

public class InventoryDB {

    //public static HashMap<Integer, String> statusMap = new HashMap<>();
    public static final String[] statusList = {"Standard", "Sold", "Bargain Bin", "Return to Customer", "Donate" };


    //Database connection string
    private static final String DB_CONNECTION_URL = "jdbc:sqlite:databases/RIMDB.sqlite";

    //Strings to reference settings table
    private static final String SETTINGS_TABLE_NAME = "Settings";
    private static final String SETTINGS_COL_NAME = "Name";
    private static final String SETTINGS_COL_BARGAIN_PRICE = "BargainPrice"; //Price when record is in Bargain Bin
    private static final String SETTINGS_COL_BARGAIN_DAYS = "BargainDays"; //Amount of days before record is set to Bargain Bin
    private static final String SETTINGS_COL_CONSIGNOR_PERCENT = "ConsginorPercent"; //Percentage of sold price that consignor gets
    private static final String SETTINGS_STORE_DEFAULT = "\'Second Hand Spins\'";

    //Strings to reference consignor table
    private static final String CONSIGNOR_TABLE_NAME = "Consignor";
    private static final String CONSIGNOR_COL_NAME = "Name";
    private static final String CONSIGNOR_COL_CONTACT = "Contact";
    private static final String CONSIGNOR_COL_ID = "ID";


    //Strings to reference record table
    private static final String RECORD_TABLE_NAME = "Record";
    private static final String RECORD_COL_TITLE = "Title";
    private static final String RECORD_COL_ARTIST = "Artist";
    private static final String RECORD_COL_PRICE = "Price";
    private static final String RECORD_COL_CREATED = "Created";
    private static final String RECORD_COL_UPDATED = "Updated";
    private static final String RECORD_COL_CONSIGNOR = "ConsignorID";
    private static final String RECORD_COL_STATUS = "Status";
    private static final String RECORD_COL_ID = "ID";

    //static final String OK = "Ok";

    //private static final String defaultStatus = "Standard";

    //SQL statements
    //Table creation sql
    private static final String CREATE_SETTINGS_TABLE = "CREATE TABLE IF NOT EXISTS %s ( %s TEXT default %s not null constraint Store_pk primary key," +
            "%s DECIMAL default 1.00 not null, %s INTEGER default 30 not null, %s DECIMAL default 0.40 not null)";
    private static final String CREATE_CONSIGNOR_TABLE = "CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY, %s TEXT, %s TEXT)";
    private static final String CREATE_RECORDS_TABLE = "CREATE TABLE IF NOT EXISTS %s (%s INTEGER not null constraint Record_pk primary key autoincrement, " +
            "%s      TEXT  default 'STANDARD' not null, %s DATETIME default CURRENT_TIMESTAMP not null, %s DATETIME default CURRENT_TIMESTAMP not null, " +
            "%s       REAL  default 10.00 not null, %s TEXT  not null, %s  TEXT  not null, %s INTEGER  default 0 not null references %s)";


    //Search Queries

    private static final String GET_ALL_RECORDS = "SELECT * FROM Record JOIN Consignor WHERE ConsignorID = CONSIGNOR.ID;";
    private static final String GET_ALL_CONSIGNORS = "SELECT * FROM Consignor;";

    private static final String GET_ALL_RECORDS_RECONCILE = "SELECT * FROM Record";
    private static final String GET_SETTINGS = "SELECT * FROM Settings";

    private static final String SEARCH_ALL_RECORDS = "SELECT * FROM Record INNER JOIN Consignor ON Record.ConsignorID = Consignor.ID WHERE ";



    //Database Change Queries
    private static final String DELETE_RECORD = "DELETE FROM Record WHERE ID = ?";
    private static final String ADD_RECORD = "INSERT INTO Record (Price, Artist, Title, ConsignorID) VALUES (?, ?,?,?) ";
    private static final String UPDATE_STATUS = "UPDATE Record SET Status = ?, Updated = CURRENT_DATE WHERE ID = ?";
    private static final String UPDATE_PRICE = "UPDATE Record SET Price = ? WHERE ID = ?";





    //private static final String concatSearchString = SEARCH_ALL_RECORDS + RECORD_COL_TITLE + " LIKE \'%" + "?"+ "%\' OR " + RECORD_COL_ARTIST+ " LIKE \'%?%\' OR "+ CONSIGNOR_COL_NAME + " LIKE \'%?%\' OR "+ RECORD_COL_STATUS+ " LIKE \'% ? %\'";
    private static final String concatSearchString = SEARCH_ALL_RECORDS + RECORD_COL_TITLE + " LIKE ? OR " + RECORD_COL_ARTIST+ " LIKE ? OR "+ CONSIGNOR_COL_NAME + " LIKE ? OR "+ RECORD_COL_STATUS+ " LIKE ? ESCAPE '!'";

    InventoryDB() {createTables(); reconcileDatesAndStatus(); getRecords();}

    private void createTables(){

        try (Connection conn = DriverManager.getConnection(DB_CONNECTION_URL);
            Statement statement = conn.createStatement()) {

            //Create table for consignors
            String createConsignorTableSQLTemplate = CREATE_CONSIGNOR_TABLE;
            String createConsignorTableSQL = String.format(createConsignorTableSQLTemplate,CONSIGNOR_TABLE_NAME,CONSIGNOR_COL_ID,CONSIGNOR_COL_NAME,CONSIGNOR_COL_CONTACT);

            statement.executeUpdate(createConsignorTableSQL);

            String createSettingsTableSQLTemplate = CREATE_SETTINGS_TABLE;
            String createSettingsTableSQL = String.format(createSettingsTableSQLTemplate, SETTINGS_TABLE_NAME,SETTINGS_COL_NAME,SETTINGS_STORE_DEFAULT, SETTINGS_COL_BARGAIN_PRICE, SETTINGS_COL_BARGAIN_DAYS,SETTINGS_COL_CONSIGNOR_PERCENT);

            statement.executeUpdate(createSettingsTableSQL);

            //create table for records
            String createRecordTableSQLTemplate = CREATE_RECORDS_TABLE;
            String createRecordTableSQL = String.format(createRecordTableSQLTemplate,RECORD_TABLE_NAME,RECORD_COL_ID,RECORD_COL_STATUS,RECORD_COL_CREATED,RECORD_COL_UPDATED, RECORD_COL_PRICE, RECORD_COL_ARTIST, RECORD_COL_TITLE,RECORD_COL_CONSIGNOR, CONSIGNOR_TABLE_NAME);

            statement.executeUpdate(createRecordTableSQL);

        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }

    }

    Vector getColumnNames() {

        Vector columnNames = new Vector();

        columnNames.add(RECORD_COL_TITLE);
        columnNames.add(RECORD_COL_ARTIST);
        columnNames.add(RECORD_COL_PRICE);
        columnNames.add(RECORD_COL_STATUS);
        columnNames.add(CONSIGNOR_COL_NAME);
        columnNames.add(CONSIGNOR_COL_CONTACT);
        columnNames.add(RECORD_COL_ID);
        columnNames.add(RECORD_COL_CONSIGNOR);

//        columnNames.add("Title");
//        columnNames.add("Artist");
//        columnNames.add("Price");
//        columnNames.add("Status");
//        columnNames.add("Consignor");
//        columnNames.add("Contact Information");


        return columnNames;
    }

    Vector<Vector> getRecords(){
        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery(GET_ALL_RECORDS);

            Vector<Vector> recordsVector = new Vector<>();

            String artist, title, name, contact,priceString,status, statusString;
            Date created, updated; //date
            double price;
            int recordID, consignorID; //int

            while (rs.next()) {


                artist = rs.getString(RECORD_COL_ARTIST);
                title = rs.getString(RECORD_COL_TITLE);

                //Get price as integer and convert it to a string
                price = rs.getDouble(RECORD_COL_PRICE);
                priceString = Record.parsePrice(price);

                //Get status of record
                status = rs.getString(RECORD_COL_STATUS);
                //statusString = getStatus(status);

                name = rs.getString(CONSIGNOR_COL_NAME);
                contact = rs.getString(CONSIGNOR_COL_CONTACT);

                recordID = rs.getInt(RECORD_COL_ID);
                consignorID = rs.getInt(RECORD_COL_CONSIGNOR);




//                price = rs.getString(Record.parsePrice(RECORD_COL_PRICE));
//                status = rs.getString(Record.getStatus(RECORD_COL_STATUS));

                Vector v = new Vector();
                v.add(title); v.add(artist); v.add(priceString); v.add(status); v.add(name); v.add(contact); v.add(recordID); v.add(consignorID);

                recordsVector.add(v);
            }

            return recordsVector;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Vector<Vector> searchRecords(String searchString){
        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             PreparedStatement preparedSearch = connection.prepareStatement(concatSearchString)) {

            String newSearchString = "%"+searchString+"%";

            preparedSearch.setString(1, newSearchString);
            preparedSearch.setString(2, newSearchString);
            preparedSearch.setString(3, newSearchString);
            preparedSearch.setString(4, newSearchString);

            ResultSet rs = preparedSearch.executeQuery();

            Vector<Vector> recordsVector = new Vector<>();

            String artist, title, name, contact,priceString, status;
            //Date created, updated; //date
            double price;
            int recordID, consignorID; //int

            while (rs.next()) {


                artist = rs.getString(RECORD_COL_ARTIST);
                title = rs.getString(RECORD_COL_TITLE);

                //Get price as integer and convert it to a string
                price = rs.getDouble(RECORD_COL_PRICE);
                priceString = Record.parsePrice(price);

                //Get status as integer and convert it to a string
                status = rs.getString(RECORD_COL_STATUS);
                //statusString = getStatus(status);

                name = rs.getString(CONSIGNOR_COL_NAME);
                contact = rs.getString(CONSIGNOR_COL_CONTACT);

                recordID = rs.getInt(RECORD_COL_ID);
                consignorID = rs.getInt(RECORD_COL_CONSIGNOR);


//                price = rs.getString(Record.parsePrice(RECORD_COL_PRICE));
//                status = rs.getString(Record.getStatus(RECORD_COL_STATUS));

                Vector v = new Vector();
                v.add(title); v.add(artist); v.add(priceString); v.add(status); v.add(name); v.add(contact); v.add(recordID); v.add(consignorID);;

                recordsVector.add(v);
            }

            return recordsVector;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    //Deletes record from database
    public void deleteRecord(Integer recordID) {

        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_RECORD)) {

            preparedStatement.setInt(1, recordID);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //Update record's sales status
    public void updateStatus(String status, Integer recordID) {

        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_STATUS)) {

            preparedStatement.setString(1,status);
            preparedStatement.setInt(2, recordID);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //Update record's price in database
    public void updatePrice(Double price, Integer recordID) {

        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_PRICE)) {

            preparedStatement.setDouble(1,price);
            preparedStatement.setInt(2, recordID);
            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    //Creates LinkedHashMap of all Consignors to keep as a record, and to propagate the NewRecordGUI combobox
    LinkedHashMap getConsignors(){
        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             Statement statement = connection.createStatement()) {

            ResultSet rs = statement.executeQuery(GET_ALL_CONSIGNORS);

            int consignorID;
            String name, contact;

            LinkedHashMap consignorsMap = new LinkedHashMap();

            while(rs.next()) {
                consignorID = rs.getInt(CONSIGNOR_COL_ID);
                name = rs.getString(CONSIGNOR_COL_NAME);
                //contact = rs.getString(CONSIGNOR_COL_CONTACT); //not used currently

                consignorsMap.put(name,consignorID);


            }


            return consignorsMap;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        }


    //Input new record into database
    public void addToRecordDB(Double price, String artist, String title, int consignorID) {

        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(ADD_RECORD)) {


            preparedStatement.setDouble(1, price);
            preparedStatement.setString(2, artist);
            preparedStatement.setString(3, title);
            preparedStatement.setInt(4, consignorID);

            preparedStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void reconcileDatesAndStatus(){
        //TODO This function analyzes existing data in records table of database and uses store settings to automatically change status of each record based on its created and updated by date.
        Double bargainPrice = 1.00, consignorPercent;
        Integer bargainDays = 30, betweenDates,recordID;
        String status;
        //LocalDate updated, current;
        //current = LocalDate.now();

        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             Statement statement = connection.createStatement()) {
            ResultSet settings = statement.executeQuery(GET_SETTINGS);

            while (settings.next()){
                bargainPrice = settings.getDouble(SETTINGS_COL_BARGAIN_PRICE);
                consignorPercent = settings.getDouble(SETTINGS_COL_CONSIGNOR_PERCENT);
                bargainDays = settings.getInt(SETTINGS_COL_BARGAIN_DAYS);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Connection connection = DriverManager.getConnection(DB_CONNECTION_URL);
             Statement statement = connection.createStatement()) {
             ResultSet recordSet = statement.executeQuery(GET_ALL_RECORDS_RECONCILE);

            while (recordSet.next()) {
                //updated = LocalDate.parse(recordSet.getString(RECORD_COL_UPDATED).toLocalDateTime().toLocalDate());
                status = recordSet.getString(RECORD_COL_STATUS);
                recordID = recordSet.getInt(RECORD_COL_ID);
                //betweenDates = (Period.between(updated,current).getDays());
                /*
                if ( (betweenDates > bargainDays) && (status == statusList[0])){
                    updateStatus(statusList[2], recordID);
                    updatePrice(bargainPrice,recordID);
                } else if ((betweenDates > bargainDays) && (status == statusList[2])) {
                    updateStatus(statusList[4], recordID);
                }*/

            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }





}


