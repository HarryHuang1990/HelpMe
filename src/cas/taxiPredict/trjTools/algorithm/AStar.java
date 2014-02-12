package cas.taxiPredict.trjTools.algorithm;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import cas.taxiPredict.trjTools.roadNetwork.Edge;
import cas.taxiPredict.trjTools.roadNetwork.EdgePath;
import cas.taxiPredict.trjTools.roadNetwork.GeoPoint;
import cas.taxiPredict.trjTools.roadNetwork.Graph;
import cas.taxiPredict.trjTools.roadNetwork.Vertex;

public class AStar {

	public EdgePath FindPath(Vertex src, Vertex dest)
	{
		return FindPath(src, dest,Double.MAX_VALUE);
	}
    public EdgePath FindPath(Vertex src, Vertex dest, double maxDist)
    {
        TreeMap<Node, Node> openTable = new TreeMap<Node, Node>();
        HashMap<Long, Node> openVertexId = new HashMap<Long, Node>(); //Used to get a node by node id
        HashMap<Long, Node> closedSet = new HashMap<Long, Node>();
        GeoPoint srcPoint = src.ToPoint();
        GeoPoint destPoint = dest.ToPoint();

        //push initial node
        double estimatedCost = GeoPoint.GetDistance(destPoint, srcPoint);
        Node n = new Node(src, 0, estimatedCost, null, null);
        openTable.put(n, n);
        openVertexId.put(src.getId(), n);

        //Begin search
        Node solution = null;
        Node removed = null;
        while (openTable.size() > 0)
        {
            //Pop the node with minimum weight
            Node parent = openTable.keySet().iterator().next();
            removed = openTable.remove(parent);
            removed = openVertexId.remove(parent.v.getId());
            closedSet.put(parent.v.getId(), parent);
            if (parent.Weight() > maxDist)
            {
                //Stop searching
                break;
            }
            if (parent.v == dest)
            {
                solution = parent;
                break;
            }

            //Get children
            List<Edge> edges = parent.v.getOutEdges();
            for (int i = 0; i < edges.size(); i++)
            {
                Edge currentEdge = edges.get(i);
                double g = parent.g + currentEdge.Length();
                Vertex v = currentEdge.End();
                double h = GeoPoint.GetDistance(v.ToPoint(), destPoint);
                Node tmpNode = closedSet.get(v.getId());
                if (tmpNode!=null)
                {
                    if (g + h >= tmpNode.Weight())
                    {
                        //this is a visited node 
                        continue;
                    }
                    else
                    {
                        removed = closedSet.remove(v.getId());
                    }
                }
                tmpNode=openVertexId.get(v.getId());
                if (tmpNode!=null)
                {
                    //Check if it has a lower cost
                    if (tmpNode.Weight() > h + g)
                    {
                        removed = openTable.remove(tmpNode);
                        tmpNode.g = g;
                        tmpNode.h = h;
                        tmpNode.e = currentEdge;
                        tmpNode.parent = parent;
                        openTable.put(tmpNode, tmpNode);
                        openVertexId.put(v.getId(), tmpNode);
                    }
                }
                else
                {
                    Node newNode = new Node(v, g, h, currentEdge, parent);
                    openTable.put(newNode, newNode);
                    openVertexId.put(v.getId(), newNode);
                }
            }

        }

        //Find path
        List<Edge> path = null;
        EdgePath list = null;
        if (solution != null)
        {
            path = new ArrayList<Edge>();
            while (solution.parent != null)
            {
                path.add(solution.e);
                solution = solution.parent;
            }
            Collections.reverse(path);
            list = new EdgePath(path);
        }
        return list;
    }

    public HashSet<Edge> GetCandiateEdges(Vertex src, GeoPoint destPoint, double maxCost, double maxDist)
    {
    	TreeMap<Node, Node> openTable = new TreeMap<Node, Node>();
        HashMap<Long, Node> openVertexId = new HashMap<Long, Node>(); //Used to get a node by node id
        HashMap<Long, Node> closedSet = new HashMap<Long, Node>();
        GeoPoint srcPoint = src.ToPoint();
        HashSet<Edge> cands = new HashSet<Edge>();
        //GeoPoint destPoint = dest;

        //push initial node
        double estimatedCost = GeoPoint.GetDistance(destPoint, srcPoint);
        Node n = new Node(src, 0, estimatedCost, null, null);
        openTable.put(n, n);
        openVertexId.put(src.getId(), n);

        //Begin search
        while (openTable.size() > 0)
        {
            //Pop the node with minimum weight
            Node parent = openTable.keySet().iterator().next();
            openTable.remove(parent);
            openVertexId.remove(parent.v.getId());
            closedSet.put(parent.v.getId(), parent);
            if (parent.Weight() > maxCost)
            {
                //Stop searching
                break;
            }
            if (parent.e != null && parent.e.DistFrom(destPoint) < maxDist)
            {
                cands.add(parent.e);
            }
            //Get children
            List<Edge> edges = parent.v.getOutEdges();
            for (int i = 0; i < edges.size(); i++)
            {
                Edge currentEdge = edges.get(i);
                //double g = parent.g + currentEdge.Length;
                double g = parent.g;
                GeoPoint result=GeoPoint.INVALID();
                if (parent.e != null)
                {
                    g += parent.e.Length();
                }
                Vertex v = currentEdge.End();
                int type = currentEdge.projectFrom(destPoint,result);
                double h = 0;
                if (type == 0)
                {
                    h = GeoPoint.GetDistance(currentEdge.Start().ToPoint(), result) + GeoPoint.GetDistance(result, destPoint);
                }
                else
                {
                    h = currentEdge.Length() + GeoPoint.GetDistance(currentEdge.End().ToPoint(), destPoint);
                }
                Node tmpNode = closedSet.get(v.getId());
                if (tmpNode!=null)
                {
                    if (g + h >= tmpNode.Weight())
                    {
                        //this is a visited node 
                        continue;
                    }
                    else
                    {
                        closedSet.remove(v.getId());
                    }
                }
                tmpNode=openVertexId.get(v.getId());
                if (tmpNode!=null)
                {
                    //Check if it has a lower cost
                    if (tmpNode.Weight() > h + g)
                    {
                        openTable.remove(tmpNode);
                        tmpNode.g = g;
                        tmpNode.h = h;
                        tmpNode.e = currentEdge;
                        tmpNode.parent = parent;
                        openTable.put(tmpNode, tmpNode);
                        openVertexId.put(v.getId(),tmpNode);
                    }
                }
                else
                {
                    Node newNode = new Node(v, g, h, currentEdge, parent);
                    openTable.put(newNode, newNode);
                    openVertexId.put(v.getId(), newNode);
                }
            }

        }
        return cands;
    }
    
	private class NodeComparer implements Comparator<Node>
    {
		@Override
		public int compare(Node o1, Node o2) {
			return (int)(o1.Weight() - o2.Weight());
		}
    }
	
	private class NodeEquityComparer implements Comparator<Node>
    {

        public int GetHashCode(Node obj)
        {
            return obj.v.hashCode();
        }

		@Override
		public int compare(Node o1, Node o2) {
			return (int) (o1.v.getId()-o2.v.getId());
		}
    }
	private Graph graph = null;
    public AStar(Graph g)
    {
        this.graph = g;
    }
    
	
	private class Node implements Comparable<Node>
    {
        /// <summary>
        /// Past  
        /// </summary>
        public double g;
        /// <summary>
        /// Estimated
        /// </summary>
        public double h;
        /// <summary>
        /// Edge used to generate path
        /// </summary>
        public Edge e;
        /// <summary>
        /// Total weight
        /// </summary>
        public double Weight()
        {
           return g + h;
        }
        public Node parent;
        public Vertex v;

        public Node(Vertex v, double g, double h, Edge e, Node parent)
        {
            this.v = v;
            this.g = g;
            this.h = h;
            this.e = e;
            this.parent = parent;
        }

        public boolean equals(Object obj)
        {
            return (obj instanceof Node) && (((Node)obj).v.getId() == this.v.getId());
        }
        public int hashCode()
        {
            return this.v.hashCode();
        }
        public  String toString()
        {
        	return (new MessageFormat("Vertex:{0},Weight:{1:0.##},Edge:{2}")).format(new Object[]{v.getId(), Weight(), e});
        }

		@Override
		public int compareTo(Node other) {
			 double diff = (this.Weight() - other.Weight());
	            int result = (int) Math.signum(diff);
	            if (result == 0)
	            {
	                result = (int)(this.v.getId() - other.v.getId());
	            }
	            return result;
		}
    }
}
