//John Carbeck
//16309095

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.lang.Integer;
import java.nio.ByteBuffer;

import tcdIO.*;

/**
 *
 * Client class
 * 
 * An instance accepts user input 
 *
 */
public class Client extends Node {
	
	Terminal terminal;
	InetSocketAddress dstAddress;
	InetSocketAddress routerAddress;
	
	/**
	 * Constructor
	 * 	 
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	Client(Terminal terminal, int port) {
		try {
			this.terminal= terminal;
			dstAddress= new InetSocketAddress(Constants.DEFAULT_NODE, Constants.SERVER);
			routerAddress = new InetSocketAddress(Constants.DEFAULT_NODE, port);
			socket = new DatagramSocket[1];
			socket[0] = new DatagramSocket(Constants.CLIENT);
			socket[0].connect(routerAddress);
			listen(socket);
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	
	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		StringContent content = new StringContent(packet);
		this.notify();
		terminal.println(content.toString());
	}

	
	/**
	 * Sender Method
	 * 
	 */
	public synchronized void start() throws Exception{
		
		while(true)
		{
			createPacket();
			this.wait();
		}
	}


	public void createPacket()  
	{
		try
		{
			DatagramPacket packet= null;

			byte[] payload= null;
			byte[] header= null;
			byte[] buffer= null;
			
			payload= (terminal.readString("String to send: ")).getBytes();

			header= new byte[PacketContent.HEADERLENGTH];
			ByteBuffer tempbuffer = ByteBuffer.wrap(header);

			byte[] sourceAddress = addressToByte(
				(new InetSocketAddress(Constants.DEFAULT_NODE, Constants.CLIENT)).getAddress());
			byte[] destinationAddress = addressToByte(
				(new InetSocketAddress(Constants.DEFAULT_NODE, Constants.SERVER)).getAddress());
			int sourePort =	Constants.CLIENT;
			int destinationPort = Constants.SERVER;

			for (int i = 0; i < sourceAddress.length; i++)
				tempbuffer.put(Constants.SOURCE_INDEX + i, sourceAddress[i]);
			for(int i = 0; i < destinationAddress.length; i++)
				tempbuffer.put(Constants.DEST_INDEX + i, destinationAddress[i]);
			tempbuffer.putInt(Constants.SOURCE_PORT_INDEX, sourePort);
			tempbuffer.putInt(Constants.DEST_PORT_INDEX, destinationPort);

			buffer = new byte[header.length + payload.length];
			System.arraycopy(header, 0, buffer, 0, header.length);
			System.arraycopy(payload, 0, buffer, header.length, payload.length);
				
			terminal.println("Sending packet...");
			packet = new DatagramPacket(buffer, buffer.length, routerAddress);
			socket[0].send(packet);
			terminal.println("Packet sent");
		} catch(java.lang.Exception e) {e.printStackTrace();}
		
	}

	/**
	 * Test method
	 * 
	 * Sends a packet to a given address
	 */
	public static void main(String[] args) {
		if(args.length == 1)
		{
			int port = Integer.valueOf(args[0]);
			try 
			{					
				Terminal terminal= new Terminal("Client");		
				(new Client(terminal, port)).start();
				terminal.println("Program completed");
			} catch(java.lang.Exception e) {e.printStackTrace();}
		}
	}
}