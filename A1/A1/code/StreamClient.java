
/**
 * StreamClient Class
 * 
 * CPSC 441
 * Assignment 1
 *
 */


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.*;


public class StreamClient {

	private static final Logger logger = Logger.getLogger("StreamClient"); // global logger
	public static int buffersize;	// global buffer size
	public static Socket socket;	// global socket


	/**
	 * Constructor to initialize the class.
	 * 
	 * @param serverName	remote server name
	 * @param serverPort	remote server port number
	 * @param bufferSize	buffer size used for read/write
	 */
	public StreamClient(String serverName, int serverPort, int bufferSize) {
		buffersize = bufferSize;
		InetSocketAddress serverSocketAddress = new InetSocketAddress(serverName, serverPort);
		socket = new Socket();

		//	attempt to connect to serverSocketAddress, and catch the exception if failed
		try {
			socket.connect(serverSocketAddress);
		} catch (IOException e) {
			throw new RuntimeException("Connection failed: " + e.getMessage(), e);
		}

	}

	
	/**
	 * Compress the specified file via the remote server.
	 * 
	 * @param inName		name of the input file to be processed
	 * @param outName		name of the output file
	 */
	public void getService(int serviceCode, String inName, String outName) {
		//	Send the service code to the server
		try {
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
			//	Send the service code as a byte
			dos.writeByte(serviceCode);
			dos.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error sending code: " + e.getMessage(), e);
		}

		//	Create objects for Writing and Reading
		Writing writing = new Writing(socket, inName);
		Reading reading = new Reading(socket, outName);

		//	Create threads by passing objects then start
		Thread writeThread = new Thread(writing);
		Thread readThread = new Thread(reading);

		writeThread.start();
		readThread.start();

		// Wait for threads to finish their jobs before starting new jobs (concurrent design)
		try {
			writeThread.join();
			readThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				//	Close socket
				socket.close();
			} catch(IOException ioe) {
				System.out.println("Error when closing socket: " + ioe);
				System.exit(1);
			}
		}
	}

}

//	Read the input file and write to the socket
class Writing implements Runnable{

	private Socket socket;
	private String inFile;
	
	Writing (Socket socket, String inFile) {
		this.socket = socket;
		this.inFile = inFile;
	}

	private void writingJob() {
		try (FileInputStream fileInputStream = new FileInputStream(inFile);
			 BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			 OutputStream sockOutputStream = socket.getOutputStream()) {
				
				byte[] buff = new byte[StreamClient.buffersize];
				int numBytes;

				while ((numBytes = bufferedInputStream.read(buff)) != -1) {
					sockOutputStream.write(buff, 0, numBytes);
					sockOutputStream.flush();

					// Print number of bytes written
					System.out.println("W " + numBytes);
				}
		} catch (IOException ioe) {
			System.out.println("IO Exception in Writing: " + ioe );
			System.exit(1);
		}
	}

	public void run() {
		writingJob();
	}
}

//	Read from the socket and write to the output file
class Reading implements Runnable {
    
    private Socket socket;
    private String outFile;

    Reading (Socket socket, String outName) {
        this.socket = socket;
        this.outFile = outName;
    }

    private void readingJob() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(outFile);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
             InputStream socketInputStream = socket.getInputStream()) {

            byte[] buff = new byte[StreamClient.buffersize];
            int numBytes;

            while ((numBytes = socketInputStream.read(buff)) != -1) {
                bufferedOutputStream.write(buff, 0, numBytes);
                bufferedOutputStream.flush();

				// Print number of bytes read
				System.out.println("R " + numBytes);
            }

        } catch (IOException ioe) {
            System.out.println("IO Exception in Reading: " + ioe);
            System.exit(1);
        }
    }

    public void run() {
        readingJob();
    }
}
