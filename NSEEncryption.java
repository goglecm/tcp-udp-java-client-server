package mytcp.mainpack;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;


public class NSEEncryption{
	
	private static final int MESSAGE_LENGTH = NSEProtocol.PACKET_SIZE;
    private static final String ALGO = "AES";
    private static final byte[] keyValue = new byte[] { 'T', 'h', 'e', 'B', 'e', 's', 't',
    		'S', 'e', 'c', 'r','e', 't', 'K', 'e', 'y' };
    

	private static void dumpECLog(String msg) {
		if (msg != null) System.out.println("Encryption:\t\t" + msg);
	}
    
    private static Key generateKey() throws Exception {
        Key key = new SecretKeySpec(keyValue, ALGO);
        return key;
    }

	public static byte[] encryptMessage(byte[] message){
		
		if (message == null) {
			dumpECLog("Error, message doesn't exist, exiting");
			return null;
		}
		
		Key key = null;
		byte[] encVal = null;
        Cipher c = null;
        int j;
        byte[] mainMessage = new byte[MESSAGE_LENGTH - 1];
        
        //Adjust size
        for (j = 0; (j < message.length) && (j < mainMessage.length); j++) {
			mainMessage[j] = message[j];
		}
		while (j < mainMessage.length) {
			mainMessage[j] = '\0';
			j++;
		}
        
		try {
			key = generateKey();
		} catch (Exception e) {
			dumpECLog("Error, could not generate encryption key, exiting");
			return null;
		}
		try {
			c = Cipher.getInstance(ALGO);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			dumpECLog("Error, could not get cipher, exiting");
			return null;
		}
        try {
			c.init(Cipher.ENCRYPT_MODE, key);
		} catch (InvalidKeyException e) {
			dumpECLog("Error, could not initialise cipher, exiting");
			return null;
		}
		try {
			encVal = c.doFinal(mainMessage);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			dumpECLog("Error, could not encrypt message, exiting");
			return null;
		}
        return encVal;
	}
	
	public static byte[] decryptMessage(byte[] message) {
		
		if (message == null) {
			dumpECLog("Error, message doesn't exist, exiting");
			return null;
		}
		
		if (message.length != MESSAGE_LENGTH) {
			dumpECLog("Error, wrong message size");
			return null;
		}
		
		Cipher c = null;
		Key key = null;
		byte[] decValue = null;
		
		try {
			key = generateKey();
		} catch (Exception e) {
			dumpECLog("Error, could not generate encryption key, exiting");
			return null;
		}
		try {
			c = Cipher.getInstance(ALGO);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e1) {
			dumpECLog("Error, could not get cipher, exiting");
			return null;
		}
        try {
			c.init(Cipher.DECRYPT_MODE, key);
		} catch (InvalidKeyException e1) {
			dumpECLog("Error, could not initialise cipher, exiting");
			return null;
		}
		try {
			decValue = c.doFinal(message);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			dumpECLog("Error, could not decrypt message, exiting");
			return null;
		}
        return decValue;
	}
}

