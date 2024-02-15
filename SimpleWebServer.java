import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * A simple web server that can serve static files.
 */
public class SimpleWebServer {

  private static final int bufferSize = 8192;

  private static final Map<String, String> contentTypes = Map.of(
      "html", "text/html",
      "htm", "text/html",
      "jpg", "image/jpeg",
      "jpeg", "image/jpeg",
      "png", "image/png"
  );

  /**
   * Main entry point of the web server.
   */
  public static void main(String[] args) {
    // Exit if port and path are not provided
    if (args.length != 2) {
      System.exit(1);
    }

    int port = Integer.parseInt(args[0]);
    String publicFolder = args[1];
    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Listening for connection on port " + port);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        handleClientRequest(clientSocket, publicFolder);
      }

    } catch (Exception e) {
      e.printStackTrace();
    } 
  }

  /**
   * Handle the client request.
   */
  public static void handleClientRequest(Socket clientSocket, String publicFolder) {
    try (
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream outputStream = clientSocket.getOutputStream();
    ) {
      String request = reader.readLine();
      System.out.println("Received request: " + request);

      String filePath = publicFolder + extractFilePath(request);
      serveFile(filePath, outputStream);

    } catch (IOException e) {
      e.printStackTrace();
    }
    
  }

  /**
   * Extract the file path from the request.
   */
  public static String extractFilePath(String request) {
    String[] tokens = request.split(" ");
    String requestPath = tokens[1];

    if ("/".equals(requestPath)) {
      return "/index.html";
    }

    Path filePath = Paths.get(requestPath).normalize();

    if (Files.isDirectory(filePath)) {
      return filePath.resolve("index.html").toString().replace(File.separatorChar, '/');
    } else {
      return filePath.toString().replace(File.separatorChar, '/');
    }
  }

  /**
   * Serve the file to the client.
   */
  public static void serveFile(String filePath, OutputStream outputStream) {
    try {
      System.out.println("Attempting to serve file: " + filePath);
      File file = new File(filePath);

      if (file.exists()) {
        if (file.isFile()) {
          PrintWriter writer = new PrintWriter(outputStream);
          writer.println("HTTP/1.1 200 OK");
          writer.println("Content-Type: " + getContentType(filePath));
          writer.println("Content-Length: " + file.length());
          writer.println();
          writer.flush();

          FileInputStream fileInputStream = new FileInputStream(file);
          byte[] buffer = new byte[bufferSize];
          int bytesRead;
          while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
          }
          fileInputStream.close();

          outputStream.flush();
          System.out.println("File served successfully: " + filePath);

        } else if (file.isDirectory()) {
          String indexFilePath = filePath + "/index.html";
          serveFile(indexFilePath, outputStream);

          // Redirect, send 302 response
          String redirectPath = filePath + "/index.html";
          sendRedirectResponse(outputStream, redirectPath);

        } else {
          // Unsupported file type, send 415 response
          sendErrorResponse(outputStream, 415, "Unsupported File Type", "The server does not support the requested file type.");
        }
      } else {
        // File not found, send 404 response
        sendErrorResponse(outputStream, 404, "Not Found", "The requested file was not found on the server.");
        System.out.println("File not found: " + filePath);
      }
    } catch (IOException e) {
      // Internal server error, send 500 response
      try {
        sendErrorResponse(outputStream, 500, "Internal Server Error", "The server encountered an unexpected condition that prevented it from fulfilling the request.");
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }

  /**
   * Get the content type based on the file name.
   */
  public static String getContentType(String filePath) {
    String extension = getFileExtension(filePath);
    return contentTypes.getOrDefault(extension, "application/octet-stream");
  }

  /**
   * Get the file extension from the file name.
   */
  public static String getFileExtension(String filePath) {
    int index = filePath.lastIndexOf('.');
    if (index == -1) {
      return "";
    } else {
      return filePath.substring(index + 1);
    }
  }

  /**
   * Send an error response to the client.
   */
  private static void sendErrorResponse(OutputStream outputStream, int statusCode, String statusText, String errorMessage) throws IOException {
    PrintWriter writer = new PrintWriter(outputStream);
    writer.println("HTTP/1.1 " + statusCode + " " + statusText);
    writer.println();
    writer.println("<html><head><title>" + statusCode + " " + statusText + "</title></head><body>");
    writer.println("<h1>" + statusCode + " " + statusText + "</h1>");
    writer.println("<p>" + errorMessage + "</p>");
    writer.println("</body></html>");
    writer.flush();
    System.out.println("Error: " + statusCode + " " + statusText);
  }

  /**
   * Send a redirect response to the client.
   */
  private static void sendRedirectResponse(OutputStream outputStream) throws IOException {
    PrintWriter writer = new PrintWriter(outputStream);
    writer.println("HTTP/1.1 302 Found");
    writer.println("Location: https://www.example.com");
    writer.println();
    writer.flush();
    System.out.println("Redirecting to: https://www.example.com");
  }
}
