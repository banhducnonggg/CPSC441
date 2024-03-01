
/**
 * WebClient Class
 * 
 * CPSC 441
 * Assignment 2
 * 
 * @author 	Majid Ghaderi
 * @version	2024
 *
 */

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.*;

import javax.net.ssl.SSLSocketFactory;

public class WebClient {

	private static final Logger logger = Logger.getLogger("WebClient"); // global logger

    /**
     * Default no-arg constructor
     */
	public WebClient() {
		// nothing to do!
	}
	
    /**
     * Downloads the object specified by the parameter url.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     */
	public void getObject(String url) {

        // Setup variables
        boolean setSecured = url.startsWith("https://");
        int port = setSecured ? 443 : 80; // Set default port number for https and http
        url = url.replaceAll("^https?://", "");
        String [] urlHostSplit = url.split("/", 2);
        String hostPart = urlHostSplit[0];
        String hostName;
        String pathName = (urlHostSplit.length > 1) ? "/" + urlHostSplit[1] : "/";

        // Extract file name, set default to medium.pdf if not included in url
        String fileName = pathName.substring(pathName.lastIndexOf("/") + 1);
        if (fileName.isEmpty() || fileName.contains("?")) {
            fileName = "medium.pdf";
        }
        
        // Check if the host part contains a port
        if (hostPart.contains(":")) {
            String[] hostPortSplit = hostPart.split(":", 2);
            hostName = hostPortSplit[0];
            try {
                port = Integer.parseInt(hostPortSplit[1]); // Override default port if specified
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "Invalid port number in URL, using default port.", e);
            }
        } else {
            hostName = hostPart;
        }

        // if url specifies path name then use it, otherwise use medium.pdf (default)
        if (urlHostSplit[1].isEmpty()) {
            pathName = "/medium.pdf";
        } else {
            pathName = "/" + urlHostSplit[1];
        }

        Socket socket = null;

        try {
            // Use secure TCP connection if setSecured == true (SSLSocketFactory)
            // Use regular TCP connection if setSecured == false
            if (setSecured == true) {
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = sslSocketFactory.createSocket(hostName, port);
            } else {
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostName, port));
            }

            // Send GET request

            // Setup variables
            OutputStream outputStream = socket.getOutputStream();

            // Construct GET request 
            String requestLine = "GET " + pathName + " HTTP/1.1\r\n";
            String hostHeader = "Host: " + hostName + "\r\n";
            String endHeader = "Connection: close\r\n\r\n";

            // Print out the request
            String httpRequest = requestLine + hostHeader + endHeader;
            System.out.println(httpRequest);

            // Convert httpRequest
            byte [] httpRequestByte = httpRequest.getBytes("US-ASCII");

            // Send request
            outputStream.write(httpRequestByte);
            outputStream.flush();

            // Reading server response

            // Setup variables
            InputStream inputStream = socket.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            String headerOuString = null;
            byte[] buffer = new byte[1024];
            int length = 1;
            int offset = 0;
            int readByte = 0;

            // Read header part of response then move on to next step
            while ((readByte = inputStream.read(buffer, offset, length)) != -1) {
                offset++;
                headerOuString = new String(buffer, 0, offset, "US-ASCII");
                if (headerOuString.contains("\r\n\r\n")) {
                    break;
                }
            }

            System.out.println(headerOuString);

            // Save and write to local file if response is OK

            if (headerOuString.contains("200 OK")) {
                String [] content1 = headerOuString.split("Content-Length: ", 2);
                String [] content2 = content1[1].split("\r\n", 2);
                int objSize = Integer.parseInt(content2[0]);

                BufferedInputStream buffIn = new BufferedInputStream(inputStream);
                BufferedOutputStream buffOut = new BufferedOutputStream(fileOutputStream);
                byte [] buff_1 = new byte[32 * 1024];
				int count_bytes = 0;

                while ((readByte = buffIn.read(buff_1)) != -1) {
					buffOut.write(buff_1, 0, readByte);
					buffOut.flush();
					count_bytes = count_bytes + readByte;
					if (count_bytes == objSize) {
						break;
					}
				}

                // Close file output stream
                fileOutputStream.close();

                System.out.println("File downloaded: " + fileName);

                // Close stream
                inputStream.close();
                buffIn.close();
                buffOut.close();

            } else {
                String [] error = headerOuString.split("\r\n", 2);
				System.out.println("Error downloading file: " + error[0]);
				System.exit(1);
            }

            // Close socket, use .shutdownOutput() to close SSLSocketFactory
            if (setSecured == true) {
                socket.shutdownOutput();
            } else {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }
    }

}
