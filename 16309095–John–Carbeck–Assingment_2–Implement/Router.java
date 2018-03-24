//John Carbeck
//16309095


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.InetAddress;

import tcdIO.Terminal;

public class Router extends Node
{
	Terminal terminal;
	
	InetSocketAddress controllerAddress;
	int controllerIndex;
	DatagramPacket lastPacket;
	InetSocketAddress lastDestination;
	InetSocketAddress lastSource;
	int[] localPorts;

	RoutingTable localTable;
	RoutingTable flowTable = new RoutingTable(10); 

	Router(Terminal terminal, int[] ports, int[] portsToConnectTo)
	{
		try
		{
			this.terminal = terminal;
			this.localPorts = ports;
			socket = new DatagramSocket[portsToConnectTo.length];
			connectedAddresses = new InetSocketAddress[portsToConnectTo.length];
			localTable = new RoutingTable(portsToConnectTo.length + ports.length + (ports.length/2 + 1));

			for(int i = 0; i < ports.length; i++)
			{
				InetSocketAddress aAddress = new InetSocketAddress(Constants.DEFAULT_NODE, ports[i]);
				InetSocketAddress bAddress;
				if(i == ports.length -1)
				{
					bAddress = new InetSocketAddress(Constants.DEFAULT_NODE, ports[0]);
				}
				else
				{
					bAddress = new InetSocketAddress(Constants.DEFAULT_NODE, ports[i + 1]);
				}
				localTable.addRoute(aAddress,bAddress,bAddress,0);
			}

			for (int i = 0; i < (ports.length/2); i++) 
			{
				InetSocketAddress aAddress = new InetSocketAddress(Constants.DEFAULT_NODE, ports[i]);
				InetSocketAddress bAddress;
				if(i == ((ports.length/2) -1))
				{
					bAddress= new InetSocketAddress(Constants.DEFAULT_NODE, ports[ports.length-1]);
				}
				else
				{
					bAddress= new InetSocketAddress(Constants.DEFAULT_NODE, ports[i + 2]);
				}
				localTable.addRoute(aAddress,bAddress,bAddress,0);
			}

			for(int i = 0; i < portsToConnectTo.length; i++)
			{
				socket[i] = new DatagramSocket(ports[i]);
				connectedAddresses[i] = new InetSocketAddress(Constants.DEFAULT_NODE, portsToConnectTo[i]);
				if(i <= portsToConnectTo.length - 1)
				{
					localTable.addRoute(new InetSocketAddress(Constants.DEFAULT_NODE, ports[i]),
						connectedAddresses[i]);
				}
			}
			controllerIndex = connectedAddresses.length - 1;
			controllerAddress = connectedAddresses[controllerIndex];
			sendTable(localTable, controllerAddress);
			listen(socket);		

		} catch(java.lang.Exception e) {e.printStackTrace();}
	}

	private void sendTable(RoutingTable table, InetSocketAddress destination)
	{
		try
		{
			int socketIndex = findSocketIndex(destination);
			DatagramSocket socketForSending = socket[socketIndex];

			byte[] header = new byte[Constants.HEADERLENGTH];
			byte[] payload = (table.getTable()).getBytes();
			
			ByteBuffer tempbuffer = ByteBuffer.wrap(header);
			byte[] sourceAddress = addressToByte(InetAddress.getByName(Constants.DEFAULT_NODE));
			byte[] destinationAddress = addressToByte(InetAddress.getByName(Constants.DEFAULT_NODE));
			int sourcePort = socket[socketIndex].getLocalPort();
			int destinationPort = destination.getPort();

			for (int i = 0; i < sourceAddress.length; i++)
				tempbuffer.put(Constants.SOURCE_INDEX + i, sourceAddress[i]);
			for(int i = 0; i < destinationAddress.length; i++)
				tempbuffer.put(Constants.DEST_INDEX + i, destinationAddress[i]);
			tempbuffer.putInt(Constants.SOURCE_PORT_INDEX, sourcePort);
			tempbuffer.putInt(Constants.DEST_PORT_INDEX, destinationPort);

			byte[] buffer = new byte[header.length + payload.length];
			System.arraycopy(header, 0, buffer, 0, header.length);
			System.arraycopy(payload, 0, buffer, header.length, payload.length);
					
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, controllerAddress);
			socket[socketIndex].send(packet);	
		} catch(java.lang.Exception e) {e.printStackTrace();}	
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
			processPacket(packet);
			this.notify();
		}
		catch(Exception e) {e.printStackTrace();}
	}

	private synchronized void processPacket(DatagramPacket packet)
	{
		byte[] packetData = packet.getData();
		byte[] destData = new byte[4];
		byte[] srcData = new byte[4];

		ByteBuffer tempbuffer = ByteBuffer.wrap(packetData);
		
		for(int i = 0; i < destData.length; i++)
			destData[i] = tempbuffer.get(Constants.DEST_INDEX + i);
		for(int i = 0; i < srcData.length; i++)
			srcData[i] = tempbuffer.get(Constants.SOURCE_INDEX + i);
	
		InetAddress destAdress = byteToAddress(destData);
		int destPort = tempbuffer.getInt(Constants.DEST_PORT_INDEX);
		InetSocketAddress destination = new InetSocketAddress(destAdress,destPort);

		InetAddress srcAddress = byteToAddress(srcData);
		int srcPort = tempbuffer.getInt(Constants.SOURCE_PORT_INDEX);
		InetSocketAddress source = new InetSocketAddress(srcAddress,srcPort);

		
		if(source.equals(controllerAddress))
		{
			InetSocketAddress newSource = lastSource;
			flowTable.addAllRoutes(flowTable, flowTable.stringToTable((new StringContent(packet)).toString()));

			
			while(((flowTable.getNextRouter(newSource,lastDestination)).getPort() >= localPorts[0]) &&
				((flowTable.getNextRouter(newSource,lastDestination)).getPort() <= localPorts[localPorts.length-1]))
			{
				newSource = flowTable.getNextRouter(newSource, lastDestination);
			}
			terminal.println("Route added!");
			System.out.println(flowTable.getTableSpace());
			sendPacket(newSource, lastDestination, lastPacket);
		}
		else 
		{
			terminal.println("Recieved from: " + source.toString());
			sendPacket(source, destination, packet);
		}
		
	}

	private synchronized void sendPacket(InetSocketAddress source, InetSocketAddress destination, DatagramPacket packet) 
	{
		try{

			if(!flowTable.isInTable(source, destination))
			{
				this.lastPacket = packet;
				this.lastDestination = destination;
				source = new InetSocketAddress(Constants.DEFAULT_NODE, socket[controllerIndex].getLocalPort());
				this.lastSource = source;
				byte[] buffer = new byte[Constants.HEADERLENGTH];
				byte[] srcAddress = addressToByte(source.getAddress());
				int srcPort = source.getPort();
				byte[] dstAddress = addressToByte(destination.getAddress());
				int dstPort = destination.getPort();
				
				ByteBuffer tempbuffer = ByteBuffer.wrap(buffer);
				for(int i = 0; i < srcAddress.length; i++)
					tempbuffer.put(Constants.SOURCE_INDEX + i, srcAddress[i]);
				for(int i = 0; i < dstAddress.length; i++)
					tempbuffer.put(Constants.DEST_INDEX + i, dstAddress[i]);

				tempbuffer.putInt(Constants.SOURCE_PORT_INDEX, srcPort);
				tempbuffer.putInt(Constants.DEST_PORT_INDEX, dstPort);

				buffer = tempbuffer.array();
				DatagramPacket newPacket = new DatagramPacket(buffer, buffer.length, controllerAddress);
				socket[controllerIndex].send(newPacket);
				terminal.println("Getting route...");	
			}
			else
			{	
				InetSocketAddress gateway = flowTable.getNextRouter(source,destination);
				int index = findSocketIndex(gateway);
				packet.setSocketAddress(gateway);
				terminal.println("Sent to: " + gateway.toString());
				socket[index].send(packet);	
			}

		} catch(java.lang.Exception e) {e.printStackTrace();}
	}


	/**
	 *start
	 *	a function that is run after the construction of the server
	 */
	public synchronized void start() throws Exception 
	{
		while(true)
		{
			this.wait();
		}
	}

	public static void main(String[] args)
	{
		if(args.length > 0 && args.length <= 5)
		{
			String routerID = args[0];
			int[] portsToConnectTo = new int[args.length - 1];
			int[] ports = new int[0];

			switch(routerID)
			{
				case "1":
					ports = Constants.ROUTER_1;
					break;
				case "2":
					ports = Constants.ROUTER_2;
					break;
				case "3":
					ports = Constants.ROUTER_3;
					break;
				case "4":
					ports = Constants.ROUTER_4;
					break;
				case "5":
					ports = Constants.ROUTER_5;
					break;
				case "6":
					ports = Constants.ROUTER_6;
					break;
				case "7":
					ports = Constants.ROUTER_7;
					break;
				case "8":
					ports = Constants.ROUTER_8;
					break;
				default:
					break;
			}

			for (int i = 0; i < portsToConnectTo.length; i++) 
			{
				portsToConnectTo[i] = Integer.valueOf(args[i + 1]);
			}

			try
			{
				Terminal Terminal = new Terminal("Router" + routerID);
				Router router = new Router(Terminal, ports, portsToConnectTo);
				router.start();
			} catch(java.lang.Exception e) {e.printStackTrace();}
		}
	}
}