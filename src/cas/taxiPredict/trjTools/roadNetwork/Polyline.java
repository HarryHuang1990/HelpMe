package cas.taxiPredict.trjTools.roadNetwork;

import java.util.List;

import cas.taxiPredict.pathIdentify.base.Constants;
import cas.taxiPredict.pathIdentify.outWrapper.SegIdxObject;
import cas.taxiPredict.pathIdentify.outWrapper.TypeObject;
import cas.taxiPredict.pathIdentify.util.Utility;

/// Driving Route
public class Polyline {
	 private MBR getMBR()
     {
         double minLat = Double.POSITIVE_INFINITY, minLng = Double.POSITIVE_INFINITY;
         double maxLat = Double.NEGATIVE_INFINITY, maxLng = Double.NEGATIVE_INFINITY;
         int pointCount = points.size();
         for (int i = 0; i < pointCount; i++)
         {
             minLat = Math.min(minLat, points.get(i).getLat());
             minLng = Math.min(minLng, points.get(i).getLng());
             maxLat = Math.max(maxLat, points.get(i).getLat());
             maxLng = Math.max(maxLng, points.get(i).getLng());
         }
         MBR mbr = new MBR(minLng, minLat, maxLng, maxLat);
         return mbr;
     }
	 
	 public double getLength()
	 {
		 return getLength(false);
	 }
	 private double getLength(boolean isPrecise)
     {
         double tmpLen = 0;
         for (int i = 0; i < this.points.size() - 1; i++)
         {
             if (isPrecise)
             {
                 tmpLen += GeoPoint.GetPreciseDistance(points.get(i), points.get(i+1));
             }
             else
             {
                 tmpLen += GeoPoint.GetDistance(points.get(i), points.get(i+1));
             }
         }
         return tmpLen;
     }
	 
	 public Polyline(List<GeoPoint> points)
     {
         this.points = points;
     }
	 
	 public static int ProjectFrom(GeoPoint start, GeoPoint end, GeoPoint p, GeoPoint result)
     {
         int type = 0;
         double vY = end.getLat() - start.getLat();
         double vX = end.getLng() - start.getLng();
         double wY = p.getLat() - start.getLat();
         double wX = p.getLng() - start.getLng();

         //扭转LAT、LNG比例误差
         double vY_m = vY * Constants.M_PER_LAT;	//
         double vX_m = vX * Constants.M_PER_LNG;	//
         double wY_m = wY * Constants.M_PER_LAT;
         double wX_m = wX * Constants.M_PER_LNG;

         double bY, bX;

         double c1 = wY_m * vY_m + wX_m * vX_m;
         double c2 = vY_m * vY_m + vX_m * vX_m;

         result.INVALID();

         if (c1 <= 0)
         {
             //when the given point is left of the source point
             //result = start;
        	 result.setLat(start.getLat());
        	 result.setLng(start.getLng());
         }
         else if (c2 <= c1)
         {
             // when the given point is right of the target point
             //result = end;
             result.setLat(end.getLat());
        	 result.setLng(end.getLng());
         }
         else //between the source point and target point
         {
             double b = c1 / c2;
             bY = start.getLat() + b * vY;
             bX = start.getLng() + b * vX;
             //result = new GeoPoint(bY, bX);
             result.setLat(bY); 
             result.setLng(bX);
         }
         type = (short)(c1 / c2);
         return type;
     }
	 /// Get projection from certain point
     public int ProjectFrom(GeoPoint p, GeoPoint result, SegIdxObject segIdxObj)
     {
         int type = -1;
         double minDist = Double.POSITIVE_INFINITY;
         GeoPoint tmpResult = GeoPoint.INVALID();
         result.setINVALID();
         segIdxObj.segIdx = 0;
         for (int i = 0; i < this.points.size() - 1; i++)
         {
             int tmpType = Polyline.ProjectFrom(points.get(i), points.get(i+1), p,tmpResult);
             double tmpDist = GeoPoint.GetDistance2(tmpResult, p);

             if (tmpDist <= minDist)
             {
                 if (tmpType == 0 || type != 0)
                 {
                     //good projection is true or tmpType==0
                     type = tmpType;
                     minDist = tmpDist;
                     //result = tmpResult;
                     result.setLat(tmpResult.getLat());
                     result.setLng(tmpResult.getLng());
                     segIdxObj.segIdx = i;
                 }
             }
             //break;
         }
         return type;
     }
     
     public GeoPoint ProjectFrom(GeoPoint p)
     {
    	 SegIdxObject segIdxObj=new SegIdxObject(0);
         GeoPoint result= GeoPoint.INVALID();
         ProjectFrom(p,  result,  segIdxObj);
         return result;
     }
     public double DistFrom(GeoPoint p, TypeObject typeObj)
     {
         return Math.sqrt(Dist2From(p, typeObj));
     }
     public double Dist2From(GeoPoint p, TypeObject typeObj)
     {
         GeoPoint projection= GeoPoint.INVALID();
         SegIdxObject segIdxObj=new SegIdxObject(0);
         typeObj.type = ProjectFrom(p,  projection,  segIdxObj);
         return GeoPoint.GetDistance2(projection, p);
     }
     
     /// Get the distance from a point to this polyline
     public double DistFrom(GeoPoint p)
     {
    	 TypeObject typeObj=new TypeObject(0);
         return DistFrom(p, typeObj);
     }
     /// Distance from p to the end of the polyline(by this route)
     public double EndDistFrom(GeoPoint p, TypeObject typeObj)
     {
    	 GeoPoint result=GeoPoint.INVALID();
         SegIdxObject segIdxObj=new SegIdxObject(null);
         typeObj.type = ProjectFrom(p,  result,  segIdxObj);
         double distance = GeoPoint.GetDistance(p, points.get(segIdxObj.segIdx + 1));
         for (int i = segIdxObj.segIdx + 1; i < points.size() - 1; i++)
         {
             distance += GeoPoint.GetDistance(points.get(i), points.get(i + 1));
         }
         return distance;
     }
   /// Get the distance between the projections on the polyline
     public double DistOnLine(GeoPoint from, GeoPoint to)
     {
         GeoPoint fromProject=GeoPoint.INVALID(), toProject=GeoPoint.INVALID();
         SegIdxObject fromSegIdxObj=new SegIdxObject(null);
         SegIdxObject toSegIdxObj=new SegIdxObject(null);
         int fromType = ProjectFrom(from,  fromProject,  fromSegIdxObj);
         int toType = ProjectFrom(to, toProject,  toSegIdxObj);
         double distance = 0;
         //Debug.Assert(fromType == 0 && toType == 0);
         if (fromSegIdxObj.segIdx == toSegIdxObj.segIdx)
         {
             distance = GeoPoint.GetDistance(fromProject, toProject);
         }
         else
         {
             distance = GeoPoint.GetDistance(fromProject, points.get(fromSegIdxObj.segIdx+1));
             for (int i = fromSegIdxObj.segIdx + 1; i < toSegIdxObj.segIdx; i++)
             {
                 distance += GeoPoint.GetDistance(points.get(i), points.get(i + 1));
             }
             distance += GeoPoint.GetDistance(points.get(toSegIdxObj.segIdx), toProject);
         }
         //distance+=GeoPoint.GetDistance(fromProject,)
         return distance;
     }
     
     public static double DistFrom(GeoPoint start, GeoPoint end, GeoPoint p)
     {
         GeoPoint result=GeoPoint.INVALID();
         ProjectFrom(start, end, p,  result);
         double distance = 0;
         distance = GeoPoint.GetDistance(p, result);
         return distance;
     }
     
   /// Calculate the cosine value with line p1,p2
     public double CosWith(GeoPoint p1, GeoPoint p2)
     {
         double wY, wX;
         double vY, vX;
         GeoPoint start=GeoPoint.INVALID(), end=GeoPoint.INVALID();
         SegIdxObject startIdx=new SegIdxObject(0);
         ProjectFrom(p1, start, startIdx);
         start = this.points.get(startIdx.segIdx);
         end = this.points.get(startIdx.segIdx + 1);
         vY = Utility.refineDoubleZero(end.getLat() - start.getLat());
         vX = Utility.refineDoubleZero(end.getLng() - start.getLng());
         wY = Utility.refineDoubleZero(p2.getLat() - p1.getLat());
         wX = Utility.refineDoubleZero(p2.getLng() - p1.getLng());
         double sum = vY * wY + vX * wX;
         double result = sum / Math.sqrt(1.0 * (vY * vY + vX * vX) * (wY * wY + wX * wX));
         return result;
     }
     
     public GeoPoint Predict(GeoPoint start, double distance)
     {
         GeoPoint projStart=GeoPoint.INVALID();
         GeoPoint target = this.points.get(this.points.size()-1);
         SegIdxObject segIdxObj=new SegIdxObject(0);
         int startType = ProjectFrom(start, projStart, segIdxObj);
         double currentDistance = 0;
         while (segIdxObj.segIdx < this.points.size() - 1)
         {
             double length = GeoPoint.GetDistance(start, points.get(segIdxObj.segIdx + 1));
             if (currentDistance + length >= distance)
             {
                 double leftLength = distance - currentDistance;
                 double ratio = leftLength / length;
                 double lat = start.getLat() + ratio * (points.get(segIdxObj.segIdx + 1).getLat() - start.getLat());
                 double lng = start.getLng() + ratio * (points.get(segIdxObj.segIdx + 1).getLng() - start.getLng());
                 target = new GeoPoint(lat, lng);
                 break;
             }
             else
             {
                 currentDistance += length;
                 start = points.get(segIdxObj.segIdx + 1);
                 segIdxObj.segIdx++;
             }
         }
         return target;
     }
     
	 //private double length = -1;
    private List<GeoPoint> points;

    public List<GeoPoint> Points()
    {
         return points;
    }
    
    public double PreciseLength()
    {
        return getLength(true);

    }
    public int Count()
    {
        return this.points.size();
    }
    
    private MBR mbr = null;

    public MBR MBR()
    {
        if (mbr == null)
        {
            mbr = getMBR();
        }
        return mbr;
    }
}
