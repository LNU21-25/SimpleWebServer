import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * A simple web server that can serve static files and handle login
 * functionality.
 */
public class SimpleWebServer {

  // Constants
  private static final int BUFFER_SIZE = 8192;
  private static final String PUBLIC_FOLDER = "public";

  // Map content types for different file extensions
  private static final Map<String, String> CONTENT_TYPES = Map.of(
      "html", "text/html",
      "htm", "text/html",
      "jpg", "image/jpeg",
      "jpeg", "image/jpeg",
      "png", "image/png");

  /**
   * Main method to start the web server.
   * 
   * @param args Command-line arguments: [port]
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.exit(1);
    }

    int port = Integer.parseInt(args[0]);
    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Listening for connection on port " + port);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        handleClientRequest(clientSocket);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Handles client requests.
   * 
   * @param clientSocket The client socket
   */
  public static void handleClientRequest(Socket clientSocket) {
    try (
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream outputStream = clientSocket.getOutputStream();) {
      String request = reader.readLine();

      String[] tokens = request.split(" ");
      String method = tokens[0];
      String path = tokens[1];
      String version = tokens[2];

      System.out.println("Received request: Method: " +
          method + ", Path: " + path + ", Version: " + version);

      if (method.equals("GET")) {
        if (path.equals("/login.html")) {
          serveLoginHtml(outputStream);
        } else if (path.equals("/redirect")) {
          sendRedirectResponse(outputStream, 302, "Found", "Redirecting to /a/b/index.html");
          serveFile(outputStream, "/a/b/index.html");
        } else {
          serveFile(outputStream, path);
        }
      } else if (method.equals("POST") && path.equals("/login")) {
        handleLogin(outputStream, reader);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Serves a file requested by the client.
   * 
   * @param outputStream The output stream to the client
   * @param filePath     The path of the requested file
   */
  public static void serveFile(OutputStream outputStream, String filePath) {
    try {
      File file = new File(PUBLIC_FOLDER + filePath);

      if (file.exists() && !file.isDirectory()) {
        String extension = getFileExtension(file.getName());
        String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");

        System.out.print("Serving file: " + filePath);
        System.out.print(", Client: " + InetAddress.getLocalHost().getHostAddress());
        System.out.println(", Server: " + InetAddress.getLocalHost().getHostName());
        System.out.print("Response: HTTP/1.1 200 OK");
        System.out.print(", Date: " + new Date());
        System.out.println(", Server Name: SimpleWebServer");
        System.out.print("Content-Length: " + file.length());
        System.out.print(", Connection: close");
        System.out.println(", Content-Type: " + contentType + "\n");

        PrintWriter writer = new PrintWriter(outputStream);
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: " + contentType);
        writer.println("Content-Length: " + file.length());
        writer.println();
        writer.flush();

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
        fileInputStream.close();
      } else {
        sendErrorResponse(outputStream, 404, "Not Found", "The requested file was not found.");
      }
    } catch (IOException e) {
      e.printStackTrace();
      sendErrorResponse(outputStream, 500, "Internal Server Error",
          "The server encountered an unexpected condition.");
    }
  }

  /**
   * Serves the login HTML page.
   * 
   * @param outputStream The output stream to the client
   */
  public static void serveLoginHtml(OutputStream outputStream) {
    try {
      File file = new File(PUBLIC_FOLDER + "/login.html");

      if (file.exists() && !file.isDirectory()) {
        PrintWriter writer = new PrintWriter(outputStream);
        writer.println("HTTP/1.1 200 OK");
        writer.println("Content-Type: text/html");
        writer.println("Content-Length: " + file.length());
        writer.println();
        writer.flush();

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }
        fileInputStream.close();
      } else {
        sendErrorResponse(outputStream, 404, "Not Found", "The login page was not found.");
      }
    } catch (IOException e) {
      e.printStackTrace();
      sendErrorResponse(outputStream, 500, "Internal Server Error",
          "The server encountered an unexpected condition.");
    }
  }

  /**
   * Handles login requests.
   * 
   * @param outputStream The output stream to the client
   * @param reader       The buffered reader for reading client input
   */
  public static void handleLogin(OutputStream outputStream, BufferedReader reader) {
    try {
      String data = reader.readLine();

      while (reader.ready()) {
        data += reader.readLine();
      }
      System.out.println(data);
      String[] credentials = data.split("&");

      // Extract username and password
      String username = URLDecoder.decode(credentials[0].split("=")[1], StandardCharsets.UTF_8);
      String password = URLDecoder.decode(credentials[1].split("=")[1], StandardCharsets.UTF_8);

      // Assuming login credentials are stored in a text file named "LoginInfo.txt"
      String filename = "/LoginInfo.txt";
      Path path = Paths.get(filename);

      if (Files.exists(path)) {
        String storedCredentials = new String(Files.readAllBytes(path));
        String[] stored = storedCredentials.split("=");
        String storedUsername = stored[0];
        String storedPassword = stored[1];

        if (username.equals(storedUsername) && password.equals(storedPassword)) {
          sendSuccessResponse(outputStream);
        } else {
          sendErrorResponse(outputStream, 401, "Unauthorized", "Incorrect username or password.");
        }
      } else {
        sendErrorResponse(outputStream, 401, "Unauthorized", "User credentials file not found.");
      }
    } catch (IOException e) {
      e.printStackTrace();
      sendErrorResponse(outputStream, 500, "Internal Server Error",
          "The server encountered an unexpected condition.");
    }
  }

  /**
   * Sends a success response to the client.
   * 
   * @param outputStream The output stream to the client
   */
  private static void sendSuccessResponse(OutputStream outputStream) {
    try {
      PrintWriter writer = new PrintWriter(outputStream);
      writer.println("HTTP/1.1 200 OK");
      writer.println("Content-Type: text/plain");
      writer.println();
      writer.println("Login successful!");
      writer.flush();

      serveFile(outputStream, "/PngInsert.html");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Sends an error response to the client.
   * 
   * @param outputStream The output stream to the client
   * @param statusCode   The HTTP status code
   * @param statusText   The status text
   * @param errorMessage The error message
   */
  private static void sendErrorResponse(OutputStream outputStream, int statusCode, String statusText,
      String errorMessage) {
    try {

      System.out.print("Client: " + InetAddress.getLocalHost().getHostAddress());
      System.out.println(", Server: " + InetAddress.getLocalHost().getHostName());
      System.out.println("Response: HTTP/1.1 " + statusCode + " " + statusText + ", " + errorMessage);
      System.out.print("Date: " + new Date());
      System.out.println(", Server Name: SimpleWebServer");
      System.out.print(", Connection: close");
      System.out.println(", Content-Type: text/html\n");

      PrintWriter writer = new PrintWriter(outputStream);
      writer.println("HTTP/1.1 " + statusCode + " " + statusText);
      writer.println();
      writer.println("<html><head><title>" + statusCode + " " + statusText + "</title></head><body>");
      writer.println("<h1>" + statusCode + " " + statusText + "</h1>");
      writer.println("<p>" + errorMessage + "</p>");
      writer.println("</body></html>");
      writer.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void sendRedirectResponse(OutputStream outputStream, int statusCode, String statusText,
      String errorMessage) {
    try {
      System.out.print("Client: " + InetAddress.getLocalHost().getHostAddress());
      System.out.println(", Server: " + InetAddress.getLocalHost().getHostName());
      System.out.print("Response: HTTP/1.1 " + statusCode + " " + statusText);
      System.out.print(", Date: " + new Date());
      System.out.println(", Server Name: SimpleWebServer");
      System.out.print("Redirecting to: /a/b/index.html\n\n");

      PrintWriter writer = new PrintWriter(outputStream);
      writer.println("HTTP/1.1 302 Found");
      writer.println("Location: /a/b/index.html");
      writer.println();
      writer.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets the file extension from the file name.
   * 
   * @param fileName The file name
   * @return The file extension
   */
  private static String getFileExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index == -1) {
      return "";
    } else {
      return fileName.substring(index + 1);
    }
  }
}