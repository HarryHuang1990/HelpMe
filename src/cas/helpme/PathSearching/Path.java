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
	// mean speed for running (m/s)
	public final static double runningSpeed = 1.2;
	// mean speed for driving (m/s = 40km/h)
	public final static double drivingSpeed = 11.1;
	
	public Path(Road road) {
		this.road = road;
	}

	public Path(Road road, List<Edge> edges, List<Vertex> vertexs) {
		this.road = road;
		this.edges = edges;
		this.vertexs = vertexs;
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
