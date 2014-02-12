package cas.helpme.PathSearching;

import cas.taxiPredict.trjTools.roadNetwork.GeoPoint;
import cas.taxiPredict.trjTools.roadNetwork.Vertex;

public class Vector {
	private GeoPoint startPoint;
	private GeoPoint endPoint;
	private double x1;
	private double x2;
	
	public Vector(GeoPoint startPoint, GeoPoint endPoint){
 		this.startPoint = startPoint;
		this.endPoint = endPoint;
		x1 = endPoint.getLng() - startPoint.getLng();
		x2 = endPoint.getLat() - startPoint.getLat();
	}

	public GeoPoint getStartPoint() {
		return startPoint;
	}

	public void setStartPoint(GeoPoint startPoint) {
		this.startPoint = startPoint;
	}

	public GeoPoint getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(GeoPoint endPoint) {
		this.endPoint = endPoint;
	}
	
	public double mod(){
		return Math.sqrt(Math.pow(x1, 2) + Math.pow(x2, 2));
	}
	
	public double CosinWith(Vector v){
		double cosine = x1 * v.x1 + x2 * v.x2;
		return cosine / (mod() * v.mod());
	}

}
