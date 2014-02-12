package cas.taxiPredict.trjTools.roadNetwork;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Vertex {

	public Vertex(long id, double lat, double lng)
    {
        this.id = id;
        this.point = new GeoPoint(lat, lng);
    }
	
	private void calculateInOut()
    {
        synchronized (syncRoot)
        {
            if (outEdges == null)
            {
                int edgeSize = adjacentEdges.size();
                outEdges = new LinkedList<Edge>();
                inEdges = new LinkedList<Edge>();
                for (int i = 0; i < edgeSize; i++)
                {
                    if (adjacentEdges.get(i).Start() == this)
                    {
                        outEdges.add(adjacentEdges.get(i));
                    }
                    else
                    {
                        inEdges.add(adjacentEdges.get(i));
                    }
                }
            }
        }

    }
	
	 long id;
     public long getId() {
		return id;
	}
	GeoPoint point;

     public double getLat()
     {
         return point.getLat();
     }

     public double getLng()
     {
         return point.getLng();
     }
     
     private final Object syncRoot = new Object();
     private List<Edge> adjacentEdges = new ArrayList<Edge>();

     private List<Edge> outEdges = null;
     private List<Edge> inEdges = null;

     /**
      * 返回相邻边,不考虑方向
      * @author Harry Huang
      * @date 2014.2.11
      * @return
      */
     public List<Edge> getAdjacentEdges(){
    	 return this.adjacentEdges;
     }
     
     public List<Edge> getOutEdges()
     {
    	 if (outEdges == null)
         {
             calculateInOut();
         }
         return outEdges;
     }
     public List<Edge> getInEdges()
     {
    	 if (inEdges == null)
         {
             calculateInOut();
         }
         return inEdges;
     }
     
     
     public boolean equals(Object obj)
     {
         if (obj instanceof Vertex)
         {
             return ((Vertex)obj).getId() == this.getId();
         }
         return false;
     }
     public int hashCode()
     {
         return (int)this.getId() ^ (int)(this.getId() >> 32);
     }
     public void RegisterEdge(Edge e)
     {
    	 synchronized (syncRoot)
         {
             this.adjacentEdges.add(e);
         }
     }
     
     public GeoPoint ToPoint()
     {
         return this.point;
     }
     public GeoPoint getPoint()
     {
         return this.point;
     }
//     public Coordinate getCorrdinate()
//     {
//          return new Coordinate(this.point.getLng(), this.point.getLat());
//     }
     public String toString()
     {
    	 return (new MessageFormat("Vertex:{0}:({1},{2})")).format(new Object[]{this.getId(), this.getLng(), this.getLat()});
     }
}
