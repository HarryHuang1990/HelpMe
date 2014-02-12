package cas.taxiPredict.trjTools.roadNetwork;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import cas.taxiPredict.pathIdentify.base.StringUtils;
import cas.taxiPredict.pathIdentify.outWrapper.SegIdxObject;
import cas.taxiPredict.pathIdentify.outWrapper.TypeObject;

public class Edge {

	long id;

    public long ID()
    {
    	return id; 
    }
    Vertex start;

    public Vertex Start()
    {
        return start; 
    }
    Vertex end;

    public Vertex End()
    {
        return end; 
    }
    
    String kind;
    
    public String Kind()
    {
    	return kind;
    }
    
    int width;
    
    public int Width()
    {
    	return width;
    }
    
    String name;
    
    public String Name()
    {
    	return name;
    }
    
    int pathClass;
    
    public int PathClass()
    {
    	return pathClass;
    }
    
    double length = -1;
    
    public double Length(){
    	return length;
    }
    
    boolean carryAble;
    
    public boolean CarrayAble(){
    	return carryAble;
    }

    /// Polyline that represents the geometry of the edge
    private Polyline geo = null;

    private String geoString = null;
    
    public MBR getMBR()
    {
        return getGeo().MBR();
    }
	public Polyline getGeo() {
		if (geo == null)
        {
            //for multiple 
            synchronized (syncRoot)
            {
                if (geo == null)
                {
                    List<GeoPoint> points = new ArrayList<GeoPoint>();
                    if (StringUtils.IsNullOrEmpty(geoString))
                    {
                        points.add(this.start.ToPoint());
                        points.add(this.end.ToPoint());
                    }
                    else
                    {
                    	List<String> fields=StringUtils.SimpleSplit(geoString, ',');
                        for (int i = 0; i < fields.size(); i += 2)
                        {
                            double lng = Double.valueOf(fields.get(i));
                            double lat = Double.valueOf(fields.get(i + 1));
                            points.add(new GeoPoint(lat, lng));
                        }
                        this.geoString = null;
                    }
                    geo = new Polyline(points);
                }
            }
        }
		return geo;
	}
	public void setGeo(Polyline geo) {
		this.geo = geo;
	}
	public String getGeoString() {
		return geoString;
	}
	public void setGeoString(String geoString) {
		this.geoString = geoString;
	}
	private double getLength()
    {
        //double len = GeoPoint.GetDistance(start.ToPoint(), end.ToPoint());
        double len = getGeo().getLength();
        return len;
    }
   
//    public double Length()
//    {
//      
//            if (length < 0)
//            {
//                synchronized (syncRoot) {
//                    if (length < 0)
//                    {
//                        length = getLength();
//                    }
//                }
//            }
//            return length;
//    }
    
    
    private final Object syncRoot = new Object();
    private List<Edge> outEdges = null;
    public List<Edge> getOutEdges()
    {
            if (outEdges == null)
            {
                //thread safe
                outEdges = this.End().getOutEdges();
            }
            return outEdges;

    }
    
    private List<Edge> inEdges = null;

    public List<Edge> getInEdges()
    {
            if (inEdges == null)
            {
                //thread safe
                inEdges = this.Start().getInEdges();
            }
            return inEdges;
    }
    
    public Edge(long id, Vertex start, Vertex end)
    {
        this.id = id;
        this.start = start;
        this.end = end;
    }
    public Edge(long id, Vertex start, Vertex end, double length)
    {
        this.id = id;
        this.start = start;
        this.end = end;
        this.length = length;
    }
    public Edge(long id, Vertex start, Vertex end, double length, double speedLimit, int type)
    {
        this.id = id;
        this.start = start;
        this.end = end;
        this.length = length;
        //this.speedLimit = speedLimit;
        //this.type = type;
    }
    public Edge(long id, Vertex start, Vertex end, 
    		String kind, int width, String name, int pathClass, double length, boolean carryAble)
    {
        this.id = id;
        this.start = start;
        this.end = end;
        this.kind = kind;
        this.width = width;
        this.name = name;
        this.pathClass = pathClass;
        this.length = length;
        this.carryAble = carryAble;
    }
    
    /// Get the projection from a certain point
    public GeoPoint projectFrom(GeoPoint p)
    {
        GeoPoint result = getGeo().ProjectFrom(p);
        return result;
    }

    public int projectFrom(GeoPoint p, GeoPoint result)
    {
    	SegIdxObject segIdxObj=new SegIdxObject(0);
        int type = getGeo().ProjectFrom(p, result, segIdxObj);
        return type;
    }
    
    /// Get the distance from a point to the edge
    public double Dist2From(GeoPoint p)
    {
        TypeObject typeObj=new TypeObject(0);
        return getGeo().Dist2From(p, typeObj);
    }

    public double Dist2From(GeoPoint p, TypeObject typeObj)
    {
        return getGeo().Dist2From(p, typeObj);
    }
    
    /// Get the distance from a point to the edge
    public double DistFrom(GeoPoint p)
    {
        return Math.sqrt(Dist2From(p));
    }
    public double DistFrom(GeoPoint p, TypeObject typeObj)
    {
        return Math.sqrt(Dist2From(p, typeObj));
    }

    public GeoPoint Predict(GeoPoint start, double distance)
    {
        return this.getGeo().Predict(start, distance);
    }
    public double EndDistFrom(GeoPoint p,TypeObject typeObj)
    {
        return this.getGeo().EndDistFrom(p, typeObj);
    }
    
  /// Predict the position from start after distance on this route
    public double DistOnLine(GeoPoint from, GeoPoint to)
    {
        return this.getGeo().DistOnLine(from, to);
    }
    /// Calculate the cosine value with line p1,p2
    public double CosWith(GeoPoint p1, GeoPoint p2)
    {
        return getGeo().CosWith(p1, p2);
    }
    
    public boolean equals(Object obj)
    {
        boolean result = false;
        if (obj != null && obj instanceof Edge)
        {
            result = ((Edge)obj).ID() == this.ID();
        }
        return result;
    }
    public int hashCode()
    {
        return (int)this.ID() ^ (int)(this.ID() >> 32);
    }
    public String toString()
    {
    	return (new MessageFormat("ID:{0},{1}->{2}")).format(new Object[]{ this.ID(), this.Start().getId(), this.end.getId()});
    }
}
