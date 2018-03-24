//John Carbeck
//16309095

import java.net.DatagramPacket;

public interface PacketContent {
	
	public static byte HEADERLENGTH = Constants.HEADERLENGTH;
	
	public String toString();
	public DatagramPacket toDatagramPacket();
}