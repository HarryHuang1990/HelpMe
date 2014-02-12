package cas.helpme.PathSearching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import cas.taxiPredict.pathIdentify.outWrapper.SegIdxObject;
import cas.taxiPredict.trjTools.roadNetwork.Edge;
import cas.taxiPredict.trjTools.roadNetwork.GeoPoint;
import cas.taxiPredict.trjTools.roadNetwork.Graph;
import cas.taxiPredict.trjTools.roadNetwork.Vertex;

/**
 * searching the shortest path from position for help to one main road.
 * @author Harry Huang
 * @date 2014.2.10
 *
 */
public class RunningPath {
	
	private Graph graph;
	private HashSet<Edge> edges;
	private List<Road> roads;
	private List<Path> shortestPaths;
	private GeoPoint spot;
	
	private Edge targetEdge;	//用户所在的Edge或离用户最近Edge
	private double lengthToStart; //用户所在位置离targetEdge的起点距离
	private double lengthToEnd;	//用户所在位置离targetEdge的终点距离
	
	final static int MAX_RADIUS_FOR_ROAD = 1000;
    final static int RADIUS_FOR_ROAD = 100;
    final static int MAX_RADIUS_FOR_USER = 100;
    final static int RADIUS_FOR_USER = 40;
    final static int MAX_RADIUS_FOR_FACILITY = 5000;
    final static int RADIUS_FOR_FACILITY = 1200;
    final static double MAX_EXPAND_LEN = 1000;
    final static double MIN_EDGE_ANGLE = Math.cos(20.0);		//判断是否为同一条路的夹角阈值
	
	public RunningPath(Graph g)
	{
		this.graph = g;
	}
	
	public void run()
	{
		// TODO 查询候选的edges;
		this.edges = graph.RangeQuery(spot, RADIUS_FOR_ROAD, MAX_RADIUS_FOR_ROAD, true);
		
		// TODO 识别恢复出干道;
		this.mainRoadRecovery();
		
		// TODO 确定用户所在的Edge
		this.locateUserPosition();
		
		// TODO 搜索从用户所在位置到主干道的最短路径
		this.searchingShortestPath(this.targetEdge.Start());
		
		// TODO 计算步行成本
		this.calculateTimeCostOfT1();
	}
	
	public List<Path> getShortestPaths() {
		return shortestPaths;
	}

	/**
	 * 计算各条最短路径上的时间代价
	 */
	private void calculateTimeCostOfT1()
	{
		for(Path path : this.shortestPaths)
		{
			path.calculateRunningTime();
		}
	}
	
	/**
	 * 从targetEdge的StartPoint为起点，搜索到各条干道的最短路径
	 * @param startPoint
	 */
	private void searchingShortestPath(Vertex startPoint)
	{
		Map<Vertex, DijkstraObject> s = new HashMap<Vertex, DijkstraObject>();		// 确定最短路径的点集
		Map<Vertex, DijkstraObject> q = new HashMap<Vertex, DijkstraObject>();
		Comparator<Entry<Vertex, DijkstraObject>> comparator = new Comparator<Entry<Vertex, DijkstraObject>>(){
			@Override
			public int compare(Entry<Vertex, DijkstraObject> arg0, Entry<Vertex, DijkstraObject> arg1) 
			{
				double r = arg0.getValue().getShortestDistance() - arg1.getValue().getShortestDistance();
				if(r > 0) return 1;
				else if(r < 0) return -1;
				else return 0;
			}
		};
		// 初始化s和q
		s.put(startPoint, new DijkstraObject(startPoint, 0, null, null));
		this.expandEdgeInDijkstra(startPoint, s, q);
		List<Road> des = this.isTheVertexOfMainRoad(startPoint);
		if(des.size() != 0)
		{
			this.roads.removeAll(des);
			this.getShortestPath(des, startPoint, s);
		}
		// 开始搜索
		while(!this.roads.isEmpty())
		{
			Vertex minVertex = this.extractMin(q, comparator);
			s.put(minVertex, q.get(minVertex));
			q.remove(minVertex);
			
			des = this.isTheVertexOfMainRoad(minVertex);
			if(des.size() != 0)
			{
				this.roads.removeAll(des);
				this.getShortestPath(des, minVertex, s);
			}
			
			this.expandEdgeInDijkstra(minVertex, s, q);
		}
		
	}
	
	/**
	 * 从s中恢复出Spot到干道的最短路径
	 * @param destinations
	 * @param endVector
	 * @param s
	 * @return
	 */
	private void getShortestPath(List<Road> destinations, Vertex endVertex, Map<Vertex, DijkstraObject> s)
	{
		//TODO 回溯恢复出最短路径
		Stack<Edge>edgeStack = new Stack<Edge>();
		Stack<Vertex>vertexStack = new Stack<Vertex>();
		
		vertexStack.add(endVertex);
		Vertex v = endVertex; 
		while(v != null)
		{
			DijkstraObject dijObj = s.get(v);
			if(dijObj.getPreviousVertex() != null)
			{
				vertexStack.push(dijObj.getPreviousVertex());
				edgeStack.push(dijObj.getPreviousEdge());
			}
			v = dijObj.getPreviousVertex();
			if(v == this.targetEdge.End())		//如果在路径上经过targetEdge的终点，则说明该路径从targetEdge终点开始即可，无需从startPoint开始
				v = null;
		}
		
		// 生成path对象
		for(Road road : destinations)
		{
			Path path = new Path(road);
			List<Edge> edges = new ArrayList<Edge>();
			List<Vertex> vertexs = new ArrayList<Vertex>();
			
			vertexs.add(vertexStack.pop());
			while(!vertexStack.empty()){
				edges.add(edgeStack.pop());
				vertexs.add(vertexStack.pop());
			}
			
			path.setVertexs(vertexs);
			path.setEdges(edges);
			this.shortestPaths.add(path);
		}
		
	}
	
	
	/**
	 * 判断是否是干道上的点。
	 * 由于存在一个点可能是多条干道交叉点，因此返回的是命中的road集合
	 * @return
	 */
	private List<Road> isTheVertexOfMainRoad(Vertex vertex)
	{
		List<Road> reachedRoads = new ArrayList<Road>();
		for(Road road : this.roads)
		{
			if(road.containsVertex(vertex))
			{
				reachedRoads.add(road);
			}
		}
		return reachedRoads;
	}
	
	/**
	 *  从Vertex开始拓展边
	 * @param vertex
	 * @param s
	 * @param q
	 */
	public void expandEdgeInDijkstra(Vertex vertex, Map<Vertex, DijkstraObject> s, Map<Vertex, DijkstraObject> q)
	{
		List<Edge> adjacentEdges = vertex.getAdjacentEdges();
		for(Edge e : adjacentEdges)
		{
			//确定相邻节点
			Vertex next = null;
			if(e.End() == vertex)
				next = e.Start();
			else
				next = e.End();
			//拓展节点
			if(!s.containsKey(next))
			{
				double curDistance = s.get(vertex).getShortestDistance() + e.Length();
				if(q.containsKey(next))
				{
					if(curDistance < q.get(next).getShortestDistance())
					{
						q.get(next).setPreviousVertex(vertex);
						q.get(next).setPreviousEdge(e);
						q.get(next).setShortestDistance(curDistance);
					}
				}
				else
				{
					DijkstraObject dijkstraObject = new DijkstraObject(next, curDistance, vertex, e);
					q.put(next, dijkstraObject);
				}
			}
		}
	}
	/**
	 *  从q中取距离最短的点
	 * @param q
	 * @param comparator
	 * @return
	 */
	private Vertex extractMin(Map<Vertex, DijkstraObject> q, Comparator<Entry<Vertex, DijkstraObject>> comparator)
	{
		List<Entry<Vertex, DijkstraObject>> list = new ArrayList<Entry<Vertex, DijkstraObject>>(q.entrySet());
		Collections.sort(list, comparator);
		return list.get(0).getKey();
	}
	
	
	
	
	private void locateUserPosition()
	{
		// 定位用户所在的位置
		HashSet<Edge> pEdges = graph.RangeQuery(this.spot, RADIUS_FOR_USER, MAX_RADIUS_FOR_USER, false);
		double minDis = Double.POSITIVE_INFINITY;
		targetEdge = null;
		GeoPoint projectPosition = null;
		SegIdxObject projectSegId = null;
		int projectType = -1;
		for(Edge e : pEdges)
		{
			SegIdxObject segIdxObj=new SegIdxObject(0);
	        GeoPoint result= GeoPoint.INVALID();
	        int type = e.getGeo().ProjectFrom(this.spot,  result,  segIdxObj);
	        double distance = GeoPoint.GetDistance2(this.spot, result);
	        if(distance < minDis)
	        {
	        	minDis = distance;
	        	targetEdge = e;
	        	projectPosition = result;
	        	projectSegId = segIdxObj;
	        	projectType = type;
	        }
		}
		
		//TODO 求用户所在点到两个端点的距离
		lengthToStart = 0;
		lengthToEnd = 0;
		
		if(projectType < 0)
		{
			// 超出起点
			lengthToEnd = targetEdge.Length();
		}
		else if(projectType > 0)
		{
			// 超出终点
			lengthToStart = targetEdge.Length();
		}
		else
		{
			// 判断离edge首尾两个节点中哪个节点更近
			List<GeoPoint> points = targetEdge.getGeo().Points();
			if((1.0 * (projectSegId.segIdx+1))/(points.size()-1) <= 0.5)
			{
				// 离起点近
				for(int i=0; i<projectSegId.segIdx; i++)
				{
					lengthToStart += GeoPoint.GetPreciseDistance(points.get(i), points.get(i+1));
				}
				lengthToStart += GeoPoint.GetPreciseDistance(points.get(projectSegId.segIdx), projectPosition);
				lengthToEnd = targetEdge.Length() - lengthToStart;
			}
			else
			{
				// 离终点近
				for(int i = projectSegId.segIdx + 1; i<points.size()-1; i++)
				{
					lengthToEnd += GeoPoint.GetPreciseDistance(points.get(i), points.get(i+1));
				}
				lengthToEnd += GeoPoint.GetPreciseDistance(projectPosition, points.get(projectSegId.segIdx + 1));
				lengthToStart += targetEdge.Length() - lengthToEnd;
			}
		}
	}
	
	private void mainRoadRecovery()
	{
		Iterator<Edge> iter = this.edges.iterator();
		
		while(iter.hasNext())
		{
			Edge edge = iter.next();
			if(!this.isIdentified(edge))
			{
				List<Edge> edgesFromIn = this.expandRoadFromInEdges(edge, 0);
				List<Edge> edgesFromOut = this.expandRoadFromOutEdges(edge, 0);
				List<Edge> rEdges = new ArrayList<Edge>();
				rEdges.addAll(edgesFromIn);
				rEdges.add(edge);
				rEdges.addAll(edgesFromOut);
				List<Vertex> vertexs = new ArrayList<Vertex>();
				for(int i=0; i<rEdges.size(); i++)
				{
					if(i==0)
						vertexs.add(rEdges.get(i).Start());
					vertexs.add(rEdges.get(i).End());
				}
				Road road = new Road(rEdges, vertexs);
				this.roads.add(road);
			}
		}
	}
	
	private List<Edge> expandRoadFromInEdges(Edge startEdge, double length)
	{
		List<Edge> roadEdges = new ArrayList<Edge>();
		List<Edge> inEdges = startEdge.getInEdges();
		
		Edge prevEdge = getPrevEdge(inEdges, startEdge);
		if(prevEdge != null)
		{
			length += prevEdge.Length();
			if(length < MAX_EXPAND_LEN)
				roadEdges.addAll(this.expandRoadFromInEdges(prevEdge, length));
			roadEdges.add(prevEdge);
		}
		
		return roadEdges;
		
	}
 	
	private List<Edge> expandRoadFromOutEdges(Edge startEdge, double length)
	{
		List<Edge> roadEdges = new ArrayList<Edge>();
		List<Edge>outEdges = startEdge.getOutEdges();
		
		Edge nextEdge = getNextEdge(startEdge, outEdges);
		if(nextEdge != null)
		{
			length += nextEdge.Length();
			roadEdges.add(nextEdge);
			if(length < MAX_EXPAND_LEN)
				roadEdges.addAll(this.expandRoadFromOutEdges(nextEdge, length));
		}
		
		return roadEdges;
	}
	
	private Edge getPrevEdge(List<Edge>inEdges, Edge edge)
	{
		Edge prevEdge = null;
		if(inEdges == null || inEdges.size() == 0)
			prevEdge = null;
		else if(inEdges.size() == 1)
			prevEdge = inEdges.get(0);
		else
		{
			//选择同方向夹角最小的Edge
			Vector vec = new Vector(edge.getGeo().Points().get(0), edge.getGeo().Points().get(1));
			double maxCos = 0;
			for(Edge e : inEdges)
			{
				if(e.Width() == edge.Width() && e.Kind().equals(edge.Width()) && e.PathClass() == edge.PathClass()){
					List<GeoPoint> points = e.getGeo().Points();
					Vector eVector = new Vector(points.get(points.size()-2), points.get(points.size()-1));
					double cos = vec.CosinWith(eVector);
					if(cos > maxCos && cos > MIN_EDGE_ANGLE)
					{
						maxCos = cos;
						prevEdge = e;
					}
				}
			}
		}
		return prevEdge;
	} 
	
	private Edge getNextEdge(Edge edge, List<Edge>outEdges)
	{
		Edge nextEdge = null;
		if(outEdges == null || outEdges.size() == 0)
			nextEdge = null;
		else if(outEdges.size() == 1)
			nextEdge = outEdges.get(0);
		else
		{
			//选择同方向夹角最小的Edge
			List<GeoPoint> segPoints = edge.getGeo().Points(); 
			Vector vec = new Vector(segPoints.get(segPoints.size()-2), segPoints.get(segPoints.size()-1));
			double maxCos = 0;
			for(Edge e : outEdges)
			{
				if(e.Width() == edge.Width() && e.Kind().equals(edge.Width()) && e.PathClass() == edge.PathClass()){
					Vector eVector = new Vector(e.getGeo().Points().get(0), e.getGeo().Points().get(1));
					double cos = vec.CosinWith(eVector);
					if(cos > maxCos && cos > MIN_EDGE_ANGLE)
					{
						maxCos = cos;
						nextEdge = e;
					}
				}
			}
		}
		return nextEdge;
	}
	
	private boolean isIdentified(Edge edge)
	{
		boolean flag = false;
		for(Road road : this.roads)
		{
			if(road.containsEdge(edge))
			{
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	public static void main(String args[]){
		System.out.println((short)(-3/2));
	}

}



