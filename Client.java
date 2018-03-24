
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.lang.Object;
import java.io.IOException;

import java.util.Arrays;

import tcdIO.*;

/**
 *
 * Client class
 * 
 * An instance accepts user input 
 *
 */
public class Client extends Node 
{
	static final int DEFAULT_SRC_PORT = 50000;
	static final int DEFAULT_GTW_PORT = 50100;
	static final String DEFAULT_GTW_NODE = "localhost";
	
	boolean finished = false;
	boolean connectionMade = true;
	Terminal terminal;
	InetSocketAddress dstAddress;
	DatagramPacket lastSentPacket;
		
	int packetID;
	int sourceID;
	int destID;
	int srcPort;
	
	/**
	 *Constructor
	 * 	Creates a client with a randomly generated id and given a 
	 * 	port that is free.	 
	 *	@param terminal a terminal for the client to exsist in
	 *	@param dstHost a string that represents the host of the gateway
	 *	@param dstport a port id for the gateway
	 */
	Client(Terminal terminal, String dstHost, int dstPort) {
		try {
			this.terminal = terminal;
			this.packetID = Constants.DEFAULT_PACKET_ID;
			this.destID = Constants.DEFAULT_SERVER_ID;
			this.sourceID = generateID();
			terminal.println("(SOURCE ID: " + sourceID + ")");
			this.dstAddress = new InetSocketAddress(dstHost, dstPort);
			int srcport = findPort(Constants.MIN_CLIENT_PORT, 
				Constants.MAX_CLIENT_PORT);
			terminal.println("(SOURCE PORT: " + srcport+ ")");
			socket =  new DatagramSocket(srcport);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 *OnReceipt
	 *	a function that is executed when the lister recieves a packet.
	 *	that packet is checked to be valid and then the current packet
	 *	id is changed and the start() funtion is notified
	 *	@param packet a DatagramPacket that was recived
	 */
	public synchronized void onReceipt(DatagramPacket packet) 
	{
		StringContent content = new StringContent(packet);

		if(isValidPacket(packet))
		{
			packetID++;
			connectionMade = true;
			this.notify();
		}			
	}

	/**
	 *Start
	 *	a function that is exicuted after the client is constructed
	 */
	public synchronized void start() throws Exception 
	{
		socket.setSoTimeout(Constants.TIMEOUT_TIME);
		while(!finished)
		{
			if(connectionMade = true)
			{
				DatagramPacket packet;
				packet = createPacket();
				socket.send(packet);
				terminal.println("    Packet sent");
				lastSentPacket = packet;
				this.connectionMade = false;
			}
			this.wait();	
		}
	}

	/**
	 *isValidPacket
	 *	a function that takes in a packet and checks to see if it is
	 *	valid.
	 *	@param packet a DatagramPacket to be checked
	 *	@retun isValidPacket
	 */
	private boolean isValidPacket(DatagramPacket packet)
	{
		boolean isValidPacket = false;

		byte[] destinationByteArray = new byte[Constants.HEADERLENGTH];
		byte[] packetArray = packet.getData();
		System.arraycopy(packetArray, 0, destinationByteArray, 
			0, destinationByteArray.length);
		ByteBuffer tempBuffer = ByteBuffer.wrap(destinationByteArray);
			
		int recievedPacketID = tempBuffer.getInt(Constants.HEADER_PACKET_ID_INDEX);
		int error = tempBuffer.getInt(Constants.HEADER_ERROR_INDEX);
		
		StringContent packetMessage = new StringContent(packet);
		if((recievedPacketID == (packetID + 1)) 
			&& (error == Constants.NO_ERROR_INT))
		{
			isValidPacket = true;
		}

		return isValidPacket;
	}

	/**
	 *createPacket
	 *	a function that looks at the users input and creates a packet
	 *	with the correct header elements.
	 *	@return packet 
	 */
	public DatagramPacket createPacket()
	{
		DatagramPacket packet = null;
		byte[] payload = null;
		byte[] header = new byte[Constants.HEADERLENGTH];
		
		payload = (terminal.readString("Message " + packetID + ": ")).getBytes();
		ByteBuffer tempBuffer = ByteBuffer.wrap(header);
		tempBuffer.putInt(Constants.HEADER_PACKET_ID_INDEX, packetID);
		tempBuffer.putInt(Constants.HEADER_SOURCE_INDEX, sourceID);
		tempBuffer.putInt(Constants.HEADER_DEST_INDEX, destID);
		header = tempBuffer.array();

		byte[] buffer = new byte[header.length + payload.length];
		System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(payload, 0, buffer, header.length, payload.length);
		
		packet = new DatagramPacket(buffer, buffer.length, dstAddress);

		return packet;
	}

	/**
	 *generatedID
	 *	a function that will generate a random number within a preset
	 *	limits
	 *	@return a random int between constraints
	 */
	public int generateID()
	{
  		double min = Math.ceil(Constants.MIN_CLIENT_ID);
  		double max = Math.floor(Constants.MAX_CLIENT_ID);
  		return (int)(Math.floor(Math.random() * (max - min + 1)) + min);
	}

	/**
	 *findPort
	 *	a function that takes in a min and max for ports and finds the lowest
	 *	port that isn't currently in use.
	 * 	@param portMin minium port possible
	 *	@param portMax maxium port possible
	 *	@return workingPort the free port, if no ports are free this value is -1
	 */
	public int findPort(int portMin, int portMax)
	{
		int workingPort = -1;
		int port = portMin;
		boolean foundPort = false;
		while(!foundPort)
		{	
			try 
    		{
    			DatagramSocket trySocket = new DatagramSocket(port);
    			workingPort = port;
        		foundPort = true;
        		trySocket.close();
   			} 
   			catch (java.lang.Exception ignored) 
   			{
   				if(port > Constants.MAX_CLIENT_PORT)
    			{
    				break;
    			}
   			}
    		port++;
		}
    	return workingPort;
	}

	/**
	 *timeout
	 *	a function that handles a lack of responce from contacted nodes
	 */
	public void timeout()
	{
		try
		{
			if(!connectionMade)
			{
				socket.send(lastSentPacket);
			}
		}
		catch(IOException e) {e.printStackTrace();}
			
		if(timeoutCode > Constants.MAX_TIMEOUTS)
		{
			terminal.println("\nConnection timed out after: " 
				+ (timeoutCode*((Constants.TIMEOUT_TIME)/1000)) + " Seconds");
			this.finished = true;
			socket.close();
		}

	}

	public static void main(String[] args) 
	{
		try {				
			Terminal terminal = new Terminal("Client");	
			Client client = new Client(terminal, DEFAULT_GTW_NODE, DEFAULT_GTW_PORT);
			client.start();
			terminal.println("Program completed");
		} catch(java.lang.Exception e) {e.printStackTrace();}
	}
}