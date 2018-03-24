import java.net.DatagramPacket;

public interface PacketContent 
{
	public static byte HEADERLENGTH = 12;
	
	public String toString();
	public DatagramPacket toDatagramPacket();
}