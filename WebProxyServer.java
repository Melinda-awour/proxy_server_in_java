
import java.io.*;
import java.net.*;
import java.util.*;

public class WebProxyServer {

    // Port where our proxy server will listen
    private static final int PORT = 8080;

    // Cache for storing web pages and images
    // The URL is the key and the response is the value
    private static Map<String, byte[]> cache = new HashMap<>();

    public static void main(String[] args) {

        try {
            // Create the proxy server
            ServerSocket serverSocket = new ServerSocket(PORT);

            System.out.println("Proxy Server Started...");
            System.out.println("Listening on port " + PORT);

            // Keep the server running continuously
            while (true) {

                // Accept a client connection
                Socket clientSocket = serverSocket.accept();

                System.out.println("\nClient connected!");

                // Handle the client request
                handleClient(clientSocket);
            }

        } catch (IOException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }

    // Method for handling client requests
    private static void handleClient(Socket clientSocket) {

        try {

            // Read the request sent by the client
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));

            // Read the first line of the HTTP request
            String request = reader.readLine();

            if (request == null) {
                clientSocket.close();
                return;
            }

            System.out.println("Request: " + request);

            // Check whether the request is a GET request
            if (!request.startsWith("GET ")) {

                String response =
                        "HTTP/1.1 405 Method Not Allowed\r\n" +
                        "Connection: close\r\n" +
                        "Content-Length: 0\r\n\r\n";

                clientSocket.getOutputStream().write(
                        response.getBytes("UTF-8"));

                clientSocket.close();
                return;
            }

            // Extract the URL from the request
            String urlString = request.split(" ")[1];

            // Check whether the requested object is cached
            if (cache.containsKey(urlString)) {

                System.out.println("CACHE HIT!");

                // Get the object from the cache
                byte[] cachedData = cache.get(urlString);

                // Send cached data to the client
                OutputStream output =
                        clientSocket.getOutputStream();

                output.write(cachedData);
                output.flush();

            } else {

                System.out.println("CACHE MISS!");

                // Convert the requested address into a URL
                URL url = new URL(urlString);

                // Connect to the actual web server
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                // Get the response from the web server
                int status = connection.getResponseCode();

                InputStream webInput;

                if (status >= 400) {
                    webInput = connection.getErrorStream();
                } else {
                    webInput = connection.getInputStream();
                }

                // Read the complete HTTP response
                ByteArrayOutputStream response =
                        new ByteArrayOutputStream();

                // Create the HTTP response header
                String headers =
                        "HTTP/1.1 " + status + " " +
                        connection.getResponseMessage() + "\r\n" +
                        "Content-Type: " +
                        connection.getContentType() + "\r\n" +
                        "Connection: close\r\n\r\n";

                response.write(headers.getBytes("UTF-8"));

                // Read the actual web content
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = webInput.read(buffer)) != -1) {
                    response.write(buffer, 0, bytesRead);
                }

                webInput.close();

                // Convert the response into a byte array
                byte[] responseData = response.toByteArray();

                // Store the response in the cache
                cache.put(urlString, responseData);

                System.out.println("Object saved in cache!");

                // Send the response to the client
                OutputStream output =
                        clientSocket.getOutputStream();

                output.write(responseData);
                output.flush();

                connection.disconnect();
            }

            // Close the client connection
            clientSocket.close();

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());

            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
