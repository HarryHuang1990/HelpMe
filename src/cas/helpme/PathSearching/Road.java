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
		return edges.contains(edge);
	}
	
	public double length(){
		double len = 0;
		for(Edge e : this.edges)
			len += e.Length();
		return len;
	}
	
	/**
	 * 确定给定的端点在该Road上的位置
	 * 包括所在的 Edge和segment id
	 * @param goalVertex
	 * @return
	 */
	public RoadDestination searchinPosition(Vertex goalVertex)
	{
		RoadDestination rd = null;
		int i=0;
		for(i=0; i<this.edges.size(); i++){
			
			if(this.edges.get(i).Start() == goalVertex)
			{
				rd = new RoadDestination(this.edges.get(i), 0);
			}
		}
		
		if(rd == null)
		{	//如果所有edge的起点都不匹配，则判断最后一个Edge的终点
			if(this.edges.get(i-1).End() == goalVertex)
			{
				rd = new RoadDestination(this.edges.get(i-1), this.edges.get(i-1).getGeo().Points().size()-2);
			}
		}
		return rd;
	}
	
	public String toString(){
		String output = edges.get(0).Name() + "\t";
		int i=0;
		for(i=0; i<edges.size(); i++){
			output += vertexs.get(i).getId() + ":<" + vertexs.get(i).getLat() + "," + vertexs.get(i).getLng() + "> -edgeID:" + edges.get(i).ID() + "(len:"+ edges.get(i).Length() +")- ";
		}
		output += vertexs.get(i).getId() + ":<" + vertexs.get(i).getLat() + "," + vertexs.get(i).getLng() + ">";
		return output;
	}
}
