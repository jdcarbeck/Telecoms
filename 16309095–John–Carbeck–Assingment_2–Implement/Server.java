//John Carbeck
//16309095

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.lang.Integer;
import java.nio.ByteBuffer;

import tcdIO.Terminal;

public class Server extends Node {

	Terminal terminal;
	InetSocketAddress dstAddress;
	InetSocketAddress routerAddress;
	/*
	 * 
	 */
	Server(Terminal terminal, int port) {
		try {
			this.terminal= terminal;
			routerAddress = new InetSocketAddress(Constants.DEFAULT_NODE, port);
			dstAddress = new InetSocketAddress(Constants.DEFAULT_NODE, Constants.CLIENT);
			connectedAddresses = new InetSocketAddress[1];
			connectedAddresses[0] = new InetSocketAddress(Constants.DEFAULT_NODE, port);
			socket = new DatagramSocket[1];
			socket[0] = new DatagramSocket(Constants.SERVER);
			listen(socket);
			
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public void onReceipt(DatagramPacket packet) {
		try {
			StringContent content= new StringContent(packet);

			terminal.println(content.toString());

			createPacket();
		}
		catch(Exception e) {e.printStackTrace();}
	}

	public void createPacket()  
	{
		try
		{
			DatagramPacket packet= null;

			byte[] payload= null;
			byte[] header= null;
			byte[] buffer= null;
			
			payload= ("OK").getBytes();

			header= new byte[PacketContent.HEADERLENGTH];
			ByteBuffer tempbuffer = ByteBuffer.wrap(header);

			byte[] sourceAddress = addressToByte(
				(new InetSocketAddress(Constants.DEFAULT_NODE, Constants.SERVER)).getAddress());
			byte[] destinationAddress = addressToByte(
				(new InetSocketAddress(Constants.DEFAULT_NODE, Constants.CLIENT)).getAddress());
			int sourePort =	Constants.SERVER;
			int destinationPort = Constants.CLIENT;

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

	
	public synchronized void start() throws Exception {
		terminal.println("Waiting for contact");
		this.wait();
	}
	
	/*
	 * 
	 */
	public static void main(String[] args) {
		if(args.length == 1)
		{
			int port = Integer.valueOf(args[0]);
			try 
			{					
				Terminal terminal= new Terminal("Server");
				(new Server(terminal, port)).start();
				terminal.println("Program completed");
			} catch(java.lang.Exception e) {e.printStackTrace();}	
		}
	}
}