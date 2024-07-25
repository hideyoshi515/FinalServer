import server.ChatServer;
import server.DBConnection;
import server.DBServer;

public class Start {
    public static void main(String[] args) throws Exception {
        DBConnection.init();
        ChatServer chatServer = new ChatServer();
        chatServer.ChatServer();
        DBServer dbServer = new DBServer();
        dbServer.DBServer();
    }
}
