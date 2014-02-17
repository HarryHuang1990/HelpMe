package cas.helpme.PathSearching;

import cas.taxiPredict.trjTools.roadNetwork.Edge;

/**
 * 干道上到达点的信息
 * 包括
 * 	1. 在哪条Edge上
 * 	2. 在Edge上的第几个segment
 * @author Harry Huang
 * @date 2014.2.17
 *
 */
public class RoadDestination {
	private Edge edge = null;
	private int segIdx = -1;
	
	
	public RoadDestination() {
		super();
	}
	public RoadDestination(Edge edge, int segIdx) {
		super();
		this.edge = edge;
		this.segIdx = segIdx;
	}
	public Edge getEdge() {
		return edge;
	}
	public void setEdge(Edge edge) {
		this.edge = edge;
	}
	public int getSegIdx() {
		return segIdx;
	}
	public void setSegIdx(int segIdx) {
		this.segIdx = segIdx;
	}
}
