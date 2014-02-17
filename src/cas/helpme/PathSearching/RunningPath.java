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

import org.apache.log4j.Logger;

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
	
	private final static Logger log = Logger.getLogger(RunningPath.class);
	private Graph graph;
	private HashSet<Edge> edges;
	private List<Road> roads = new ArrayList<Road>();
	private List<Path> shortestPaths = new ArrayList<Path>();
	private GeoPoint spot;
	
	private Edge targetEdge;		//用户所在的Edge或离用户最近Edge
	private Vertex projectPoint;	//用户所在位置在targetEdge上的投影点【如果不是Graph上的点，则新建一个新点，ID=-1】
	private int segIdx=0;			//投影点所在segment的Id
	private double lengthToStart; //用户所在位置离targetEdge的起点距离
	private double lengthToEnd;	//用户所在位置离targetEdge的终点距离
	private List<Edge> adjacentEdgesOfProjectPoint = null;		// 如果ProjectPoint是targetEdge上的某一点，则以该投影点端点，分别造两条到targetEdge起点和终点的Edge, ID分别为-1,-2
	
	final static int MAX_RADIUS_FOR_ROAD = 1000;
    final static int RADIUS_FOR_ROAD = 300;
    final static int MAX_RADIUS_FOR_USER = 100;
    final static int RADIUS_FOR_USER = 40;
    final static int MAX_RADIUS_FOR_FACILITY = 5000;
    final static int RADIUS_FOR_FACILITY = 1200;
    final static double MAX_EXPAND_LEN = 1000;
    final static double MIN_ROAD_LEN = 50;						// 恢复出的主干道的允许的最小长度【针对交叉路口上起连接干道作用的短Edge】
    final static double MIN_EDGE_ANGLE = Math.cos(Math.PI/6);		//判断是否为同一条路的夹角阈值
	
	public RunningPath(Graph g)
	{
		this.graph = g;
	}
	
	public void search(GeoPoint spot)
	{	
		this.spot = spot;
		// TODO 查询候选的edges;
		log.info("正在查询候选的干道Edges...");
		this.edges = graph.RangeQuery(this.spot, RADIUS_FOR_ROAD, MAX_RADIUS_FOR_ROAD, true);
		printCandidateEdges(this.edges);
		
		// TODO 识别恢复出干道;
		log.info("正在识别干道...");
		this.mainRoadRecovery();
		this.printCandidateRoads();
		
		// TODO 确定用户所在的Edge
		log.info("正在确定用户所在的Edge...");
		this.locateUserPosition();
		this.printTargetEdge();
		
		// TODO 搜索从用户所在位置到主干道的最短路径
		log.info("正在搜索从用户所在位置到主干道的最短路径...");
		this.searchingShortestPath(this.projectPoint);
		
		// TODO 计算步行成本
		log.info("正在计算前往各条干道的最短路径上的行走成本...");
		this.calculateTimeCostOfT1();
		this.printSerchingResults();
		
		this.edges = null;
		this.roads = null;
		this.shortestPaths = null;
	}
	
	/**
	 * 打印用户所在的Edge
	 */
	public void printTargetEdge(){
		System.out.println("用户所在的Edge:");
		System.out.println("ID : " + targetEdge.ID() + "\t" + targetEdge.Kind() + "\t" + targetEdge.Width() + "\t" + targetEdge.Name() + "\t" + targetEdge.PathClass() + "\t" + targetEdge.Length() + "\t" + targetEdge.CarrayAble() + "\t<" + targetEdge.Start().getLat() + ", " + targetEdge.Start().getLng() + ">--><" + targetEdge.End().getLat() + ", " + targetEdge.End().getLng() + ">");
		System.out.println("到起点的距离" + this.lengthToStart);
		System.out.println("到终点的距离" + this.lengthToEnd);
	}
	
	/**
	 * 输出延拓出的干道
	 */
	public void printCandidateRoads()
	{
		System.out.println("候选road:");
		for(Road road : this.roads)
			System.out.println(road.toString());
	}
	
	/**
	 * 输出候选的Edge
	 */
	public static void printCandidateEdges(HashSet<Edge>edges){
		System.out.println("候选Edges:");
		for(Edge edge : edges){
			System.out.println("ID : " + edge.ID() + "\t" + edge.Kind() + "\t" + edge.Width() + "\t" + edge.Name() + "\t" + edge.PathClass() + "\t" + edge.Length() + "\t" + edge.CarrayAble() + "\t<" + edge.Start().getLat() + ", " + edge.Start().getLng() + ">--><" + edge.End().getLat() + ", " + edge.End().getLng() + ">");
		}
	}
	
	/**
	 * 打印搜索结果
	 */
	public void printSerchingResults()
	{
		int resultCount = 0;
		if(this.shortestPaths != null && this.shortestPaths.size() > 0)
			resultCount = this.shortestPaths.size();
		System.out.println("共找到" + resultCount + "条主干道");
		for(int i=0; i<resultCount; i++)
		{
			Path path = this.shortestPaths.get(i);
			System.out.println("主干道" + i);
			System.out.println("\t" + path.getRoad().toString());
			System.out.println("\t最短路径:");
			System.out.println("\t\t" + path.toString());
		}
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
		Map<Vertex, DijkstraObject> s = new HashMap<Vertex, DijkstraObject>();		// 已经确定了最短路径的点集
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
//			if(v == this.targetEdge.End())		//如果在路径上经过targetEdge的终点，则说明该路径从targetEdge终点开始即可，无需从startPoint开始
//				v = null;
		}
		
		// 生成path对象
		List<Edge> edges = new ArrayList<Edge>();
		List<Vertex> vertexs = new ArrayList<Vertex>();
		
		vertexs.add(vertexStack.pop());
		while(!vertexStack.empty()){
			edges.add(edgeStack.pop());
			vertexs.add(vertexStack.pop());
		}
		
		for(Road road : destinations)
		{
			Path path = new Path(road, edges, vertexs);
			if(edges.size() == 0)	//如果请求服务的点spot就在主干道上
			{
				path.setDestinationEdge(this.targetEdge);
				path.setDestinationSegIdx(this.segIdx);
			}
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
		Vertex v = vertex;
		if(vertex.getId() == -1)
		{
			//如果vertex是projectPoint
			v = this.targetEdge.Start();
		}
		for(Road road : this.roads)
		{
			if(road.containsVertex(v))
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
			this.projectPoint = targetEdge.Start();
			this.segIdx = 0;
		}
		else if(projectType > 0)
		{
			// 超出终点
			lengthToStart = targetEdge.Length();
			this.projectPoint = targetEdge.End();
			this.segIdx = targetEdge.getGeo().Points().size() - 2;
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
			this.projectPoint = new Vertex(-1, projectPosition.getLat(), projectPosition.getLng());
			this.segIdx = projectSegId.segIdx;
			// 造两条分别到起点和终点的新边
			this.adjacentEdgesOfProjectPoint = new ArrayList<Edge>();
			this.adjacentEdgesOfProjectPoint.add(new Edge(-1, this.targetEdge.Start(), this.projectPoint, this.lengthToStart));
			this.adjacentEdgesOfProjectPoint.add(new Edge(-2, this.projectPoint, this.targetEdge.End(), this.lengthToEnd));
			this.projectPoint.setAdjacentEdges(adjacentEdgesOfProjectPoint);
		}
	}
	
	private void mainRoadRecovery()
	{
		Iterator<Edge> iter = this.edges.iterator();
		
		while(iter.hasNext())
		{
			Edge edge = iter.next();
			if(edge.ID() == 59567212065L){
				int a = 0;
			}
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
				if(road.length() > MIN_ROAD_LEN)
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
		{
			Vector vec = new Vector(edge.getGeo().Points().get(0), edge.getGeo().Points().get(1));
			List<GeoPoint> points = inEdges.get(0).getGeo().Points();
			Vector eVector = new Vector(points.get(points.size()-2), points.get(points.size()-1));
			double cos = vec.CosinWith(eVector);
			if(cos > MIN_EDGE_ANGLE)
			{
				prevEdge = inEdges.get(0);
			}
		}
		else
		{
			//选择同方向夹角最小的Edge
			Vector vec = new Vector(edge.getGeo().Points().get(0), edge.getGeo().Points().get(1));
			double maxCos = 0;
			for(Edge e : inEdges)
			{
//				if(e.Width() == edge.Width() && e.Kind().equals(edge.Kind()) && e.PathClass() == edge.PathClass()){
					List<GeoPoint> points = e.getGeo().Points();
					Vector eVector = new Vector(points.get(points.size()-2), points.get(points.size()-1));
					double cos = vec.CosinWith(eVector);
					if(cos > maxCos && cos > MIN_EDGE_ANGLE)
					{
						maxCos = cos;
						prevEdge = e;
					}
//				}
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
		{
			List<GeoPoint> segPoints = edge.getGeo().Points(); 
			Vector vec = new Vector(segPoints.get(segPoints.size()-2), segPoints.get(segPoints.size()-1));
			Vector eVector = new Vector(outEdges.get(0).getGeo().Points().get(0), outEdges.get(0).getGeo().Points().get(1));
			double cos = vec.CosinWith(eVector);
			if(cos > MIN_EDGE_ANGLE)
			{
				nextEdge = outEdges.get(0);
			}
		}
		else
		{
			//选择同方向夹角最小的Edge
			List<GeoPoint> segPoints = edge.getGeo().Points(); 
			Vector vec = new Vector(segPoints.get(segPoints.size()-2), segPoints.get(segPoints.size()-1));
			double maxCos = 0;
			for(Edge e : outEdges)
			{
//				if(e.Width() == edge.Width() && e.Kind().equals(edge.Kind()) && e.PathClass() == edge.PathClass()){
					Vector eVector = new Vector(e.getGeo().Points().get(0), e.getGeo().Points().get(1));
					double cos = vec.CosinWith(eVector);
					if(cos > maxCos && cos > MIN_EDGE_ANGLE)
					{
						maxCos = cos;
						nextEdge = e;
					}
//				}
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



