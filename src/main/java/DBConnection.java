import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centralized MySQL JDBC Connection Utility.
 *
 * Uses the local MySQL Connector/J JAR (lib/mysql-connector-j-26.10.0.jar).
 *
 * Configuration resolution priority:
 * 1. Environment variables: DB_URL, DB_USER, DB_PASSWORD
 * 2. JVM System properties: -Ddb.url, -Ddb.user, -Ddb.password
 * 3. Local properties file: db.properties (in working directory or classpath)
 * 4. Fallback defaults (localhost:3306/dvms_db, root, MY PASSWORD)
 *
 * Passwords are never logged or exposed in toString/safe output.
 */
public final class DBConnection {

    public static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 3306;
    public static final String DEFAULT_DATABASE = "dvms_db";
    public static final String DEFAULT_URL =
            "jdbc:mysql://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/" + DEFAULT_DATABASE
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    public static final String DEFAULT_USER = "root";
    public static final String DEFAULT_PASSWORD = "MY PASSWORD";

    // Secondary fallback for local environment compatibility if primary credentials fail
    private static final String LOCAL_COMPAT_PASSWORD = "YOUR_FALLBACK_PASSWORD";

    private static final String CONFIG_FILE_NAME = "db.properties";

    static {
        try {
            Class.forName(DRIVER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: MySQL Connector/J driver class not found on classpath: " + e.getMessage());
        }
    }

    private DBConnection() {
        // Utility class
    }

    /**
     * Confirms whether the MySQL Connector/J Driver class is available on the classpath.
     */
    public static boolean isDriverAvailable() {
        try {
            Class.forName(DRIVER_CLASS_NAME);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Resolves the configured database URL.
     */
    public static String getUrl() {
        String env = System.getenv("DB_URL");
        if (env != null && !env.isBlank()) return env.trim();

        String sysProp = System.getProperty("db.url");
        if (sysProp != null && !sysProp.isBlank()) return sysProp.trim();

        Properties fileProps = loadPropertiesFile();
        String fileVal = fileProps.getProperty("db.url");
        if (fileVal != null && !fileVal.isBlank()) return fileVal.trim();

        return DEFAULT_URL;
    }

    /**
     * Resolves the configured database username.
     */
    public static String getUser() {
        String env = System.getenv("DB_USER");
        if (env != null && !env.isBlank()) return env.trim();

        String sysProp = System.getProperty("db.user");
        if (sysProp != null && !sysProp.isBlank()) return sysProp.trim();

        Properties fileProps = loadPropertiesFile();
        String fileVal = fileProps.getProperty("db.user");
        if (fileVal != null && !fileVal.isBlank()) return fileVal.trim();

        return DEFAULT_USER;
    }

    /**
     * Resolves the configured database password.
     */
    public static String getPassword() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null && !env.isBlank()) return env;

        String sysProp = System.getProperty("db.password");
        if (sysProp != null && !sysProp.isBlank()) return sysProp;

        Properties fileProps = loadPropertiesFile();
        String fileVal = fileProps.getProperty("db.password");
        if (fileVal != null && !fileVal.isBlank()) return fileVal;

        return DEFAULT_PASSWORD;
    }

    /**
     * Opens and returns a new MySQL JDBC connection using the resolved settings.
     *
     * @return active java.sql.Connection
     * @throws SQLException if a database access error occurs or credentials are invalid
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(getUrl(), getUser(), getPassword());
        } catch (SQLException ex) {
            // If primary password failed with access denied (1045 / 28000), test local compatibility fallback
            if ("28000".equals(ex.getSQLState()) || ex.getErrorCode() == 1045) {
                if (DEFAULT_PASSWORD.equals(getPassword())) {
                    try {
                        return DriverManager.getConnection(getUrl(), getUser(), LOCAL_COMPAT_PASSWORD);
                    } catch (SQLException ignored) {
                        // Fall through to throw original exception
                    }
                }
            }
            throw ex;
        }
    }

    /**
     * Opens and returns a MySQL JDBC connection with explicitly provided parameters.
     */
    public static Connection getConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Returns a safe description of the connection target (never reveals the password).
     */
    public static String getSafeConnectionInfo() {
        return "MySQL Target: " + getUrl() + " [User: " + getUser() + ", Password: ***]";
    }

    /**
     * Loads settings from a local db.properties file if present in the working
     * directory or classpath.
     */
    private static Properties loadPropertiesFile() {
        Properties props = new Properties();

        // 1. Try working directory
        File localFile = new File(CONFIG_FILE_NAME);
        if (localFile.exists() && localFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(localFile)) {
                props.load(fis);
                return props;
            } catch (Exception ignored) {
                // Ignore and fall back to classpath
            }
        }

        // 2. Try classpath
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {
            // Ignore if missing
        }

        return props;
    }
}
