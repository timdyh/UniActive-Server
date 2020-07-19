import com.example.util.JSONOperator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;

public class Main {

    private final static String driver = "com.mysql.cj.jdbc.Driver";
    private final static String url = "jdbc:mysql://127.0.0.1:3306/buaa_activity?serverTimezone=UTC";
    private final static String user = "root";
    private final static String db_password = "root996258";

    private static class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) {
            Connection conn;
            try {
                conn = DriverManager.getConnection(
                        url, user, db_password);
            } catch (SQLException e) {
                e.printStackTrace();
                httpExchange.close();
                return;
            }
            try {
                InputStream is = httpExchange.getRequestBody();
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                int size;
                byte[] buffer = new byte[1024];
                while ((size = is.read(buffer)) != -1) {
                    bs.write(buffer, 0, size);
                }
                String request = bs.toString("UTF-8");
                JSONObject in = new JSONObject(request);
                is.close();
                JSONOperator operator = new JSONOperator(in, conn);
                String result = operator.execute();
                OutputStream os = httpExchange.getResponseBody();
                byte[] rb = result.getBytes(StandardCharsets.UTF_8);
                httpExchange.sendResponseHeaders(200, rb.length);
                os.write(rb);
                os.close();
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    conn.close();
                    httpExchange.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Class.forName(driver);
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(12345), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", new MyHttpHandler());
            server.start();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
