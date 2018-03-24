//John Carbeck
//16309095

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public abstract class Node {
	static final int PACKETSIZE = 65536;

	DatagramSocket[] socket;
	InetSocketAddress[] connectedAddresses;
	Listener listener;
	CountDownLatch latch;
	
	Node() 
	{	
	
	}
	
	public void listen(DatagramSocket[] socketArray)
	{
		this.socket = socketArray;
		for(int i = 0; i < socket.length; i++)
		{
			latch = new CountDownLatch(1);
			listener = new Listener(socket[i]);
			listener.setDaemon(true);
			listener.start();
			listener.go();
		}
	}

	public abstract void onReceipt(DatagramPacket packet);
	

	public byte[] addressToByte(InetAddress address)
	{
		String addressString = address.toString();
		String[] addressElements = addressString.split("/");
		String ipAddress = addressElements[1];
		String[] ipElements = ipAddress.split("\\.");
		byte[] ipData = new byte[ipElements.length];
	
		for(int i = 0; i < ipElements.length; i++)
		{
			byte numberAsByte =(byte)(Integer.parseInt(ipElements[i]));
			ipData[i] = numberAsByte;
		}
		return ipData;
	}

	public InetAddress byteToAddress(byte[] address)
	{
		String addressString = "";
		int currentValue;
		for(int i = 0; i < address.length; i++)
		{
			addressString += Integer.toString((int)address[i]);
			if(i != (address.length - 1)) addressString += ".";
		}
		try
		{
			return InetAddress.getByName(addressString);
		}
		catch (java.lang.Exception e) {return null;}
	}

	public int findSocketIndex(InetSocketAddress socketAddress)
	{
		int port = socketAddress.getPort();
		for(int i = 0; i < connectedAddresses.length; i++)
		{
			if(port == connectedAddresses[i].getPort())
				return i;
		}
		return -1;
	}


	/**
	 *
	 * Listener thread
	 * 
	 * Listens for incoming packets on a datagram socket and informs registered receivers about incoming packets.
	 */
	class Listener extends Thread {
		
		DatagramSocket socketToListenTo;

		Listener(DatagramSocket socketToListenTo)
		{
			this.socketToListenTo = socketToListenTo;
		}
		/*
		 *  Telling the listener that the socket has been initialized 
		 */

		public void go() {
			latch.countDown();
		}
		
		/*
		 * Listen for incoming packets and inform receivers
		 */
		public void run() {
			try {
				latch.await();
				// Endless loop: attempt to receive packet, notify receivers, etc
				while(true) 
				{
					DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
					socketToListenTo.receive(packet);
					onReceipt(packet);
				}
			} catch (Exception e) {if (!(e instanceof SocketException)) e.printStackTrace();}
		}
	}
}