
import java.util.ArrayList;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import tcdIO.Terminal;

public class Server extends Node 
{
	static final int DEFAULT_PORT = 50200;

	boolean finished = false;
	Terminal terminal;
	ArrayList<Device> serverDevices = new ArrayList<Device>();
	
	/**
	 *Server
	 *	a constructor that initalizes and creates a Server node
	 *	@param terminal a terminal for the server to exist
	 *	@param port the port of the server
	 */
	Server(Terminal terminal, int port) 
	{
		try {
			this.terminal= terminal;
			socket = new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 *onReceipt
	 *	a function that is run when the listener from the node recieves a packet
	 *	@param packet a packet that was detected from the listener
	 */
	public synchronized void onReceipt(DatagramPacket packet) 
	{
		try 
		{
			if(isValidPacket(packet))
			{
				StringContent message = new StringContent(packet);
				terminal.println("    '" + message.toString() + "'");
				repackage(packet, Constants.NO_ERROR_INT);
				socket.send(packet);
			}
			else
			{
				terminal.println("    Invalid packet ID");
				repackage(packet, Constants.ERROR_INT);
				socket.send(packet);
			}

			this.notify();
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	/**
	 *start
	 *	a function that is run after the construction of the server
	 */
	public synchronized void start() throws Exception 
	{
		while(!finished)
		{
			terminal.println("Waiting for contact");
			this.wait();
		}
	}

	/**
	 *isValidPacket
	 *	a function that returns if a specified packet is valid
	 *	@param packet a DatagramPacket to be checked
	 *	@return isValidPacket
	 */
	private boolean isValidPacket(DatagramPacket packet)
	{
		boolean isValidPacket = false;

		byte[] destinationByteArray = new byte[Constants.HEADERLENGTH];
		byte[] packetArray = packet.getData();
		
		System.arraycopy(packetArray, 0, destinationByteArray, 
			0, destinationByteArray.length);
		ByteBuffer tempBuffer = ByteBuffer.wrap(destinationByteArray);

		int sourceID = tempBuffer.getInt(Constants.HEADER_SOURCE_INDEX);
		terminal.println("    Source: " + sourceID);
		int packetID = tempBuffer.getInt(Constants.HEADER_PACKET_ID_INDEX);

		if(sourceID >= Constants.MIN_CLIENT_ID && 
			sourceID <= Constants.MAX_CLIENT_ID)
		{
			isValidPacket = true;
			if(!isDeviceInList(sourceID))
			{
				serverDevices.add(new Device(packetID, sourceID));
			}
			else
			{
				if(isDeviceInList(sourceID))
				{
					Device device = getDevice(sourceID);
					if(packetID == (device.getPacketID() + 1))
					{
						isValidPacket = true;
						device.setPacketID(packetID);
					}
				}
			}
		}	
		
		return isValidPacket;
	}

	/**
	 *repackage
	 *	a function that takes in a packet and a error int and adjusts the 
	 *	header of the packet to show the correct values
	 *	@param packet a packet to be repackaged
	 *	@param error a value of the error that occured, is 0 when no error occured
	 *	@return repackedpacket a packet that has the updated header
	 */
	private DatagramPacket repackage(DatagramPacket packet, int error)
	{
		DatagramPacket repackagedPacket = packet;

		byte[] packetData = packet.getData();
		ByteBuffer tempBuffer = ByteBuffer.wrap(packetData);
		int server = tempBuffer.getInt(Constants.HEADER_DEST_INDEX);
		int client = tempBuffer.getInt(Constants.HEADER_SOURCE_INDEX);
		int packetID = tempBuffer.getInt(Constants.HEADER_PACKET_ID_INDEX);

		byte[] header = new byte[Constants.HEADERLENGTH];
		tempBuffer = ByteBuffer.wrap(header);  

		terminal.println("    RecievedID: " + packetID + 
			"\n    SentID: " + (packetID + 1));
			
		tempBuffer.putInt(Constants.HEADER_PACKET_ID_INDEX, ++packetID);
		tempBuffer.putInt(Constants.HEADER_SOURCE_INDEX, server);
		tempBuffer.putInt(Constants.HEADER_DEST_INDEX, client);
		tempBuffer.putInt(Constants.HEADER_ERROR_INDEX, error);

		header = tempBuffer.array();
		byte[] payload = new byte[0];//Constants.SUCCESS_ACK;
		byte[] buffer = new byte[header.length + payload.length];
		System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(payload, 0, buffer, header.length, payload.length);

		repackagedPacket.setData(buffer);

		return repackagedPacket;
	}

	/**
	 *timeout
	 */
	public void timeout()
	{}

	/**
	 *getDevice
	 *	a function that uses a sourceID to find a specific device in a list
	 *	of devices that is contained in the server
	 *	@param sourceID the sourceID of the desired Device
	 *	@return deviceToReturn the device that is specified by the sourceID, note
	 *	returns null if the device isn't in the list
	 */
	public Device getDevice(int sourceID)
	{
		Device deviceToReturn = null;
		for(int i = 0; i < serverDevices.size(); i ++)
		{
			Device temp = serverDevices.get(i);
			if(temp.getSourceID() == sourceID) deviceToReturn = temp;
		}
		return deviceToReturn;
	}

	/**
	 *isDeviceInList
	 *	a function that takes a sourceID and returns if it is in a list
	 *	@param sourceID
	 *	@return isDeviceInList
	 */
	public boolean isDeviceInList(int sourceID)
	{
		boolean isDeviceInList = false;
		for(int i = 0; i < serverDevices.size(); i ++)
		{
			Device temp = serverDevices.get(i);
			if(temp.getSourceID() == sourceID) isDeviceInList = true;
		}
		return isDeviceInList;
	}	

	public static void main(String[] args) 
	{
		try {					
			Terminal terminal= new Terminal("Server");
			(new Server(terminal, DEFAULT_PORT)).start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}