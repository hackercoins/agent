package creators;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import core.Logger;
import storage.Store;

/*
 * This class creates a new address by first generating a EC key pair and computing the 
 * address as sha256 over the public key. Encryption of the wallet is executed by the DataManager.
 */
public class CreateAddress {

	public String create(Store store, Logger log) {
		
		try {

			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
				
			ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256r1");
				
			keyGen.initialize(ecGenParameterSpec, SecureRandom.getInstance("SHA1PRNG", "SUN"));
			KeyPair pair = keyGen.generateKeyPair();

			PrivateKey priv = pair.getPrivate(); 
			PublicKey pub = pair.getPublic();
				
			PKCS8EncodedKeySpec prKeySpec = new PKCS8EncodedKeySpec(priv.getEncoded());
			byte[] priv_Bytes = prKeySpec.getEncoded();
			String priv_String = bytesToHex(priv_Bytes);
			X509EncodedKeySpec puKeySpec = new X509EncodedKeySpec(pub.getEncoded());
			byte[] pub_Bytes = puKeySpec.getEncoded();
			String pub_String = bytesToHex(pub_Bytes);

			String address = "HC";
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			address += bytesToHex(digest.digest((pub_String).getBytes()));
			
			while(!store.wallet.claim()) { Thread.sleep(10);}
			store.wallet.addToWallet(address + "#" + pub_String + "#" + priv_String);
			store.wallet.release();
			
			log.addEntry("CreateAdress>create()",  "Successfully created the new address " + address + ".");
			return address;
		        
		} catch (Exception e) {e.printStackTrace();}
		
		log.addEntry("CreateAdress>create()", "Creator>createAddress(): Could not create a new address.");
		return null;
	}
	
	// Converts a byte array into a hexadecimal string.
	private String bytesToHex(byte[] bytes) {
		
		char[] hexArray = "0123456789ABCDEF".toCharArray();
	    char[] hexChars = new char[bytes.length * 2];
	    
	    for ( int j = 0; j < bytes.length; j++ ) {
	    	
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}
