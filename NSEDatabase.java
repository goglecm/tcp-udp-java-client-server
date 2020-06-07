package mytcp.mainpack;
import java.io.*;
import java.util.Arrays;

public class NSEDatabase {
	public static final int RECORD_SIZE = NSEProtocol.PACKET_SIZE;
	private File f = null;
	private String fileName;
	private int totalRecords;
	
	//Constructors
	public NSEDatabase(String filename) {
		if (filename != null) {
			fileName = filename;
			totalRecords = 0;
		}
	}
	
	public void dumpDBLog(String msg) {
		if (msg != null)
			if (f == null)
				System.out.println("DB(-):\t" + msg);
			else if (!f.exists()) {
				System.out.println("DB(-):\t" + msg);
			} else {
				System.out.println("DB(" + fileName + "):\t" + msg);
			}
	}
	
	public int getTotalRecords() {
		return totalRecords;
	}
	
	public int searchPlainRecord(byte[] s) {
		
		if (s == null) {
			dumpDBLog("Error, record to search is missing");
			return -1;
		}
		
		int t, z, index;
		byte [] data;
		
		t = 0;
		while (s[t] != '\0') t++;
		
		data = new byte[t];
		for (z = 0; z < t; z++) {
			data[z] = s[z];
		}
		
		if ((s = NSEEncryption.encryptMessage(data)) == null ) return -1;

		data = new byte[RECORD_SIZE];
    	for (t = 0; t < s.length; t++) 
    		data[t] = s[t];
    	while (t < RECORD_SIZE) {
    		data[t] = '\0';
    		t++;
    	}
    	
    	if ((index = searchRecord(data)) == -1) return -1;

		return index;
	}
	
	//Searches for a user name and retrieves its ID
	public int searchRecord(byte[] s) {
		
		if (s == null) {
			dumpDBLog("Error, record to search is missing");
			return -1;
		}
		
		int i = 0;
		byte [] data;
		
		if (f == null) {
			dumpDBLog("Error, file never created");
			return -1;
		}
		
		if (!f.exists()) {
			dumpDBLog("Error, file does not exist");
			return -1;
		}
		
		while (i < totalRecords){
			if ((data = getRecord(i)) == null) return -1;
			
			if (Arrays.equals(data, s)) break;
			else i += 2;
		}
		if (i == totalRecords) {
			dumpDBLog("Error, record not found");
			return -1;
		}
		
		dumpDBLog("Record found at: " + i);
		return i;
	}
	
	public int addRecord(byte[] username, byte[] mark) {
		
		if (username == null) {
			dumpDBLog("Error, username to add is missing");
			return -1;
		}
		if (mark == null) {
			dumpDBLog("Error, mark to add is missing");
			return -1;
		}
		
		FileOutputStream foutStream;
		
		if (f == null) {
			dumpDBLog("Error, file never created");
			return 1;
		}
		
		if (!f.exists()) {
			dumpDBLog("Error, file does not exist");
			return 1;
		}
		
		try  {
			//Open file for appending data
			foutStream = new FileOutputStream(f.getAbsolutePath(), true);
		}catch  (FileNotFoundException e){
			dumpDBLog("Error, file not found");
			return 1;
		}
		
		try {
			try {
				int i;
				foutStream.write(username);
				i = username.length;
				while (i < RECORD_SIZE) {
					foutStream.write('\0');
					i++;
				}
				foutStream.write(mark);
				i = mark.length;
				while (i < RECORD_SIZE) {
					foutStream.write('\0');
					i++;
				}
			} catch (IOException e) {
				dumpDBLog("Error, could not write to file");
				try {
					foutStream.close();
				} catch (IOException e1){
					dumpDBLog("Error, could not close file");
					return 1;
				}
				return 1;
			}
		} finally {
			try {
				foutStream.close();
			} catch (IOException e1){
				dumpDBLog("Error, could not close file");
				return 1;
			}
		}
		
		totalRecords += 2;
		
		dumpDBLog("Records have been added successfully");
		return 0;
	}
	
	public int flushRecords() {
		if (f == null) {
			dumpDBLog("Error, file never created");
			return 1;
		}
		
		if (!f.exists()) {
			dumpDBLog("Error, file does not exist");
			return 1;
		}

		f.delete();
		
		if (createDBFile() != 0) {
			dumpDBLog("Error, file could not be erased");
		}
		dumpDBLog("File flushed successfully");
		return 0;
	}
	
	public byte[] getPlainRecord(int index) {
		
		if (index < 0) {
			dumpDBLog("Error, invalid index");
			return null;
		}
		
		byte[] data;
		if ((data = getRecord(index)) == null) return null;
    	if ((data = NSEEncryption.decryptMessage(data)) == null ) return null;
    	return data;
	}
	
	public byte[] getRecord(int index) {
		
		if (index < 0) {
			dumpDBLog("Error, invalid index");
			return null;
		}
		
		byte[] record = new byte[RECORD_SIZE];
		FileInputStream finStream;
		
		if (totalRecords < 1) {
			dumpDBLog("Error, empty database");
			return null;
		}
		
		if (index >= totalRecords) {
			dumpDBLog("Error, index too high");
			return null;
		}
		
		if (f == null) {
			dumpDBLog("Error, file never created");
			return null;
		}
		
		if (!f.exists()) {
			dumpDBLog("Error, file does not exist");
			return null;
		}
		
		try {
			finStream = new FileInputStream(f.getAbsolutePath());
		}catch  (FileNotFoundException e){
			dumpDBLog("Error, file not found");
			return null;
		}
		
		try  {
			try {
				int j = 0;
				do {
					if (finStream.read(record, 0, RECORD_SIZE) != RECORD_SIZE) {
						dumpDBLog("Error, could not fully read record");
						return null;
					}
					j++;
				} while (j <= index);
			} catch (IOException e) {
				dumpDBLog("Error, could not read record");
				return null;
			}
		} finally {
			try {
				finStream.close();
			} catch (IOException e1) {
				dumpDBLog("Error, could not close file");
				return null;
			}
		}
		
		return record;
	}
	
	public int createDBFile() {
		f = new File(fileName + ".nsedb");
		try {
			f.createNewFile();
		} catch (IOException e) {
			dumpDBLog("Error, file could not be created");
			return 1;
		}
		dumpDBLog("File created: " + fileName);
		return 0;
	}
}
