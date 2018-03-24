//John Carbeck
//16309095

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.Arrays;

//Routing Table is  a table of routes needed for UDP Protocol
public class RoutingTable
{
	Route[] routes;
	public int length;
	public int routesInTable;

	private class Route
	{
		private InetSocketAddress sourceAddress;
		private InetSocketAddress finalAddress;
		private InetSocketAddress gateway;
		private int hops;
		private boolean connected = false;

		Route(InetSocketAddress sourceAddress, InetSocketAddress finalAddress, 
				InetSocketAddress gateway, int hops)
		{
			this.connected = true;
			this.sourceAddress = sourceAddress;
			this.finalAddress = finalAddress;
			this.gateway = gateway;
			this.hops = hops;
		}

		Route(InetSocketAddress sourceAddress, InetSocketAddress finalAddress)
		{
			this.connected = true;
			this.sourceAddress = sourceAddress;
			this.finalAddress = finalAddress;
			this.gateway = finalAddress;
			this.hops = 1;
		}

		public InetSocketAddress getSourceAddress() {return sourceAddress;}
		public InetSocketAddress getFinalAddress() {return finalAddress;}
		public InetSocketAddress getGateway() {return gateway;}
		public int getHops() {return hops;}
		
		public boolean isConnected()
		{
			return false;
		}

		public boolean compareTo(Route comparedRoute)
		{
			if( ((this.sourceAddress).equals(comparedRoute.getSourceAddress())) &&
				((this.finalAddress).equals(comparedRoute.getFinalAddress())) &&
				((this.gateway).equals(comparedRoute.getGateway())) &&
				((this.hops) == (comparedRoute.getHops()))) return true;

			return false;
		}
	}

	RoutingTable(int size)
	{
		this.routes = new Route[size];
		this.length = size;
	}

	public boolean addRoute(InetSocketAddress sourceAddress, InetSocketAddress finalAddress, 
				InetSocketAddress gateway, int hops)
	{
		boolean routeAdded = false;
		Route routeToAdd = new Route(sourceAddress, finalAddress, gateway, hops);
		if(!isInTable(sourceAddress, finalAddress))
		{
			if(!isRoutingTableFull())
			{	
				int i = 0;
				while((routes[i] != null) && (i < routes.length)) i++;
				if(i >= 0 && i < routes.length)
				{
					routes[i] = routeToAdd;
					routeAdded = true;
					routesInTable++;
				}
			}
		}
		return routeAdded;
	}

	public boolean addRoute(InetSocketAddress sourceAddress, InetSocketAddress finalAddress)
	{
		boolean routeAdded = false;
		Route routeToAdd = new Route(sourceAddress, finalAddress);
		if(!isInTable(sourceAddress, finalAddress))
		{
			if(!isRoutingTableFull())
			{
				int index = nextOpenRouteInTable();
				if(index != -1)
				{
					routes[index] = routeToAdd;
					routeAdded = true;
					routesInTable++;
				}
			}
		}
		return routeAdded;
	}

	public boolean addRoute(String routeAsString)
	{
		try
		{
			int hops;
			String[] routeElements = routeAsString.split("\\|");
			InetSocketAddress[] addresses = new InetSocketAddress[routeElements.length - 1];
			hops = Integer.parseInt(routeElements[routeElements.length - 1]);
			for(int i = 0; i < (routeElements.length - 1); i++)
			{
				String[] addressElements = routeElements[i].split("\\:");
				String[] ipElements = addressElements[0].split("\\/");
				InetAddress address = InetAddress.getByName(ipElements[1]);
				int port = Integer.parseInt(addressElements[1]);
				addresses[i] = new InetSocketAddress(address,port); 
			}
			return addRoute(addresses[0], addresses[1], addresses[2], hops);
		} catch(java.lang.Exception f) {return false;}
	}

	public boolean addAllRoutes(RoutingTable tableTo, RoutingTable tableFrom)
	{
		boolean valid = false;
		for(int i = 0; i < tableFrom.length; i++)
		{
			if(!tableTo.addRoute(tableFrom.getRoute(i))) return false;
		}
		return true;
	}

	public RoutingTable stringToTable(String tableAsString)
	{
		String[] tableElements = tableAsString.split("\\$");
		RoutingTable newTable = new RoutingTable(tableElements.length);
		for(int i = 0; i < tableElements.length; i++) newTable.addRoute(tableElements[i]);
		return newTable;
	}

	public String getRoute(InetSocketAddress source, InetSocketAddress destination)
	{	
		String route = "";
		boolean finished = false;
		int i = 0;
		while(i < routesInTable && !finished)
		{
			InetSocketAddress routeSource = routes[i].getSourceAddress();
			InetSocketAddress routeFinal = routes[i].getFinalAddress();
			if(routeSource.equals(source) && routeFinal.equals(destination))
			{
				route = getRoute(i);
				finished = true;
			}
			i++;
		}
		return route;
	}

	public InetSocketAddress getRouteSource(int index)
	{
		return routes[index].getSourceAddress();
	}

	public InetSocketAddress getRouteDestination(int index)
	{
		return routes[index].getFinalAddress();
	}

	public InetSocketAddress getRouteGateway(int index)
	{
		return routes[index].getGateway();
	}

	public String getRoute(int index)
	{
		return (routes[index].getSourceAddress()).toString() + "|" + (routes[index].getFinalAddress()).toString() +
		"|" + (routes[index].getGateway()).toString() + "|" + (routes[index].getHops());
	}

	public String getTable()
	{
		String routingTableString = "";
		for(int i = 0; i < routes.length; i++)
		{
			if(routes[i] != null)
			{
				routingTableString += getRoute(i);
				if(i < (routes.length - 1))
				{
					routingTableString += "$";
				}
			}
		}
		return routingTableString;
	}

	public String getTableSpace()
	{
		String routingTableString = "";
		for(int i = 0; i < routes.length; i++)
		{
			if(routes[i] != null)
			{
				routingTableString += getRoute(i);
				if(i < (routes.length - 1))
				{
					routingTableString += "\n";
				}
			}
		}
		return routingTableString;
	}

	public boolean isInTable(InetSocketAddress source, InetSocketAddress destination)
	{
		boolean isInTable = false;
		for(int i = 0; i < routesInTable; i++)
		{
			InetSocketAddress routeSource = routes[i].getSourceAddress();
			InetSocketAddress routeFinal = routes[i].getFinalAddress();
			if(routeSource.equals(source) && routeFinal.equals(destination))
			{
				isInTable = true;
				i = routesInTable;
			} 
		}	
		return isInTable;
	}

	public InetSocketAddress getNextRouter(InetSocketAddress source, InetSocketAddress destination)
	{
		for(int i = 0; i < routesInTable; i++)
		{
			InetSocketAddress routeSource = routes[i].getSourceAddress();
			InetSocketAddress routeFinal = routes[i].getFinalAddress();
			if(routeSource.equals(source) && routeFinal.equals(destination)) return routes[i].getGateway();
		}
		return null;
	}

	public void empty()
	{
		for (int i = 0; i < length; i++) routes[i] = null;
	}

	private int nextOpenRouteInTable()
	{
		int i = 0;
		while((routes[i] != null) && (i < routes.length)) i++;
		if(i >= 0 && i < routes.length) return i;
		else return - 1;
	}

	private boolean isRoutingTableFull()
	{
		boolean isRoutingTableFull = true;
		for(int i = 0; i < routes.length; i++)
		{
			if(routes[i] == null) isRoutingTableFull = false;
		}
		return isRoutingTableFull;
	}
}