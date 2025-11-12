import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static Connection conn;

    public static Connection getConnection() {
        if (conn == null) {
            try {
                conn = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/datafiles",
                        "root", ""
                );
                System.out.println("✅ Connected to Database!");
            } catch (SQLException e) {
                System.out.println("❌ Database Connection Failed: " + e.getMessage());
            }
        }
        return conn;
    }
}

