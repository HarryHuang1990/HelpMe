package cas.helpme.PathSearching;

import cas.taxiPredict.trjTools.roadNetwork.Edge;
import cas.taxiPredict.trjTools.roadNetwork.Vertex;

/**
 * 
 * 用于记录Dijkstra计算过程中各个节点信息的对象
 * @author Harry Huang
 * @date 2014.2.11
 *
 */
public class DijkstraObject {
	private Vertex vertex;				//当前点
	private double shortestDistance;	//当前点到起点的最短路程
	private Vertex previousVertex;		//最短路径上当前点的前向节点
	private Edge previousEdge;			//前向节点于当前节点之间的边
	
	public DijkstraObject(Vertex vertex, double shortestDistance, Vertex previousVertex, Edge previousEdge)
	{
		this.vertex = vertex;
		this.shortestDistance = shortestDistance;
		this.previousVertex = previousVertex;
		this.previousEdge = previousEdge;
	}
	
	public DijkstraObject(Vertex vertex, double shortestDistance)
	{
		this.vertex = vertex;
		this.shortestDistance = shortestDistance;
	}

	public Vertex getVertex() {
		return vertex;
	}

	public void setVertex(Vertex vertex) {
		this.vertex = vertex;
	}

	public double getShortestDistance() {
		return shortestDistance;
	}

	public void setShortestDistance(double shortestDistance) {
		this.shortestDistance = shortestDistance;
	}

	public Vertex getPreviousVertex() {
		return previousVertex;
	}

	public void setPreviousVertex(Vertex previousVertex) {
		this.previousVertex = previousVertex;
	}
	
	public Edge getPreviousEdge() {
		return previousEdge;
	}

	public void setPreviousEdge(Edge previousEdge) {
		this.previousEdge = previousEdge;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((previousVertex == null) ? 0 : previousVertex.hashCode());
		long temp;
		temp = Double.doubleToLongBits(shortestDistance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((vertex == null) ? 0 : vertex.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DijkstraObject other = (DijkstraObject) obj;
		if (previousVertex == null) {
			if (other.previousVertex != null)
				return false;
		} else if (!previousVertex.equals(other.previousVertex))
			return false;
		if (Double.doubleToLongBits(shortestDistance) != Double
				.doubleToLongBits(other.shortestDistance))
			return false;
		if (vertex == null) {
			if (other.vertex != null)
				return false;
		} else if (!vertex.equals(other.vertex))
			return false;
		return true;
	}
	
}
