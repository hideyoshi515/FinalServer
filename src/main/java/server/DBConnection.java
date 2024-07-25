package server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DBConnection {
    private static HikariDataSource dataSource;
    private static final int MySQLMINCONNECTION = 5;
    private static int MySQLMAXCONNECTION = 10;
    private static final String host = "nas.necohost.co.kr";
    private static final String db_id = "final";
    private static final String db_pw = "Soldesk@802";
    private static final String db_schema = "final";
    public static final String MySQLURL = "jdbc:mysql://"+host+":3306/" + db_schema + "?autoReconnect=true&characterEncoding=utf8&maxReconnects=5";

    private static String databaseName;
    private static int databaseMajorVersion;
    private static int databaseMinorVersion;
    private static String databaseProductVersion;

    public synchronized static void init() {
        if (dataSource != null) {
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        if (MySQLMINCONNECTION > MySQLMAXCONNECTION) {
            MySQLMAXCONNECTION = MySQLMINCONNECTION;
        }

        try {
            // HikariCP 설정
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(MySQLURL);
            config.setUsername(db_id);
            config.setPassword(db_pw);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(MySQLMAXCONNECTION);
            config.setMinimumIdle(MySQLMINCONNECTION);
            config.setIdleTimeout(30000);  // 유휴 커넥션의 최대 유지 시간 (밀리초)
            config.setConnectionTimeout(5000); // 커넥션을 얻기 위한 최대 대기 시간 (밀리초)
            config.setMaxLifetime(600000); // 커넥션의 최대 수명 (밀리초)
            config.setLeakDetectionThreshold(2000); // 누수 감지 시간 (밀리초)
            config.setConnectionTestQuery("SELECT 1"); // 커넥션 테스트 쿼리

            dataSource = new HikariDataSource(config);

            // 데이터베이스 메타데이터 가져오기
            try (Connection c = getConnection()) {
                DatabaseMetaData dmd = c.getMetaData();
                databaseName = dmd.getDatabaseProductName();
                databaseMajorVersion = dmd.getDatabaseMajorVersion();
                databaseMinorVersion = dmd.getDatabaseMinorVersion();
                databaseProductVersion = dmd.getDatabaseProductVersion();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static void closeObject(Connection con) {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static synchronized void shutdown() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSource = null;
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            init();
        }
        return dataSource.getConnection();
    }

    public static int getActiveConnections() {
        return dataSource != null ? dataSource.getHikariPoolMXBean().getActiveConnections() : 0;
    }

    public static int getIdleConnections() {
        return dataSource != null ? dataSource.getHikariPoolMXBean().getIdleConnections() : 0;
    }
}
