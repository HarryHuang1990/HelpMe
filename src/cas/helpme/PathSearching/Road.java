package cas.helpme.PathSearching;

import java.util.List;

import cas.taxiPredict.trjTools.roadNetwork.Edge;
import cas.taxiPredict.trjTools.roadNetwork.Vertex;

/**
 * Main Road
 * @author Harry Huang
 * @date 2014.2.10
 *
 */
public class Road {
	private List<Edge> edges;
	private List<Vertex> vertexs;
	
	public Road(List<Edge>edges, List<Vertex>vertexs){
		this.edges = edges;
		this.vertexs = vertexs;
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

	public boolean containsVertex(Vertex vertex){
		return vertexs.contains(vertex);
	}
	
	public boolean containsEdge(Edge edge){
		return edges.contains(edges);
	}
	
}
