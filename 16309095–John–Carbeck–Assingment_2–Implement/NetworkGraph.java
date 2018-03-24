//John Carbeck
//16309095

import java.util.LinkedList;
import java.util.Arrays;
import java.net.InetSocketAddress;

public class NetworkGraph
{
	private final LinkedList<LinkedList<Integer>> adj;
	private RoutingTable tempTable = new RoutingTable(10);

	public NetworkGraph()
	{
		adj = new LinkedList<LinkedList<Integer>>();
		for(int i = 0; i < adj.size(); i++)
			adj.add(new LinkedList<Integer>());
	}

	public void addEdge(int x, int y)
	{
		Integer xInt = x;
		Integer yInt = y;
		
		if(isNewEdge(x,y)) 
		{
			if(!isVertexInGraph(x)) addVertex(x);
			if(!isVertexInGraph(y)) addVertex(y);

			getList(getIndexOf(x)).add(yInt);
			getList(getIndexOf(y)).add(xInt);
		}
	}

	public void addVertex(int x)
	{
		if(!isVertexInGraph(x))
		{
			adj.add(new LinkedList<Integer>());
			adj.get(adj.size() - 1).add((Integer)x);
		}
	}

	public boolean isVertexInGraph(int x)
	{
		for(int i = 0; i < adj.size(); i++)
		{
			if((adj.get(i).get(0)).equals((Integer)(x))) return true;
		}
		return false;
	}

	public boolean isNewEdge(int x, int y)
	{
		Integer xInt = (Integer) x;
		Integer yInt = (Integer) y;
		if(isVertexInGraph(x) && isVertexInGraph(y))
		{
			if(getList(getIndexOf(x)).contains(yInt) && getList(getIndexOf(y)).contains(xInt)) return false;
		}
		
		return true;
	}

	public void add(String routes)
	{
		tempTable = tempTable.stringToTable(routes);
		for(int i = 0; i < tempTable.routesInTable; i++)
		{
			int node1 = (Integer)(tempTable.getRouteSource(i)).getPort();
			int node2 = (Integer)(tempTable.getRouteDestination(i)).getPort();
			addEdge(node1, node2);
		}
	}

	public String findRoute(InetSocketAddress source, 
		InetSocketAddress destination, NetworkGraph graph)
	{
		int srcPort = source.getPort();
		int dstPort = destination.getPort();
		String routesAdded = "";

		if(isVertexInGraph(srcPort) && isVertexInGraph(dstPort))
		{
			Path path = new Path(graph, srcPort);
			LinkedList<Integer> shortestPath = path.pathTo(graph, dstPort);
			if(shortestPath != null)
			{
				RoutingTable tempTable = new RoutingTable(shortestPath.size());
				
				InetSocketAddress routeDestination = destination;
				for(int i = 0; i < (shortestPath.size() - 1); i++)
				{
					InetSocketAddress routeSource = new 
						InetSocketAddress(Constants.DEFAULT_NODE, shortestPath.get(i));
					InetSocketAddress routeGateway = new
						InetSocketAddress(Constants.DEFAULT_NODE, shortestPath.get(i + 1));
					int hops = shortestPath.size() - 1 - i;
					tempTable.addRoute(routeSource, routeDestination, routeGateway, hops);
				}
				routesAdded = tempTable.getTable();
			}
		}
		return routesAdded;
	}

	public LinkedList<Integer> getList(int index)
	{
		return adj.get(index);
	}

	public int getIndexOf(int index)
	{
		for(int i = 0; i < this.adj.size(); i++)
			if((adj.get(i).get(0)).equals((Integer)index)) return i;
		return - 1;
	}

	public int size()
	{
		return this.adj.size();
	}

	public String graphToString(NetworkGraph graph)
	{
		String graphString = "";
		for(int i = 0; i < graph.size(); i++)
		{
			graphString +=  graph.getList(i).size() + ": "+ Arrays.toString((graph.getList(i)).toArray()) + "\n";
		}
		return graphString;
	}

	private class Path
	{
		private Vertex[] marked;
		private int[] edgeTo;
		private final int source;

		Path(NetworkGraph graph, int source)
		{
			marked = new Vertex[graph.size()];
			edgeTo = new int[graph.size()];
			this.source = source;

			for(int i = 0; i < graph.size(); i ++)
			{
				marked[i] = new Vertex((graph.getList(i)).get(0));
			}

			findPathsBFS(graph,source);
		}


		private void findPathsBFS(NetworkGraph graph, int source)
		{
			LinkedList<Integer> queue = new LinkedList<Integer>();
			marked[graph.getIndexOf(source)].mark(true); //mark the source

			queue.push(source);	//put source into the queue
			while(!queue.isEmpty())
			{
				int v = queue.poll();		//remove the next vertex from the graph
				int indexOfV = graph.getIndexOf(v);
				for(int i = 1; i < graph.getList(indexOfV).size(); i++)
				{

					int element = graph.getList(indexOfV).get(i);
					int indexOfElement = graph.getIndexOf(element);
	
					if(!(marked[indexOfElement].isMarked()))	//for ever unmarked adjacent vertex
					{
						edgeTo[indexOfElement] = v;	//save the last edge to the shortest path
						marked[indexOfElement].mark(true);	// mark true
						queue.push(element);	//add it to the queue
					}
				}
			}
		}

		public boolean hasPathTo(int vertex)
		{
			if(isVertexInGraph(vertex))
			{
				return marked[getIndexOf(vertex)].isMarked();		
			}
			else return false;
		}

		public LinkedList<Integer> pathTo(NetworkGraph graph, int vertex)
		{
			if(!hasPathTo(vertex)) return null;
			LinkedList<Integer> path = new LinkedList<Integer>();
			for(int x = vertex; x != source; x = edgeTo[graph.getIndexOf(x)])
			{
				path.push(x);
			}
			path.push(source);
			
			return path;
		}

		private class Vertex
		{
			private boolean marked = false;
			private int value;
			
			Vertex(int value)
			{
				this.value = value;
			}
			public int getValue()
			{
				return value;
			}
			public void mark(boolean marked)
			{
				this.marked = marked;
			}
			public boolean isMarked()
			{
				return marked;
			}
		}
	}
}