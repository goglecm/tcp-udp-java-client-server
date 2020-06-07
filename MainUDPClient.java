package mytcp.mainpack;

import javax.swing.SwingUtilities;

public class MainUDPClient {
	public static void main(String[] args) {
		Client mainClient = new Client();
		//Display GUI in separate thread
	    SwingUtilities.invokeLater(new Runnable() {
	    	public void run() {
	        	mainClient.createUDPUI();
	        }
	    });
	    
	}
}
