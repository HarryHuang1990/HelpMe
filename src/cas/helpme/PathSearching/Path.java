package cas.helpme.PathSearching;

import java.util.List;

import cas.taxiPredict.trjTools.roadNetwork.Edge;
import cas.taxiPredict.trjTools.roadNetwork.Vertex;

/**
 * path to run
 * @author Harry Huang
 * @date 2014.2.10
 *
 */
public class Path {
	private Road road;
	private List<Edge> edges;
	private List<Vertex> vertexs;
	private double TimeCost = 0;
	private Edge destinationEdge = null;
	private int destinationSegIdx = -1;
	// mean speed for running (m/s)
	public final static double runningSpeed = 2.2;
	// mean speed for driving (m/s = 40km/h)
	public final static double drivingSpeed = 11.1;
	
	public String toString(){
		String output = "";
		output = "估计用时=" + TimeCost + "\t路径=";
		int i=0;
		for(i=0; i<edges.size(); i++)
		{
			output += vertexs.get(i).getId() + ":<" + vertexs.get(i).getLat() + "," + vertexs.get(i).getLng() + "> -" + edges.get(i).ID() + "- ";  
		}
		output += vertexs.get(i).getId() + ":<" + vertexs.get(i).getLat() + "," + vertexs.get(i).getLng() + ">\r\n";
		RoadDestination rd = this.getDestinationPosition();
		output += "\t\t到达点信息:Edge=" + rd.getEdge().ID() + ", segIdx=" + rd.getSegIdx();		
		return output;
	}

	public Path(){}
	
	public Path(Road road, List<Edge> edges, List<Vertex> vertexs) {
		this.road = road;
		this.edges = edges;
		this.vertexs = vertexs;
	}

	
	public Edge getDestinationEdge() {
		return destinationEdge;
	}

	public void setDestinationEdge(Edge destinationEdge) {
		this.destinationEdge = destinationEdge;
	}

	public int getDestinationSegIdx() {
		
		return destinationSegIdx;
	}

	public void setDestinationSegIdx(int destinationSegIdx) {
		this.destinationSegIdx = destinationSegIdx;
	}

	public Road getRoad() {
		return road;
	}

	public void setRoad(Road road) {
		this.road = road;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}

	public List<Vertex> getVertexs() {
		return vertexs;
	}

	public void setVertexs(List<Vertex> vertexs) {
		this.vertexs = vertexs;
	}

	public double getTimeCost() {
		return TimeCost;
	}

	public void setTimeCost(double timeCost) {
		TimeCost = timeCost;
	}
	
	/**
	 * 确定达到目的地所在的Edge和segIdx
	 */
	public RoadDestination getDestinationPosition(){
		RoadDestination rd = new RoadDestination(this.destinationEdge, this.destinationSegIdx);
		if(this.destinationEdge == null || this.destinationSegIdx == -1)
		{
			// 如果目的地跟事发点是同一个地方，则不需要搜索。以下搜索是针对非同一地点的情况
			rd = this.road.searchinPosition(this.vertexs.get(this.vertexs.size() - 1));
		}
		return rd;
	}

	public void calculateRunningTime(){
		double runningDistance = this.getPathLength();
		this.TimeCost = runningDistance / Path.runningSpeed;
	}
	
	public void calculateDrivingTime(){
		double drivingDistance = this.getPathLength();
		this.TimeCost = drivingDistance / Path.drivingSpeed;
	}
	
	public double getPathLength(){
		double length = 0;
		for (Edge edge : edges){
			length += edge.Length();
		}
		return length;
	}
}
