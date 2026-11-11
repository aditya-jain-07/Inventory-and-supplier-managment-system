import java.sql.Connection;
import java.sql.SQLException;

/**
 * Central MySQL JDBC connection settings.
 * Delegates to DBConnection for centralized configuration and security.
 */
public final class Database {

    private Database() { }

    /**
     * Obtains a connection via the centralized DBConnection utility.
     */
    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }
}
