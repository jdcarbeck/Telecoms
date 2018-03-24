//John Carbeck
//16309095
	
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.lang.Integer;

import tcdIO.Terminal;

public class Controller extends Node
{
	private Terminal terminal;
	private NetworkGraph network = new NetworkGraph();
	private RoutingTable routingTable = new RoutingTable(1000);

	Controller(Terminal terminal, int[] ports)
	{
		try
		{
			this.terminal = terminal;
			socket = new DatagramSocket[ports.length];
			connectedAddresses = new InetSocketAddress[ports.length];
			for(int i = 0; i < ports.length; i ++)
			{
				socket[i] = new DatagramSocket(new InetSocketAddress(Constants.DEFAULT_NODE, Constants.CONTROLLER[i]));
				connectedAddresses[i] = new InetSocketAddress(Constants.DEFAULT_NODE, ports[i]);
			}
			listen(socket);
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

	private void processPacket(DatagramPacket packet)
	{
		byte[] packetData = packet.getData();
		byte[] destData = new byte[4];
		byte[] srcData = new byte[4];

		ByteBuffer tempbuffer = ByteBuffer.wrap(packetData);
		
		for(int i = 0; i < destData.length; i++)
			destData[i] = tempbuffer.get(Constants.DEST_INDEX + i);
		for(int i = 0; i < srcData.length; i++)
			srcData[i] = tempbuffer.get(Constants.SOURCE_INDEX + i);
	
		InetAddress destAddress = byteToAddress(destData);
		int destPort = tempbuffer.getInt(Constants.DEST_PORT_INDEX);
		InetSocketAddress destination = new InetSocketAddress(destAddress,destPort);

		InetAddress srcAddress = byteToAddress(srcData);
		int srcPort = tempbuffer.getInt(Constants.SOURCE_PORT_INDEX);

		InetSocketAddress source = new InetSocketAddress(srcAddress,srcPort);

		if(!((new StringContent(packet).toString()).equals("")))
		{
			String tableString = (new StringContent(packet)).toString();
			routingTable.addAllRoutes(routingTable, routingTable.stringToTable(tableString));
			network.add(tableString);
		
			terminal.println("New router connection!");
		}
		else
		{
			terminal.println("Gateway: " + source.toString() + " -> " + destination.toString());

			if(!(routingTable.isInTable(source, destination)))
			{
				RoutingTable tempTable = routingTable.stringToTable(network.findRoute(source, destination, network));
				routingTable.addAllRoutes(routingTable, tempTable);
			}
			try
			{
				int[] routerPorts = getRouterPorts(source.getPort());

				RoutingTable tempTable = new RoutingTable(Constants.ROUTER_SIZE);
				for (int i = 0; i < routerPorts.length; i++ ) 
				{
					InetSocketAddress tempAddress = new InetSocketAddress(Constants.DEFAULT_NODE, routerPorts[i]);
					tempTable.addRoute(routingTable.getRoute(tempAddress,destination));
				}
				String payloadString = tempTable.getTable(); 

				
				byte[] payload = payloadString.getBytes();
				byte[] header = new byte[Constants.HEADERLENGTH];
				
				InetSocketAddress newSource = new InetSocketAddress(Constants.DEFAULT_NODE, socket[findSocketIndex(source)].getLocalPort());
				InetSocketAddress newDestination = source;
				byte[] newSrcData = addressToByte(source.getAddress());
				byte[] newDestData = addressToByte(destination.getAddress());

				tempbuffer = ByteBuffer.wrap(header);
				for(int i = 0; i < newDestData.length; i++)
					tempbuffer.put(Constants.DEST_INDEX + i, newSrcData[i]);
				for(int i = 0; i < newSrcData.length; i++)
					tempbuffer.put(Constants.SOURCE_INDEX + i, newSrcData[i]);

				tempbuffer.putInt(Constants.SOURCE_PORT_INDEX, newSource.getPort());
				tempbuffer.putInt(Constants.DEST_PORT_INDEX, newDestination.getPort());
				header = tempbuffer.array();

				byte[] buffer = new byte[header.length + payload.length];
				System.arraycopy(header, 0, buffer, 0, header.length);
				System.arraycopy(payload, 0, buffer, header.length, payload.length);

				socket[findSocketIndex(source)].send(new DatagramPacket(buffer, buffer.length, source));

			} catch(java.lang.Exception e) {e.printStackTrace();}
		}
	}

	private int[] getRouterPorts(int port)
	{
		if(port >= Constants.ROUTER_MIN_1 && port <= Constants.ROUTER_MAX_1)
		{
			return Constants.ROUTER_1;
		}
		else if(port >= Constants.ROUTER_MIN_2 && port <= Constants.ROUTER_MAX_2)
		{
			return Constants.ROUTER_2;
		}
		else if(port >= Constants.ROUTER_MIN_3 && port <= Constants.ROUTER_MAX_3)
		{
			return Constants.ROUTER_3;
		}
		else if(port >= Constants.ROUTER_MIN_4 && port <= Constants.ROUTER_MAX_4)
		{
			return Constants.ROUTER_4;
		}
		else if(port >= Constants.ROUTER_MIN_5 && port <= Constants.ROUTER_MAX_5)
		{
			return Constants.ROUTER_5;
		}
		else if(port >= Constants.ROUTER_MIN_6 && port <= Constants.ROUTER_MAX_6)
		{
			return Constants.ROUTER_6;
		}
		else if(port >= Constants.ROUTER_MIN_7 && port <= Constants.ROUTER_MAX_7)
		{
			return Constants.ROUTER_7;
		}
		else if(port >= Constants.ROUTER_MIN_8 && port <= Constants.ROUTER_MAX_8)
		{
			return Constants.ROUTER_8;
		}
		else
		{
			return null;
		}
	}
	
	public static void main(String[] args)
	{
		if(args.length > 0 && args.length <= 5)
		{
			int[] portsToConnectTo = new int[args.length];
			for (int i = 0; i < args.length; i++) 
			{
				portsToConnectTo[i] = Integer.valueOf(args[i]);
			}
			try
			{
				Terminal Terminal = new Terminal("Controller");
				Controller controller = new Controller(Terminal, portsToConnectTo);
				controller.start();
			} catch(java.lang.Exception e) {e.printStackTrace();}
		}
	}
}
