package mytcp.mainpack;
import java.io.*;
import java.net.*;

public class Server extends NSEProtocol {
	

	private int mainTCPPort;
	private int mainUDPPort;
	private NSEDatabase mainDB;
	private int TCPClientCounter = 1;
	private Socket clientTCPSocket = null;
	private ServerSocket serverTCPSocket = null;
	private DatagramSocket mainUDPSocket = null;
	private int serverStatus; //0 = OK, 1 = FAIL
	
	public Server() {
		mainDB = new NSEDatabase("main");
		mainTCPPort = DEFAULT_TCP_PORT;
		mainUDPPort = DEFAULT_UDP_PORT;
		serverStatus = 0;
	}
	
	public Server(String TCPPort, String UDPPort, String dbName) {
		serverStatus = 1;
		
		//Validate arguments
		if (TCPPort == null) {
			dumpLocalLog("Error, TCP port doesn't exist");
	    	return;
		}
		if (UDPPort == null) {
			dumpLocalLog("Error, UDP port doesn't exist");
	    	return;
		}
		if (dbName == null) {
			dumpLocalLog("Error, database name doesn't exist");
	    	return;
		}
		
	    //Validate ports
	    try {
	    	mainTCPPort = Integer.parseInt(TCPPort);
	    } catch (NumberFormatException e) {
	    	dumpLocalLog("Error, invalid TCP port: " + TCPPort);
	    	return;
	    }
	    try {
	    	mainUDPPort = Integer.parseInt(UDPPort);
	    } catch (NumberFormatException e) {
	    	dumpLocalLog("Error, invalid UDP port: " + UDPPort);
	    	return;
	    }
	    
	    //Compare ports
	    if (mainUDPPort == mainTCPPort) {
	    	dumpLocalLog("Error, ports cannot be equal: (" + mainUDPPort + " != " + mainTCPPort + ")");
	    	return;
	    }
	    
	    //Validate port ranges
	    if (mainTCPPort < 1025) {
	    	dumpLocalLog("Error, TCP port number below 1024");
	    	return;
	    }
	    if (mainUDPPort < 1025) {
	    	dumpLocalLog("Error, UDP port number below 1024");
	    	return;
	    }
	    
	    //Create database
		mainDB = new NSEDatabase(dbName);
		
		serverStatus = 0;
	}
	
	public int getServerStatus() {
		return serverStatus;
	}
	
	private int manageUDPClient(InetAddress clientAddress, int portNumber) {

		//Validate arguments
		if (clientAddress == null) {
			dumpLocalLog("Error, client address doesn't exist");
			return 1;
		}
		if (portNumber < 1025) {
			dumpLocalLog("Error, port number below 1024");
			return 1;
		}
		
		dumpUDPLog(mainUDPSocket, "Connected to an UDP client");
		
		//Send AKN message
		if (sendUDPPacket(AKN_SIG.getBytes(), mainUDPSocket, clientAddress, portNumber) != 0) return 1;

		byte[] packet;
		//Read student ID
		if ((packet = receiveUDPPacket(mainUDPSocket)) == null) return 1;
		
		//Search student ID in the DB
		int markIndex;
		if ((markIndex = mainDB.searchPlainRecord(packet)) == -1) {
			disconnectUDPSocket(mainUDPSocket);
			return 1;
		}
		dumpUDPLog(mainUDPSocket, "Student ID found");
		
		//Get mark location for this student
		markIndex++;
		if ((packet = mainDB.getPlainRecord(markIndex)) == null) {
			disconnectUDPSocket(mainUDPSocket);
			return 1;
		}
		dumpUDPLog(mainUDPSocket, "Student mark found");
		
		//Send student mark
		if (sendUDPPacket(packet, mainUDPSocket, clientAddress, portNumber) != 0) return 1;
		
		//Read AKN Close
		if ((packet = receiveUDPPacket(mainUDPSocket)) == null) return 1;
		
		//Validate AKN Close
		if (!new String(packet).substring(0, AKN_CLS.length()).equals(AKN_CLS)) {
			dumpUDPLog(mainUDPSocket, "Error, incorrect AKN CLOSE received");
			disconnectUDPSocket(mainUDPSocket);
			return 1;
		}
		dumpUDPLog(mainUDPSocket, "Closing notification validated");
		
		//Close connection
		disconnectUDPSocket(mainUDPSocket);
		return 0;
	}
	
	private int manageTCPClient(Socket clientSocket, int threadID) {
    	
		if (clientSocket == null) {
			dumpLocalLog("Error, client socket doesn't exist");
			return 1;
		}
		if (threadID < 0) {
			dumpLocalLog("Error, invalid thread number");
			return 1;
		}
		
		if (!clientSocket.isConnected()) {
			dumpLocalLog("Error, unconnected client socket");
			return 1;
		}
		
		dumpTCPLog(clientSocket, "Client accepted", threadID);
		
		//Open I/O streams
    	InputStream inStream;
		OutputStream outStream;
		try {
			inStream = clientSocket.getInputStream();
			outStream = clientSocket.getOutputStream();
		} catch (IOException e2) {
			dumpTCPLog(clientSocket, "Error, could not open input/output stream", threadID);
			closeTCPSocket(clientSocket, threadID);
			return 1;
		}
		dumpTCPLog(clientSocket, "I/O streams opened", threadID);
		
		//Send AKN message
		if (sendTCPPacket(clientSocket, outStream, AKN_SIG.getBytes(), threadID) != 0) return 1;
		
		byte[] mainData = new byte[PACKET_SIZE];

		//Read student ID
		if ((mainData = receiveTCPPacket(clientSocket, inStream, threadID)) == null) return 1;

		//Search student ID in the DB
		int markIndex;
		if ((markIndex = mainDB.searchPlainRecord(mainData)) == -1) {
			closeTCPSocket(clientSocket, threadID);
			return 1;
		}
		dumpTCPLog(clientSocket, "Student ID found", threadID);
		
    	//Get mark location for this student
    	markIndex++;
    	if ((mainData = mainDB.getPlainRecord(markIndex)) == null) {
    		closeTCPSocket(clientSocket, threadID);
    		return 1;
    	}
		dumpTCPLog(clientSocket, "Student mark found", threadID);
		
		//Send student mark
		if (sendTCPPacket(clientSocket, outStream, mainData, threadID) != 0) return 1;
		
		//Read AKN Close
		if ((mainData = receiveTCPPacket(clientSocket, inStream, threadID)) == null) return 1;
		
		//Validate AKN Close
		if (!new String(mainData).substring(0, AKN_CLS.length()).equals(AKN_CLS)) {
			dumpTCPLog(clientSocket, "Error, incorrect AKN CLOSE received", threadID);
			closeTCPSocket(clientSocket, threadID);
			return 1;
		}
		dumpTCPLog(clientSocket, "AKN Close validated", threadID);
		
		//Close connection
		closeTCPSocket(clientSocket, threadID);
		
		return 0;
	}
	
    public void startServer() {

    	//######################################################################################
    	//Prepare records
    	byte[] studentid0 = new String("W1234567").getBytes();
    	byte[] mark0 = new String("100%").getBytes();
    	byte[] studentid1 = new String("W7654321").getBytes();
    	byte[] mark1 = new String("99%").getBytes();
    	byte[] studentid2 = new String("W2222222").getBytes();
    	byte[] mark2 = new String("33%").getBytes();
    	
    	int y = 0;
    	if ((studentid0 = NSEEncryption.encryptMessage(studentid0)) == null) y++;
    	if ((studentid1 = NSEEncryption.encryptMessage(studentid1)) == null) y++;
    	if ((studentid2 = NSEEncryption.encryptMessage(studentid2)) == null) y++;

    	if ((mark0 = NSEEncryption.encryptMessage(mark0)) == null) y++;
    	if ((mark1 = NSEEncryption.encryptMessage(mark1)) == null) y++;
    	if ((mark2 = NSEEncryption.encryptMessage(mark2)) == null) y++;
    	
    	//Configure database
    	mainDB.createDBFile();
    	mainDB.flushRecords();
    	if (mainDB.addRecord(studentid0, mark0) != 0) y++;
    	if (mainDB.addRecord(studentid1, mark1) != 0) y++;
    	if (mainDB.addRecord(studentid2, mark2) != 0) y++;
    	if (y != 0) {
    		dumpLocalLog("Error, database could NOT be created successfully, exiting");
    		serverStatus = 1;
    		return;
    	}
    	//*##########################################################################################
    	
    	
    	mainDB.dumpDBLog("Database size: " + (mainDB.getTotalRecords() / 2));
    	dumpLocalLog("Database successfully created"); 
    	
    	//Open TCP port
    	try {
			serverTCPSocket = new ServerSocket(mainTCPPort);
		} catch (IOException e) {
			dumpLocalLog("Error, TCP connection could not be initiated, exiting");
			serverStatus = 1;
			return;
		}
    	dumpLocalLog("Started to listen TCP port: " + mainTCPPort);
    	
    	//Open UDP port
    	try {
    		mainUDPSocket = new DatagramSocket(mainUDPPort);
    	} catch (SocketException se) {
    		dumpLocalLog("Error, UDP connection could not be initiated, exiting");
    		serverStatus = 1;
			return;
    	}
    	dumpLocalLog("Started to listen UDP port: " + mainUDPPort);
    	
    	//Set UDP port timeouts
    	try {
			mainUDPSocket.setSoTimeout(DEFAULT_UDP_TIMEOUT);
		} catch (SocketException e) {
			disconnectUDPSocket(mainUDPSocket);
			dumpLocalLog("Connection timeout");
			serverStatus = 1;
			return;
		}
    	
    	try {
    		//start TCP
    		new Thread () {
    			@Override
    			public synchronized void run() {
			    	//Loop forever
			    	while (true) {
			    		//Accept client connection
			    		dumpLocalLog("Waiting for next TCP client...");
			    		try {
			    			clientTCPSocket = serverTCPSocket.accept();
						} catch (IOException e) {
							dumpLocalLog("Error, could not accept client");
							continue;
						}
			    		dumpLocalLog("Starting to serve the next client");
			    		new Thread() {
			    			@Override
			    			public synchronized void run(){

			    				int currentThread = TCPClientCounter;
			    				Socket localClient = clientTCPSocket;
			    				
			    				TCPClientCounter++;
			    				
			    				//Check socket
			    				if (localClient == null) {
			    					dumpLocalLog("Error, invalid client socket");
			    					return;
			    				}
			    				
			    				//Set TCP socket timeout
			    				try {
									localClient.setSoTimeout(DEFAULT_TCP_TIMEOUT);
								} catch (SocketException e) {
									closeTCPSocket(localClient, currentThread);
									dumpLocalLog("Connection timeout");
									return;
								}
			    				
			    				//Get started
			    		    	if (manageTCPClient(localClient, currentThread) != 0) {
			    		    		dumpLocalLog("Error, client at " + localClient.getRemoteSocketAddress().toString() + " served unsuccessfully");
			    		    	} else {
			    		    		dumpTCPLog(localClient, "Client at " + localClient.getRemoteSocketAddress().toString() + " served successfully", currentThread);
			    		    	}
			    		    }
			    			//Start accepting individual clients in threads
			    		}.start();
			    	}
    			}
    			//Start the entire TCP service in a thread
    		}.start();
    		
    		//Start UDP thread
    		new Thread () {
    			@Override
    			public synchronized void run() {
			    	//Loop forever
			    	while (true) {
			    		//Accept client connection
			    		dumpLocalLog("Waiting for next UDP client...");
			    		DatagramPacket data = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
			    		//Validate waking packet
			    		do {
			    			try {
				        		mainUDPSocket.receive(data);
				        		break;
				    		} catch (IOException e) {
				    			continue;
				    		}
			    		} while (!new String(data.getData().toString().substring(0, UDP_BYPASS.length())).equals(UDP_BYPASS));
			    		//Validate UDP bypass
		        		dumpUDPLog(mainUDPSocket, "UDP connection bypassed");
			    		manageUDPClient(data.getAddress(), data.getPort());
			    	}
    			}
    		}.start();
    		
    		//keep server alive
    		while (true);
    	} finally {
    		if (serverTCPSocket != null) {
    			try {
    				serverTCPSocket.close();
				} catch (IOException e) {
					dumpLocalLog("Error, unable to close server socket");
					serverStatus = 1;
					return;
				}
    		}
    	}
    }
}
