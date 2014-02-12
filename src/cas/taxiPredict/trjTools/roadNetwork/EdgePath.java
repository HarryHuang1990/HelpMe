package cas.taxiPredict.trjTools.roadNetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import cas.taxiPredict.pathIdentify.outWrapper.TypeObject;

public class EdgePath{
	
	 List<Edge> edges = new LinkedList<Edge>();
     HashSet<Long> vertices = new HashSet<Long>();
     Vertex dummyVertex = null;
     public List<Edge> getEdges()
     {
         return edges; 
     }
     public EdgePath(Collection<Edge> edges)
     {
         this.edges = new LinkedList<Edge>(edges);
         for (Edge e : edges)
         {
             this.vertices.add(e.Start().getId());
             this.vertices.add(e.End().getId());
         }
     }

     public EdgePath(EdgePath path)
     {
         this.edges = new LinkedList<Edge>(path.edges);
         this.vertices = new HashSet<Long>(path.vertices);
     }
     public EdgePath(Vertex v)
     {
         this.dummyVertex = v;
     }
     public EdgePath()
     {
     }
     
     public int Count()
     {
             return this.edges.size();
     }
     public Edge get(int idx)
     {
    	 Edge e = null;
             if (idx < this.Count() && idx >= 0)
             {
                 e = this.edges.get(idx);
             }
             return e;
     }
     
     /// The start vertex
     public Vertex Start()
     {
         Vertex v = dummyVertex;
         if (this.Count() > 0)
         {
             v = this.get(0).Start();
         }
         return v;
     }
     /// The end vertex
     public Vertex End()
     {
         Vertex v = dummyVertex;
         if (this.Count() > 0)
         {
             v = this.get(this.Count() - 1).End();
         }
         return v;
     }
     
     public Edge FirstEdge()
     {
             Edge e = null;
             if (this.Count()> 0)
             {
                 e = this.get(0);
             }
             return e;

     }
     public Edge LastEdge()
     {

         Edge e = null;
         if (this.Count() > 0)
         {
             e = this.get(this.Count() - 1);
         }
         return e;
     }
     public String toString()
     {
         StringBuilder sb = new StringBuilder();
         for (Edge e : this.edges)
         {
             sb.append(e.ID()).append("->");
         }
         return sb.toString();
     }
     public void add(Edge e)
     {
         this.edges.add(e);
         this.vertices.add(e.Start().getId());
         this.vertices.add(e.End().getId());
         //this.dummyVertex = null;
     }
     public boolean Contains(Edge e)
     {
         return this.edges.contains(e);
     }
     public boolean Contains(Vertex v)
     {
         return this.vertices.contains(v.getId());
     }
     public GeoPoint Predict(GeoPoint start, double distance)
     {
    	 return Predict(start, distance,0);
     }
   /// Predict the position from start after distance on this route
     public GeoPoint Predict(GeoPoint start, double distance, int startIdx)
     {
         GeoPoint target = this.End().ToPoint();
         //GeoPoint target = GeoPoint.INVALID;
         double currentDistance = 0;
         while (startIdx < this.Count())
         {
             Edge e = this.get(startIdx);
             TypeObject typeObj=new TypeObject(0);
             double length = e.EndDistFrom(start, typeObj);
             if (currentDistance + length >= distance)
             {
                 double leftDistance = distance - currentDistance;
                 target = e.Predict(start, leftDistance);
                 break;
             }
             else
             {
                 currentDistance += length;
                 startIdx++;
                 start = e.End().ToPoint();
             }
         }
         return target;
     }
     
     public double DistanceOnRoute(GeoPoint fromPoint, Edge from, GeoPoint toPoint, Edge to)
     {
         GeoPoint fromProject = from.projectFrom(fromPoint);
         GeoPoint toProject = to.projectFrom(toPoint);
         if (from == to)
         {
             List<GeoPoint> points = new ArrayList<GeoPoint>();
             points.add(fromProject);
             points.add(toProject);
         }
         else if (from.End() == to.Start())
         {
             List<GeoPoint> points = new ArrayList<GeoPoint>();
             points.add(fromProject);
             points.add(from.End().ToPoint());
             points.add(toProject);
         }
         return 0.0;
     }

     public Collection<Edge> GetEnumerator()
     {
         return this.edges;
     }
}
