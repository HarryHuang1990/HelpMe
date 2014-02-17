package cas.taxiPredict.trjTools.roadNetwork;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import cas.helpme.PathSearching.RunningPath;
import cas.taxiPredict.pathIdentify.base.Constants;
import cas.taxiPredict.pathIdentify.base.StringUtils;
import cas.taxiPredict.pathIdentify.grid.GridEdge;
import cas.taxiPredict.pathIdentify.grid.GridPoint;
import cas.taxiPredict.trjTools.algorithm.AStar;

public class Graph {

	 private HashMap<Long, Edge> edges;

     public HashMap<Long, Edge> getEdges()
     {
         return edges; 
     }
     private HashMap<Long, Vertex> vertices;

     public HashMap<Long, Vertex> getVertices()
     {
          return vertices; 
     }
     
     private GridEdge edgeIndex = null;

     public GridEdge getEdgeIndex()
     {
         if (this.edgeIndex == null)
         {
             this.edgeIndex = new GridEdge(edges.values(), mbr, edgeCellSize);
         }
         return edgeIndex; 
     }
     private GridPoint vertexIndex;
     private MBR mbr;
     private double edgeCellSize = 500 * Constants.D_PER_M;
     private double vertexCellSize = 100 * Constants.D_PER_M;
     private long base_id = 100000000000L;
     
     public Graph(String vertexFile, String edgeFile, String geometryFile) throws NumberFormatException, IOException
     {
    	 long beginTimeStamp=System.currentTimeMillis();
         loadVertices(vertexFile);
         System.out.printf("Vertex:%dms\n",(System.currentTimeMillis()-beginTimeStamp));
         loadEdges(edgeFile);
         System.out.printf("Edge:%dms\n",(System.currentTimeMillis()-beginTimeStamp));

         if (geometryFile != null)
         {
             loadGeometry(geometryFile);
         }
         System.out.printf("Gem:%dms\n", (System.currentTimeMillis()-beginTimeStamp));
         //Console.ReadLine();
         buildRNIndex();
         System.out.printf("Index:%dms\n", (System.currentTimeMillis()-beginTimeStamp));

     }
     public Graph(String vertexFile, String edgeFile) throws NumberFormatException, IOException
     {
    	 long beginTimeStamp=System.currentTimeMillis();
         loadVertices(vertexFile);
         System.out.printf("Vertex:%dms\n",(System.currentTimeMillis()-beginTimeStamp));
         loadEdges(edgeFile);
         System.out.printf("Edge:%dms\n",(System.currentTimeMillis()-beginTimeStamp));
         System.out.printf("Gem:%dms\n", (System.currentTimeMillis()-beginTimeStamp));
         //Console.ReadLine();
         buildRNIndex();
         System.out.printf("Index:%dms\n", (System.currentTimeMillis()-beginTimeStamp));
     }
     
   /// Get the edge with a distance roughly lower than radius from point p 
     public HashSet<Edge> RangeQuery(GeoPoint p, double radius)
     {
         return this.getEdgeIndex().RangeQuery(p, radius);
     }
   /// Get the edge with a distance roughly lower than radius from point p 
     public HashSet<Edge> RangeQuery(GeoPoint p, double radius, double maxRadius, int minSize, boolean filter)
     {
         HashSet<Edge> result = null;
         while (radius <= maxRadius && (result == null || result.size() <= minSize))
         {
             result = RangeQuery(p, radius);
//             RunningPath.printCandidateEdges(result);
             if(filter)
             {
            	 result = this.filterEdge(result);
             }
             radius *= 2;
         }
         return result;
     }
     public HashSet<Edge> RangeQuery(GeoPoint p, double radius, double maxRadius)
     {
    	 return RangeQuery(p,radius,maxRadius,0,false);
     }

     public HashSet<Edge> RangeQuery(GeoPoint p, double radius, double maxRadius, boolean filter){
    	 return RangeQuery(p,radius,maxRadius,0,filter);
     }
     
     /**
      * filter out the certain class of edge.
      * @return
      */
     private HashSet<Edge> filterEdge(HashSet<Edge> result)
     {
    	 Iterator<Edge> iter = result.iterator();
    	 while(iter.hasNext())
    	 {
    		Edge cand = iter.next();
    		if(!((cand.PathClass() == 2 || cand.PathClass() == 3) ))
    			iter.remove();
    	 }
    	 return result;
     }
     
   /// Get the vertex with a mbr
     public HashSet<Vertex> VertexRangeQuery(MBR rect)
     {
         return this.vertexIndex.RangeQuery(rect);
     }
   /// Get the vertex with a mbr
     public HashSet<Vertex> VertexRangeQuery(GeoPoint p, double radius)
     {
         double minLat, minLng, maxLat, maxLng;
         double d_radius = radius * Constants.D_PER_M;	//radius in degree
         minLng = p.getLng() - d_radius;
         maxLng = p.getLng() + d_radius;
         minLat = p.getLat() - d_radius;
         maxLat = p.getLat() + d_radius;
         MBR rect = new MBR(minLng, minLat, maxLng, maxLat);
         return this.vertexIndex.RangeQuery(rect);
     }
     
     private void loadVertices(String fileName) throws NumberFormatException,IOException
     {
         this.mbr = MBR.EMPTY();
         //id,lng,lat
         vertices = new HashMap<Long, Vertex>();
         FileInputStream dataInputStream= new FileInputStream(fileName);
         InputStreamReader read = new InputStreamReader(dataInputStream);
 		 BufferedReader reader = new BufferedReader(read);
 		String Line = new String();
		while ((Line = reader.readLine()) != null)
		{
			List<String> fields= StringUtils.SimpleSplit(Line, ',');
			long id = Long.valueOf(fields.get(0));
            double lng = Double.valueOf(fields.get(1));
            double lat = Double.valueOf(fields.get(2));
            Vertex v = new Vertex(id, lat, lng);
            vertices.put(id, v);
            this.mbr.Include(new GeoPoint(lat, lng));
		}
		reader.close();
		read.close();
		dataInputStream.close();
     }
     
     private void loadEdges(String fileName) throws NumberFormatException, IOException
     {
         edges = new HashMap<Long, Edge>();
         FileInputStream dataInputStream= new FileInputStream(fileName);
         InputStreamReader read = new InputStreamReader(dataInputStream);
 		 BufferedReader reader = new BufferedReader(read);
 		String Line = new String();
		while ((Line = reader.readLine()) != null)
		{
			List<String> fields= StringUtils.SimpleSplit(Line, ',');
			long id = Long.valueOf(fields.get(0));
            long startId = Long.valueOf(fields.get(1));
            long endId = Long.valueOf(fields.get(2));
            Vertex start = getVertices().get(startId);
            Vertex end = getVertices().get(endId);
            String kind = fields.get(3);
            int width = Integer.valueOf(fields.get(4));
            String name = fields.get(5);
            int pathClass = Integer.valueOf(fields.get(6));
            double length = Double.valueOf(fields.get(7)) * 1000;
            boolean carryAble = Boolean.valueOf(fields.get(8));
            
            Edge e = new Edge(id, start, end, kind, width, name, pathClass, length, carryAble);
            edges.put(id, e);
            start.RegisterEdge(e);
            end.RegisterEdge(e);
		}
		reader.close();
		read.close();
		dataInputStream.close();
     }
     
     /// Load geometry information of the edge
     private void loadGeometry(String fileName) throws NumberFormatException, IOException
     {
    	 FileInputStream dataInputStream= new FileInputStream(fileName);
         InputStreamReader read = new InputStreamReader(dataInputStream);
 		 BufferedReader reader = new BufferedReader(read);
 		String Line = new String();
		while ((Line = reader.readLine()) != null)
		{
			String[] fields= Line.split(",", 2);
			/////List<String> fields= StringUtils.SimpleSplit(Line, '\t');
			long edgeId = Long.valueOf(fields[0]);
            Edge e = this.edges.get(edgeId);
            if (e!=null)
            {
                e.setGeoString(fields[1]);
            }
		}
		reader.close();
		read.close();
		dataInputStream.close();
     }
     
   /// Build grid index for road network
     private void buildRNIndex()
     {
         //this.edgeIndex = new GridEdge(edges.Values, mbr, edgeCellSize);
         this.vertexIndex = new GridPoint(vertices.values(), mbr, vertexCellSize);
     }
     
   /// Check if edge is single direction
     /// </summary>
     public void TestConnection()
     {
         for(Edge e : edges.values())
         {
             //Check if there exists an edge end->start
             List<Edge> outEdges = e.End().getOutEdges();
             boolean found = false;
             for (Edge e2 : outEdges)
             {
                 if (e2.End() == e.Start())
                 {
                     found = true;
                     log.info(e+", "+e2);
                     break;
                 }
             }
             if (!found)
             {
                 //logger.Info(String.Format("Reverse Edge of {0}  not found!", e.ID));
             }
         }
     }
     
     public void CleanAndSaveEdgeData(String edgeFileName) throws IOException
     {
         List<Edge> newEdges = new ArrayList<Edge>();
         HashSet<Long> removedEdge = new HashSet<Long>();

         for (Edge e : edges.values())
         {
             //Check if there exists an edge end->start
             List<Edge> outEdges = e.End().getOutEdges();
             boolean found = false;
             for(Edge e2 : outEdges)
             {
                 if (e2.End() == e.Start())
                 {
                     found = true;
                     if (!removedEdge.contains(e2.ID()))
                     {
                         edges.remove(e2.ID());
                         removedEdge.add(e.ID());
                         removedEdge.add(e2.ID());
                         //logger.Info(String.Format("{0}, {1}", e, e2));
                         log.info("remove:" + e2);
                     }
                     break;
                 }
             }
         }
         //write new dictionary
         //int count = 1;
         //String edgeFileName = Constants.MAP_DIR + "beijing_e2.txt";
         //String edgeFileName = Constants.MAP_DIR + "beijing_e2.txt";
         FileOutputStream outputStream=new FileOutputStream(edgeFileName);
         OutputStreamWriter out = new OutputStreamWriter(outputStream);	
         MessageFormat lineMessageFormat= new MessageFormat("{0}\t{1}\t{2}\r\n");
 		for(Edge e : edges.values())
        {
 			out.write(lineMessageFormat.format(new Object[]{e.ID(), e.Start().getId(), e.End().getId()}));
 			out.write(lineMessageFormat.format(new Object[]{e.ID()+ base_id, e.End().getId(), e.Start().getId()}));
        }
 		out.flush();
 		out.close();
 		outputStream.close();
     }
     
     
//     public void SaveAsShpFile(String fileName)
//     {
//         //写入文件
//         String rootDir = new File(fileName).getParent();
//         String shapeFileName = FileUtil.getMinFileNameAndNoSuffix(fileName);
//         ShapeType shapeType = ShapeType.PolyLine;
//         DbfFieldDesc[] fields = new DbfFieldDesc[] 
//         { 
//             new DbfFieldDesc { FieldName = "ID", FieldType = DbfFieldType.Character, FieldLength = 14, RecordOffset = 0 },
//             //new DbfFieldDesc { FieldName = "length", FieldType = DbfFieldType.FloatingPoint, FieldLength = 10, RecordOffset = 14 },
//         };
//         ShapeFileWriter sfw = ShapeFileWriter.CreateWriter(rootDir, shapeFileName, shapeType, fields);
//         foreach (Edge e in Edges.Values)
//         {
//             String id = e.ID.ToString();
//             if (e.ID > this.base_id)
//             {
//                 //continue;
//             }
//             //String[] fieldData = new string[] { id, e.Length.ToString() };
//             String[] fieldData = new string[] { id };
//             List<PointF> vertices = new List<PointF>();
//             for (int i = 0; i < e.Geo.Points.Count; i++)
//             {
//                 float lng = (float)e.Geo.Points[i].Lng;
//                 float lat = (float)e.Geo.Points[i].Lat;
//                 vertices.Add(new PointF(lng, lat));
//             }
//             //vertices.Add(new PointF((float)e.Start.Lng, (float)e.Start.Lat));
//             //vertices.Add(new PointF((float)e.End.Lng, (float)e.End.Lat));
//             sfw.AddRecord(vertices.ToArray(), vertices.Count, fieldData);
//         }
//         sfw.Close();
//     }
     
     
      /// Find a path between two point using A* algorithm
     public List<Edge> FindPath(Vertex from, Vertex to)
     {
         AStar astar = new AStar(this);
         List<Edge> list = new ArrayList<Edge>();
         if (from != to)
         {
             list = astar.FindPath(from, to).getEdges();
         }
         return list;
     }
      /// Find a path between two point using A* algorithm
     public List<Edge> FindPath(Vertex from, Vertex to, double maxDist)
     {
         AStar astar = new AStar(this);
         EdgePath edgePath = null;
         List<Edge> list = null;
         if (from != to)
         {
             edgePath = astar.FindPath(from, to, maxDist);
             if(edgePath != null)
                 list = edgePath.getEdges();
         }
         return list;
     }
     public Polyline FindPath(Edge from, GeoPoint fromPoint, Edge to, GeoPoint toPoint)
     {
    	 return FindPath(from,fromPoint,to,toPoint,Double.MAX_VALUE);
     }
   /// Find a path between two point using A* algorithm
     public Polyline FindPath(Edge from, GeoPoint fromPoint, Edge to, GeoPoint toPoint, double maxDist)
     {
         Polyline route = null;
         GeoPoint fromProject = from.projectFrom(fromPoint);
         GeoPoint toProject = to.projectFrom(toPoint);
         if (from == to)
         {
             List<GeoPoint> points = new ArrayList<GeoPoint>();
             points.add(fromProject);
             points.add(toProject);
             route = new Polyline(points);
         }
         else if (from.End() == to.Start())
         {
             List<GeoPoint> points = new ArrayList<GeoPoint>();
             points.add(fromProject);
             points.add(from.End().ToPoint());
             points.add(toProject);
             route = new Polyline(points);
         }
         else
         {
             //Directed road only
             Vertex src = from.End();
             Vertex dest = to.Start();
             AStar astar = new AStar(this);
             EdgePath path = astar.FindPath(src, dest, maxDist);
             if (path != null && path.Count() > 0)
             {
                 //build route
                 List<GeoPoint> points = new ArrayList<GeoPoint>();
                 points.add(fromProject);
                 for (int i = 0; i < path.Count(); i++)
                 {
                     Edge e = path.get(i);
                     points.add(e.Start().ToPoint());
                 }
                 points.add(path.End().ToPoint());
                 points.add(toProject);
                 route = new Polyline(points);

             }
         }
         return route;
     }

     public boolean IsReachable(Vertex from, Vertex to, double maxDist)
     {
         boolean result = false;
         if (from != null && to != null)
         {
             if (from == to)
             {
                 result = true;
             }
             else
             {
                 AStar astar = new AStar(this);
                 EdgePath path = astar.FindPath(from, to, maxDist);
                 result = (path != null);
             }
         }
         return result;
     }
     public boolean IsReachable(Edge from, GeoPoint fromPoint, Edge to, GeoPoint toPoint)
     {
    	 return IsReachable(from, fromPoint,to, toPoint,Double.MAX_VALUE);
     }
     public boolean IsReachable(Edge from, GeoPoint fromPoint, Edge to, GeoPoint toPoint, double maxDist )
     {
         Polyline route = FindPath(from, fromPoint, to, toPoint, maxDist);
         boolean result = false;
         if (route != null && route.getLength() < maxDist)
         {
             result = true;
         }
         return result;
     }

     public HashSet<Edge> GetCandiateEdges(Vertex src, GeoPoint destPoint, double maxCost, double maxDist)
     {
         AStar astar = new AStar(this);
         return astar.GetCandiateEdges(src, destPoint, maxCost, maxDist);
     }
     
     
     private static final Logger log= Logger.getLogger(Graph.class);
}
