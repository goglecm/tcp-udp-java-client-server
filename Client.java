package mytcp.mainpack;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Client extends NSEProtocol{

	private JLabel UDPextraFld = new JLabel();
	private JLabel UDPoutputFld = new JLabel();
	private JLabel TCPextraFld = new JLabel();
	private JLabel TCPoutputFld = new JLabel();
    
	private int AcceptUDPClient(String serverAddressStr, String ServerPortStr, String login) {

		//Validate arguments
		if (serverAddressStr == null) {
			dumpLocalLog("Error, missing server address, exiting");
			return 1;
		}
		dumpLocalLog("Server address exists");
		
		if (ServerPortStr == null) {
			dumpLocalLog("Error, missing port, exiting");
			return 1;
		}
		dumpLocalLog("Server port exists");
		
		if (login == null) {
			dumpLocalLog("Error, missing login, exiting");
			return 1;
		}
		dumpLocalLog("Login exists");
		
		//Further validate server address
		InetAddress serverAddress = null;
		try {
			serverAddress = InetAddress.getByName(serverAddressStr);
		} catch (UnknownHostException e1) {
			dumpLocalLog("Error, unknown server address, exiting");
			return 1;
		}
		dumpLocalLog("Server address successfully validated");
		
		//Convert port number to integer
		int ServerPort;
		try {
			ServerPort = Integer.parseInt(ServerPortStr);
		} catch (NumberFormatException e) {
			dumpLocalLog("Error, illegal port, exiting");
			return 1;
		}
		dumpLocalLog("Server port is correct");
				
		//Validate port number range
		if (ServerPort < 1025) {
			dumpLocalLog("Error, port number below 1024, not permitted, exiting");
			return 1;
		}
		dumpLocalLog("Server port successfully validated");

		//Create socket to server
		DatagramSocket mainSocket = null;
		try {
			mainSocket = new DatagramSocket();
		} catch (IOException e3) {
			dumpLocalLog("Error, could not connect to server, exiting");
			return 1;
		}
		dumpLocalLog("Connected to " + serverAddress.getHostAddress());
		
		//Set timeout for the socket
		try {
			mainSocket.setSoTimeout(DEFAULT_UDP_TIMEOUT);
		} catch (SocketException e) {
			disconnectUDPSocket(mainSocket);
			dumpLocalLog("Connection timeout");
			return 1;
		}
		
		//Send waking packet to server
		if (sendUDPPacket(UDP_BYPASS.getBytes(), mainSocket, serverAddress, ServerPort) != 0) {
			disconnectUDPSocket(mainSocket);
			return 1;
		}

		byte[] mainData = new byte[PACKET_SIZE];

		//Read AKN from server
		if ((mainData = receiveUDPPacket(mainSocket)) == null) return 1;
		
		//Send message to GUI
		UDPextraFld.setText(new String(mainData));
		
		//Validate ANK signal
		if (new String(mainData).substring(0, AKN_SIG.length()).equals(AKN_SIG)) 
			dumpUDPLog(mainSocket, "AKN signal validated");
		else {
			dumpUDPLog(mainSocket, "AKN signal was NOT validated, exiting");
			disconnectUDPSocket(mainSocket);
			return 1;
		}
		
		//Send login to server
		if (sendUDPPacket(login.getBytes(), mainSocket, serverAddress, ServerPort) != 0) {
			disconnectUDPSocket(mainSocket);
			return 1;
		}
		
		//Send message to GUI
		UDPoutputFld.setText(login);
				
		//Read mark
		if ((mainData = receiveUDPPacket(mainSocket)) == null) {
			disconnectUDPSocket(mainSocket);
			return 1;
		}

		//Send mark to GUI
		UDPextraFld.setText(new String(mainData));
		
		//Send close notification to server
		if (sendUDPPacket(AKN_CLS.getBytes(), mainSocket, serverAddress, ServerPort) != 0) {
			disconnectUDPSocket(mainSocket);
			return 1;
		}

		//Disconnect socket
		disconnectUDPSocket(mainSocket);

		return 0;
	}

	public void createUDPUI() {
        //Create and set up the frame
        JFrame clientFrame = new JFrame();
        clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        clientFrame.setLayout(new GridLayout(6, 2, 5, 5));
        
        //Create elements
        JButton submitBtn = new JButton("Submit");
        JButton quitBtn = new JButton("Quit");
        JTextField serverFld = new JTextField("localhost");
        JTextField portFld = new JTextField(DEFAULT_UDP_PORT + "");
        JTextField inputFld = new JTextField("W1234567");

        //Add elements to frame
        clientFrame.getContentPane().add(new JLabel("Server IP: "));
        clientFrame.getContentPane().add(serverFld);
        clientFrame.getContentPane().add(new JLabel("Server port: "));
        clientFrame.getContentPane().add(portFld);
        clientFrame.getContentPane().add(new JLabel("Student ID: "));
        clientFrame.getContentPane().add(inputFld);
        clientFrame.getContentPane().add(submitBtn);
        clientFrame.getContentPane().add(quitBtn);
        clientFrame.getContentPane().add(new JLabel("Last sent message: "));
        clientFrame.getContentPane().add(UDPoutputFld);
        clientFrame.getContentPane().add(new JLabel("Last received message: "));
        clientFrame.getContentPane().add(UDPextraFld);
        
        //Action listeners
        submitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){
                new Thread() {
                	@Override
                    public void run() {
                    	AcceptUDPClient(serverFld.getText(), portFld.getText(), inputFld.getText());
                    }
                }.start();
            }
        });
        quitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){
                clientFrame.dispose();
            }
        });
 
        //Display the window
        clientFrame.setTitle("UDP Client Side");
        clientFrame.pack();
        clientFrame.setLocationRelativeTo(null);
        clientFrame.setVisible(true);
    }

	private int AcceptTCPClient(String serverAddressStr, String ServerPortStr, String login) {
		
		//Validate arguments
		if (serverAddressStr == null) {
			dumpLocalLog("Error, missing server address, exiting");
			return 1;
		}
		dumpLocalLog("Server address exists");
		
		if (ServerPortStr == null) {
			dumpLocalLog("Error, missing port, exiting");
			return 1;
		}
		dumpLocalLog("Server port exists");
		
		if (login == null) {
			dumpLocalLog("Error, missing login, exiting");
			return 1;
		}
		dumpLocalLog("Login exists");
		
		//Convert port number to integer
		int ServerPort;
		try {
			ServerPort = Integer.parseInt(ServerPortStr);
		} catch (NumberFormatException e) {
			dumpLocalLog("Error, illegal port, exiting");
			return 1;
		}
		dumpLocalLog("Server port is correct");
				
		//Validate port number range
		if (ServerPort < 1025) {
			dumpLocalLog("Error, port number below 1024, not permitted, exiting");
			return 1;
		}
		dumpLocalLog("Server port successfully validated");

		//Create connection with server
		Socket mainSocket;
		try {
			mainSocket = new Socket(serverAddressStr, ServerPort);
		} catch (IOException e3) {
			dumpLocalLog("Error, could not connect to server, exiting");
			return 1;
		}
		dumpLocalLog("Connected to " + mainSocket.getRemoteSocketAddress());

		//Open i/o streams
		InputStream inStream = null;
		OutputStream outStream = null;
		try {
			inStream = mainSocket.getInputStream();
		} catch (IOException e2) {
			dumpTCPLog(mainSocket, "Error, could not open input stream, exiting", 0);
			closeTCPSocket(mainSocket, 0);
			return 1;
		}
		try {
			outStream = mainSocket.getOutputStream();
		} catch (IOException e2) {
			dumpTCPLog(mainSocket, "Error, could not open output stream, exiting", 0);
			closeTCPSocket(mainSocket, 0);
			return 1;
		}
		dumpTCPLog(mainSocket, "I/O streams opened", 0);
		
		byte[] mainData = new byte[PACKET_SIZE];
				
		//Read AKN from server
		if ((mainData = receiveTCPPacket(mainSocket, inStream, 0)) == null) return 1;
		
		//Send message to GUI
		TCPextraFld.setText(new String(mainData));
		
		//Validate ANK signal
		if (!new String(mainData).substring(0, AKN_SIG.length()).equals(AKN_SIG)) {
			dumpTCPLog(mainSocket, "AKN signal was NOT validated, exiting", 0);
			closeTCPSocket(mainSocket, 0);
			return 1;
		}
		dumpTCPLog(mainSocket, "AKN signal validated", 0);
		
		//Send message to server
		if (sendTCPPacket(mainSocket, outStream, login.getBytes(), 0) != 0) return 1;
		
		//Write message to GUI
		TCPoutputFld.setText(login);
				
		//Read mark
		if ((mainData = receiveTCPPacket(mainSocket, inStream, 0)) == null) return 1;
		
		//Write message to GUI
		TCPextraFld.setText(new String(mainData));
		
		//Write message
		if (sendTCPPacket(mainSocket, outStream, AKN_CLS.getBytes(), 0) != 0) return 1;

		//Close socket
		closeTCPSocket(mainSocket, 0);
		
		return 0;
	}

	public void createTCPUI() {
        //Create and set up the frame
        JFrame clientFrame = new JFrame();
        clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        clientFrame.setLayout(new GridLayout(6, 2, 5, 5));
        
        //Create elements
        JButton submitBtn = new JButton("Submit");
        JButton quitBtn = new JButton("Quit");
        JTextField serverFld = new JTextField("localhost");
        JTextField portFld = new JTextField(DEFAULT_TCP_PORT + "");
        JTextField inputFld = new JTextField("W1234567");
        
        //Add elements to frame
        clientFrame.getContentPane().add(new JLabel("Server IP: "));
        clientFrame.getContentPane().add(serverFld);
        clientFrame.getContentPane().add(new JLabel("Server port: "));
        clientFrame.getContentPane().add(portFld);
        clientFrame.getContentPane().add(new JLabel("Student ID: "));
        clientFrame.getContentPane().add(inputFld);
        clientFrame.getContentPane().add(submitBtn);
        clientFrame.getContentPane().add(quitBtn);
        clientFrame.getContentPane().add(new JLabel("Last sent message: "));
        clientFrame.getContentPane().add(TCPoutputFld);
        clientFrame.getContentPane().add(new JLabel("Last received message: "));
        clientFrame.getContentPane().add(TCPextraFld);
        
        //Action listeners
        submitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){
                new Thread() {
                    public void run() {
                    	AcceptTCPClient(serverFld.getText(), portFld.getText(), inputFld.getText());
                    }
                }.start();
            }
        });
        quitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae){
                clientFrame.dispose();
            }
        });
 
        //Display the window
        clientFrame.setTitle("TCP Client Side");
        clientFrame.pack();
        clientFrame.setLocationRelativeTo(null);
        clientFrame.setVisible(true);
    }
}
