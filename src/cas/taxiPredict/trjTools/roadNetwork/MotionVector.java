package cas.taxiPredict.trjTools.roadNetwork;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MotionVector {

	public final int TICKS_PER_SECOND = 1000;
    public enum MatchType
    {
        NoneMatched(0),
        SingleMatched(1),
        MultiMatched(2);
        MatchType(Integer val)
        {
        	this.value=val;
        }
        private Integer value;
		public Integer getValue() {
			return value;
		}
    }
    
  /// Time represented by seconds
    public long t;
    public GeoPoint point;
    public float v;
    //public int direction;
    public Edge e;
    public String orginalString;
    public long getEdgeId()
    {
            long edgeId = 0;
            if (e != null)
            {
                edgeId = e.ID();
            }
            return edgeId;
    }
    
    public MatchType type;
    //public HashSet<Edge> candidateEdges;
    public MotionVector(double lat, double lng, String t, double v, int direction) throws ParseException
    {
        this.t = dateFormat.parse(t).getTime() / TICKS_PER_SECOND;
        this.point = new GeoPoint(lat, lng);
        this.v = (float)v;
        this.e = null;
        //this.candidateEdges = new HashSet<Edge>();
        this.type = MatchType.NoneMatched;
        this.orginalString = "";
    }
    public MotionVector(GeoPoint p, long t)
    {
        this.point = p;
        this.t = t;
        this.v = 0;
        //this.candidateEdges = new HashSet<Edge>();
        this.type = MatchType.NoneMatched;
        this.e = null;
        this.orginalString = "";
    }
    public MotionVector()
    {
    	
    }
    public long getTimeSeconds()
    {
    	return this.t;
    }
    public String getTimeStr()
    {
    	return dateFormat.format(new Date(this.t*TICKS_PER_SECOND));
    }
    public String toString()
    {
        return (new MessageFormat("{0}:{1},{2}")).format(new Object[]{ getTimeStr(), this.point, this.e});
    }
    
   private SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMddHHmmss");
}
