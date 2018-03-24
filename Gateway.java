
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.io.IOException;

import java.util.Arrays;

import tcdIO.Terminal;
import tcdIO.*;

public class Gateway extends Node
{
	static final int DEFAULT_SRC_PORT = 50100;
	static final int DEFAULT_DST_PORT = 50200;
	static final String DEFAULT_DST_NODE = "localhost";	
	static final String DEFAULT_SRC_NODE = "localhost";

	boolean finished = false;
	Terminal terminal;

	ArrayList<Device> gatewayDevices = new ArrayList<Device>();

	/**
	 *Gateway
	 *	a constructor that generates a gateway from a terminal and a
	 *	source port
	 *	@param terminal a terminal to house the gateway
	 *	@param srcPort the port of the gateway
	 */
	Gateway(Terminal terminal, int srcPort) 
	{
		try 
		{
			this.terminal = terminal;
			socket = new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 *onReceipt
	 *	a function that is activated when the listen discovers a packet
	 *	@param packet a DatagramPacket that was found by the listener
	 */
	public synchronized void onReceipt(DatagramPacket packet) 
	{
		try
		{
			if(checkPacket(packet))
			{
				DatagramPacket newPacket = repackage(packet);
				socket.send(newPacket);
				
				terminal.println("    Packet Sent");
			}
			
			this.notify();	
		}catch(java.lang.Exception e) {e.printStackTrace();}
		
	}

	/**
	 *start
	 *	a function that begins after the gateway has been constructed
	 */
	public synchronized void start() throws Exception {
		while(!finished)
		{
			terminal.println("Waiting for contact");
			this.wait();
		}
	}

	/**
	 *repackage
	 *	a function that takes in a current packet and will adjust the values
	 *	in the header to the appropriate new values
	 *	@param packet a packet that needs its header adjusted
	 *	@return repackagedPacket a DatagramPacket that contains the correct
	 *	new values
	 */
	private DatagramPacket repackage(DatagramPacket packet)
	{
		DatagramPacket repackagedPacket = packet;
		byte[] header = new byte[Constants.HEADERLENGTH];
		byte[] packetArray = repackagedPacket.getData();

		System.arraycopy(packetArray, 0, header, 0, header.length);
		ByteBuffer tempBuffer = ByteBuffer.wrap(header);

		int destID = tempBuffer.getInt(Constants.HEADER_DEST_INDEX);
		int sourceID = tempBuffer.getInt(Constants.HEADER_SOURCE_INDEX);

		if(destID >= Constants.MIN_SERVER_ID && destID <= Constants.MAX_SERVER_ID)
		{
			terminal.println("    Source: " + sourceID + " Dest: " + destID);
			repackagedPacket.setPort(DEFAULT_DST_PORT);
		}
		else
		{ 
			terminal.println("    Source: " + sourceID + " Dest: " + destID);
			Device device = getDevice(destID);
			tempBuffer = ByteBuffer.wrap(packetArray);
			repackagedPacket.setPort(device.getPort());
		}

		return repackagedPacket;
	}

	/**
	 *checkPacket
	 *	a function that takes in a packet and checks to make sure that is valid
	 *	@param packet a packet that is to be inspected
	 *	@return isValidPacket
	 */
	private boolean checkPacket(DatagramPacket packet)
	{
		boolean isValidPacket = false;

		byte[] destinationByteArray = new byte[Constants.HEADERLENGTH];
		byte[] packetArray = packet.getData();
		System.arraycopy(packetArray, 0, destinationByteArray, 
			0, destinationByteArray.length);
		ByteBuffer tempBuffer = ByteBuffer.wrap(destinationByteArray);
			
		int sourceID = tempBuffer.getInt(Constants.HEADER_SOURCE_INDEX);
		int destID = tempBuffer.getInt(Constants.HEADER_DEST_INDEX);
		int packetID = tempBuffer.getInt(Constants.HEADER_PACKET_ID_INDEX);
		int port = packet.getPort();
		
		if(((sourceID >= Constants.MIN_CLIENT_ID) && (sourceID <= Constants.MAX_CLIENT_ID)) 
			&& ((destID >= Constants.MIN_SERVER_ID) && (destID <= Constants.MAX_SERVER_ID)))
		{
			if(gatewayDevices.size() <= 0 || !isDeviceInList(port, sourceID))
			{
				InetAddress address = packet.getAddress();
				// int uniqueID = generateUniqueID();
				gatewayDevices.add(new Device(packetID, sourceID, destID, address, port));
			
				isValidPacket = true;
			}
			else if(isDeviceInList(port, sourceID))
			{
				isValidPacket = true;
			}		
		}
		else if(isDeviceInList(destID))
		{
			isValidPacket = true;
		}

		return isValidPacket;
	}

	/**
	 *timeout
	 */
	public void timeout()
	{}

	/**
	 *isDeviceInList
	 *	a funtion that looks only at the sourceID to determin if it is
	 *	a list of devices at the gateway
	 *	@param sourceID the id used to find the Device in the list
	 *	@return isDeviceInList
	 */
	public boolean isDeviceInList(int sourceID)
	{
		boolean isDeviceInList = false;

		for(int i = 0; i < gatewayDevices.size(); i++)
		{
			Device temp = gatewayDevices.get(i);
			if(sourceID == temp.getSourceID()) isDeviceInList = true;
		}
		return isDeviceInList;
	}

	/**
	 *isDeviceInList
	 *	a function that determines if a element exsists within a list
	 *	by using the port and sourceID of the desired element
	 *	@param port the port of the desired Device
	 *	@param sourceID the sourceID of the desired Device
	 *	@return isDeviceInList
	 */
	public boolean isDeviceInList(int port, int sourceID)
	{
		boolean isDeviceInList = false;

		for(int i = 0; i < gatewayDevices.size(); i++)
		{
			Device temp = gatewayDevices.get(i);
			if(port == temp.getPort() && sourceID == temp.getSourceID()) 
			{
				isDeviceInList = true;
			}
		}
		return isDeviceInList;
	}

	/**
	 *getDevice
	 *	a function that uses a sourceID and returns that element from a list
	 *	if it exisits
	 *	@param sourceID the sourceID of the desired element in the list
	 *	@return returnDevice a device that has the specified sourceID from a list
	 *	of devices, note the return value is null if the Device isnt in the list
	 */
	public Device getDevice(int sourceID)
	{
		Device returnDevice = null;
		for(int i = 0; i < gatewayDevices.size(); i++)
		{
			Device temp = gatewayDevices.get(i);
			if(sourceID == temp.getSourceID()) returnDevice = temp;
		}
		return returnDevice;
	}

	/**
	 *getDeviceAtPort
	 *	a function that uses a port and returns that element from a list
	 *	if it exisits
	 *	@param port the port of the desired element in the list
	 *	@return returnDevice a device that has the specified sourceID from a list
	 *	of devices, note the return value is null if the Device isnt in the list
	 */
	public Device getDeivceAtPort(int port)
	{
		Device returnDevice = null;
		for(int i = 0; i < gatewayDevices.size(); i++)
		{
			Device temp = gatewayDevices.get(i);
			if(port == temp.getPort()) returnDevice = temp;
		}
		return returnDevice;
	}

	public static void main(String[] args) {
		try {					
			Terminal terminal= new Terminal("Gateway");
			(new Gateway(terminal, DEFAULT_SRC_PORT)).start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}