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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.io.FileReader;

/**
 * A simple web server that can serve static files and handle login
 * functionality.
 */
public class SimpleWebServer {

  // Constants
  private static final int BUFFER_SIZE = 8192;

  // Map content types for different file extensions
  private static final Map<String, String> CONTENT_TYPES = Map.of(
      "html", "text/html",
      "htm", "text/html",
      "jpg", "image/jpeg",
      "jpeg", "image/jpeg",
      "png", "image/png");

  /**
   * Main method to start the web server.

   * @param args Command-line arguments: [port]
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.exit(1);
    }

    int port = Integer.parseInt(args[0]);
    String publicFolderPath = args[1];
    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(port);
      System.out.println("Listening for connection on port " + port
          + ", serving files from " + publicFolderPath);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        handleClientRequest(clientSocket, publicFolderPath);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Handles client requests.

   * @param clientSocket The client socket
   */
  public static void handleClientRequest(Socket clientSocket, String publicFolderPath) {
    try (
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream outputStream = clientSocket.getOutputStream();) {

      String request = reader.readLine();
      if (request == null) {
        sendErrorResponse(outputStream, 400, "Bad Request", "Invalid request.");
        return;
      }

      System.out.println("Received request: " + request);

      String[] tokens = request.split(" ");
      String method;
      String path;
      String version;
      if (tokens.length >= 3) {
        method = tokens[0];
        path = tokens[1];
        version = tokens[2];

        System.out.println("Method: " + method + ", Path: " + path + ", Version: " + version);
      } else {
        sendErrorResponse(outputStream, 400, "Bad Request", "Invalid request.");
        return;
      }

      if (path.equals("/")) {
        path = "/index.html";
      }

      if (method.equals("GET")) {
        if (path.equals("/login.html")) {
          serveLoginHtml(outputStream, publicFolderPath);
        } else if (path.equals("/redirect")) {
          sendRedirectResponse(outputStream, 302, "Found", "Redirecting to /a/b/index.html");
          serveFile(outputStream, "/a/b/index.html", publicFolderPath);
        } else {
          serveFile(outputStream, path, publicFolderPath);
        }
      } else if (method.equals("POST") && path.equals("/login")) {
        System.out.println("\nHandling login request...");
        handleLogin(outputStream, reader, publicFolderPath);
      } else if (method.equals("POST") && path.equals("/upload")) {
        handleImageUpload(outputStream, reader, publicFolderPath);
      } else {
        // Handle other request methods.
        sendErrorResponse(outputStream, 404, "Not Found", "The requested resource was not found.");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Serves a file requested by the client.

   * @param outputStream The output stream to the client
   * @param filePath     The path of the requested file
   */
  public static void serveFile(OutputStream outputStream, String filePath, String publicFolderPath) {
    try {
      File file = new File(publicFolderPath + filePath);

      if (file.exists()) {
        if (file.isDirectory()) {
          File indexFile = new File(file, "index.html");
          if (indexFile.exists() && !indexFile.isDirectory()) {
            file = indexFile;
          } else {
            sendErrorResponse(outputStream, 404, "Not Found", "The requested file was not found.");
            return;
          }
        }

        String extension = getFileExtension(file.getName());
        String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");

        System.out.print("Serving file: " + filePath);
        System.out.print(", Client: " + InetAddress.getLocalHost().getHostAddress());
        System.out.println(", Server: " + InetAddress.getLocalHost().getHostName());
        System.out.print("Response: HTTP/1.1 200 OK");
        System.out.print(", Date: " + new Date());
        System.out.println(", Server Name: SimpleWebServer");
        System.out.print("Content-Length: " + file.length() + ", ");
        System.out.print("Connection: close");
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

   * @param outputStream The output stream to the client
   */
  public static void serveLoginHtml(OutputStream outputStream, String publicFolderPath) {
    try {
      File file = new File(publicFolderPath + "/login.html");

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
   * @param outputStream    The output stream to the client
   * @param reader          The buffered reader for reading client input
   * @param publicFolderPath The public folder path
   */
  public static void handleLogin(OutputStream outputStream, BufferedReader reader, String publicFolderPath) {
    try {
      boolean accessGranted = false;
      String data = "";
      while (reader.ready()) {
        data += (char) reader.read();
      }
      String cutHeaders = data.substring(data.indexOf("\r\n\r\n") + 4);

      String[] formData = cutHeaders.split("&");

      String username = "";
      String password = "";

      for (String pair : formData) {
        String[] keyValue = pair.split("=");
        if (keyValue.length == 2) {
          String key = keyValue[0];
          String value = keyValue[1];
          if ("username".equals(key)) {
            username = value;
          } else if ("password".equals(key)) {
            password = value;
          }
        }
      }

      if (username != null && password != null) {
        System.out.println("\nUsername: " + username + ", Password: " + password);
        String workingDir = System.getProperty("user.dir");
        String filename = "LoginInfo.txt";
        File file = new File(workingDir ,"LoginInfo.txt");
        if (file.exists()) {
          System.out.println("LoginInfo.txt found.");
          String userData = "";
          try {
            FileReader fr = new FileReader(filename);
            while (fr.ready()) {
              char c = (char) fr.read();
              userData += c;
            }
            String[] userDatas = userData.split("\n");
            for (String user : userDatas) {
              String[] userCreds = user.split("=");
              System.out.println("User: " + userCreds[0] + ", Password: " + userCreds[1]);
              if (userCreds[0].equals(username) && userCreds[1].equals(password)) {
                accessGranted = true;
                break;
              }
            }
            if (!accessGranted) {
              sendErrorResponse(outputStream, 401, "Unauthorized", "Invalid username or password.");
            } else {
              sendSuccessResponse(outputStream, publicFolderPath);
            }
            fr.close();
          } catch (IOException e) {
            e.printStackTrace();
            sendErrorResponse(outputStream, 500, "Internal Server Error",
                "The server encountered an unexpected condition.");
          }
        } else {
          sendErrorResponse(outputStream, 500, "Internal Server Error",
              "The server encountered an unexpected condition.");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      sendErrorResponse(outputStream, 500, "Internal Server Error",
                "The server encountered an unexpected condition.");
    }
  }

  /**
   * Handles image upload requests.

   * @param outputStream The output stream to the client.
   * @param reader The buffered reader for reading client input.
   * @param publicFolderPath The public folder path.
   */
  public static void handleImageUpload(OutputStream outputStream, BufferedReader reader, String publicFolderPath) {
    try {
      MultiPartFormData formData = new MultiPartFormData();

      byte[] imageData = formData.getFile("image");

      String uploadPath = "/uploads";
      String fileName = uploadPath + "/uploaded_image.jpg";
      Files.write(Paths.get(publicFolderPath + fileName), imageData);

      PrintWriter writer = new PrintWriter(outputStream);
      writer.println("HTTP/1.1 200 OK");
      writer.println("Content-Type: text/plain");
      writer.println();
      writer.println("Image uploaded successfully!");
      writer.println("Location: " + fileName);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
      sendErrorResponse(outputStream, 500, "Internal Server Error",
          "Error handling image upload.");
    }
  }

  /**
   * Sends a success response to the client.

   * @param outputStream The output stream to the client
   */
  private static void sendSuccessResponse(OutputStream outputStream, String publicFolderPath) {
    try {
      PrintWriter writer = new PrintWriter(outputStream);
      writer.println("HTTP/1.1 200 OK");
      writer.println("Content-Type: text/plain");
      writer.println();
      writer.println("Login successful!");

      // Redirect to the login page
      writer.println("Location: /login.html");
      writer.println();
      writer.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Sends an error response to the client.

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
      System.out.println(", Server Name: SimpleWebServer, ");
      System.out.print("Connection: close");
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