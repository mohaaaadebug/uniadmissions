import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class DataQueries {
    private Connection conn;
    private DefaultTableModel tableModel;

    public DataQueries(DefaultTableModel tableModel) {
        this.conn = DatabaseConnection.getConnection();
        this.tableModel = tableModel;
    }

    public void loadTable(String query) {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);

            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(meta.getColumnLabel(i));
            }

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Query Error: " + e.getMessage());
        }
    }
}

