package mytcp.mainpack;
import java.io.*;
import java.net.*;

public class NSEProtocol {
	
	//Constants
	public static final int PACKET_SIZE = 128;
	public static final int DEFAULT_TCP_TIMEOUT = 1000;
	public static final int DEFAULT_UDP_TIMEOUT = 1000;
	public static final int DEFAULT_TCP_PORT = 1448;
	public static final int DEFAULT_UDP_PORT = 1449;
	public static final String AKN_SIG = "AKN";
	public static final String AKN_CLS = "CAKN";
	public static final String UDP_BYPASS = "UKKU";
	
	public static void dumpLocalLog(String msg) {
		if (msg != null) System.out.println("[/localhost]:\t\t" + msg);
	}
	
	public static void dumpTCPLog(Socket s, String msg, int threadID) {
		if (msg != null)
			if (threadID > 0) {
				if (s != null)
					System.out.println("[" + s.getRemoteSocketAddress().toString() +"]:\t" + "(" + threadID + ")" + msg);
				else
					System.out.println("[/localhost]:\t\t" + msg);
			} else {
				if (s != null)
					System.out.println("[" + s.getRemoteSocketAddress().toString() +"]:\t" + msg);
				else
					System.out.println("[/localhost]:\t\t" + msg);
			}
		return;
	}
	
	public static void dumpUDPLog(DatagramSocket s, String msg) {
		if (msg != null)
			if (s != null)
				if (s.getRemoteSocketAddress() != null)
					System.out.println("[" + s.getRemoteSocketAddress().toString() +"]:\t" + msg);
			else
				System.out.println("[/localhost]:\t\t" + msg);
		return;
	}
	
	//Receives a packet and returns it
	public static byte[] receiveTCPPacket(Socket s, InputStream inStream, int threadID) {
		//Validate arguments
		if (s == null) {
			dumpTCPLog(s, "Error, client socket doesn't exist", threadID);
			return null;
		}
		if (inStream == null) {
			dumpTCPLog(s, "Error, client input stream doesn't exist", threadID);
			closeTCPSocket(s, threadID);
			return null;
		}
		if (threadID < 0) {
			dumpTCPLog(s, "Error, invalid thread ID", threadID);
			closeTCPSocket(s, threadID);
			return null;
		}
		
		int lastReadSize, totalReadSize = 0;
		byte[] mainData = new byte[PACKET_SIZE];
		

		while (totalReadSize < PACKET_SIZE) {
			try {
    			if ((lastReadSize = inStream.read(mainData, totalReadSize, PACKET_SIZE - totalReadSize)) == -1) {
    				dumpTCPLog(s, "Error, connection closed prematurely", threadID);
    				closeTCPSocket(s, threadID);
    				return null;
    			}
    			totalReadSize += lastReadSize;
			} catch (IOException e) {
				dumpTCPLog(s, "Error, could not read data, exiting", threadID);
				closeTCPSocket(s, threadID);
				return null;
			}
		}
		
		if ((mainData = NSEEncryption.decryptMessage(mainData)) == null) {
			closeTCPSocket(s, threadID);
			return null;
		}
		dumpTCPLog(s, "Message received: " + new String(mainData), threadID);
		return mainData;
	}
	
	//Sends a packet
	public static int sendTCPPacket(Socket s, OutputStream outStream, byte[] packet, int threadID) {

		//Validate arguments
		if (s == null) {
			dumpTCPLog(s, "Error, client socket doesn't exist", threadID);
			return 1;
		}
		if (packet == null) {
			dumpTCPLog(s, "Error, packet doesn't exist", threadID);
			closeTCPSocket(s, threadID);
			return 1;
		}
		if (outStream == null) {
			dumpTCPLog(s, "Error, client input stream doesn't exist", threadID);
			closeTCPSocket(s, threadID);
			return 1;
		}
		if (threadID < 0) {
			dumpTCPLog(s, "Error, invalid thread ID", threadID);
			closeTCPSocket(s, threadID);
			return 1;
		}
		
		byte[] tempData = packet;
		
		if ((packet = NSEEncryption.encryptMessage(packet)) == null) {
			closeTCPSocket(s, threadID);
			return 1;
		}

		try {
			outStream.write(packet);
			int i = packet.length;
	    	while (i < PACKET_SIZE) {
	    		outStream.write('\0');
	    		i++;
	    	}
		} catch (IOException e1)   {
			dumpTCPLog(s, "Error, could not write to server", threadID);
			closeTCPSocket(s, threadID);
			return 1;
		}
		dumpTCPLog(s, "Message sent: " + new String(tempData), threadID);
		return 0;
	}
	
	public static void closeTCPSocket(Socket s, int threadID) {
		if (s != null)
			try {
				s.close();
			} catch (IOException e) {
				dumpTCPLog(null, "Error, could not close the socket", threadID);
			}
		dumpTCPLog(null, "Connection closed", threadID);
	}
	
	public static int sendUDPPacket(byte[] packet, DatagramSocket socket, InetAddress serverIP, int portNumber) {

		//Validate arguments
		if (socket == null) {
			dumpLocalLog("Error, client socket doesn't exist");
			return 1;
		}
		if (serverIP == null) {
			dumpUDPLog(socket, "Error, server address doesn't exist");
			disconnectUDPSocket(socket);
			return 1;
		}
		if (portNumber < 1025) {
			dumpUDPLog(socket, "Error, invalid thread ID");
			disconnectUDPSocket(socket);
			return 1;
		}
		if (packet == null) {
			dumpTCPLog(null, "Error, UDP packet doesn't exist", 0);
			disconnectUDPSocket(socket);
			return 1;
		}
		
		byte[] mainPacket = packet;
		
		if ((packet = NSEEncryption.encryptMessage(packet)) == null) {
			disconnectUDPSocket(socket);
			return 1;
		}
		try {
			socket.send(new DatagramPacket(packet, PACKET_SIZE, serverIP, portNumber));
		} catch (IOException e) {
			dumpTCPLog(null, "Error, could not send UDP packet", 0);
			disconnectUDPSocket(socket);
			return 1;
		}
		dumpTCPLog(null, "Message sent: " + new String(mainPacket), 0);
		return 0;
	}
	
	public static byte[] receiveUDPPacket(DatagramSocket socket) {

		//Validate arguments
		if (socket == null) {
			dumpLocalLog("Error, client socket doesn't exist");
			return null;
		}
		
		byte[] mainData = new byte[PACKET_SIZE];
		DatagramPacket mainPacket = new DatagramPacket(mainData, PACKET_SIZE);

		try {
    		socket.receive(mainPacket);
    		mainData = mainPacket.getData();
		} catch (IOException e) {
			dumpTCPLog(null, "Error, could not receive data", 0);
			disconnectUDPSocket(socket);
			return null;
		}
		
		if ((mainData = NSEEncryption.decryptMessage(mainData)) == null) {
			disconnectUDPSocket(socket);
			return null;
		}
		dumpTCPLog(null, "Message received: " + new String(mainData), 0);
		return mainData;
	}
	
	public static void disconnectUDPSocket(DatagramSocket s) {
		if (s != null) s.disconnect();
		dumpLocalLog("UDP connection disconnected");
	}
}
