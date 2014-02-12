package cas.taxiPredict.trjTools.roadNetwork;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cas.taxiPredict.pathIdentify.base.FileUtil;
import cas.taxiPredict.pathIdentify.base.StringUtils;
import cas.taxiPredict.pathIdentify.outWrapper.SegIdxObject;

public class Trajectory extends ArrayList<MotionVector>{

	/**
	 * @Fields serialVersionUID : TODO
	 */ 
	private static final long serialVersionUID = 2504341203006774001L;

	public double getLength()
    {
            double length = 0;
            for (int i = 0; i < this.size() - 1; i++)
            {
                double tmpLength = GeoPoint.GetDistance(this.get(i).point, this.get(i + 1).point);
                double speed = tmpLength / (this.get(i + 1).getTimeSeconds() - this.get(i).getTimeSeconds());
                if (speed < 60)
                {
                    length += tmpLength;
                }
            }
            return length;
    }
	public Trajectory()
	{
		super();
	}
	public Trajectory(List<MotionVector> mvs)
	{
		super(mvs);
	}
	public Trajectory(MotionVector[] mvs)
	{
		super(Arrays.asList(mvs));
	}
	public Trajectory(String fileName, Graph g) throws IOException, ParseException
	{
		super();
	    this.Load(new File(fileName), g);
	}
	 public String toString()
     {
         return ("Count="+ this.size());
     }
	 public void Load(File sourceFile, Graph graph) throws IOException, ParseException
     {
         List<String> lines = FileUtil.readAllLines(sourceFile,Charset.defaultCharset().displayName());
         Load(lines, graph);
     }
	 public void Load(List<String> lines, Graph graph) throws IOException, ParseException
     {
         SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMddHHmmss");
         for (String line:lines)
         {
        	 List<String> fields=StringUtils.SimpleSplit(line, ',');
             if (fields.size() >= 3)
             {
                 long time = dateFormat.parse(fields.get(0)).getTime();
                 double lat = Double.valueOf(fields.get(1));
                 double lng = Double.valueOf(fields.get(2));
                 this.add(new MotionVector(new GeoPoint(lat, lng), time));
             }
         }
     }
	 
	 /**
      *得到每个GPS点对应segmentID
      * 
      */
     public int getSegmentID(GeoPoint point, long edgeID, Graph graph)
     {
    	 SegIdxObject segIdx=new SegIdxObject(-1);
         Edge edge = graph.getEdges().get(edgeID);
         if(edge!=null)
         {
        	 GeoPoint result=GeoPoint.INVALID();
        	 edge.getGeo().ProjectFrom(point,result,segIdx);
         }
         return segIdx.segIdx;
     }
	 
	 /**
	  * 
	  * @Title: SaveAndConcat 
	  * @Description: 
	  * 保存路段号并和原始文本串联
      * 此外还保存segmentID和补全的edge序列
      * 格式 time,lat,lng,...., edgeID, segmentID#edgeID1,edgeID2,edgeID3,..
	  * @param outFile
	  * @param sourceFile
	  * @param graph
	  * @throws IOException 
	  * @author JiahaiWu
	  * @return void 
	  * @throws
	  */
     public void SaveAndConcat(File outFile,List<String> srcLines, Graph graph) throws IOException
     {
    	 String segStr=Concat(srcLines, graph);
    	 if(segStr.length()>0)
    	 {
    		 FileOutputStream fos=new FileOutputStream(outFile,outFile.exists());
        	 OutputStreamWriter os=new OutputStreamWriter(fos);
    		 if(outFile.exists() && outFile.length()>0)
    		 {
    			 os.write("***\r\n");
    		 }
    		 os.write(segStr);
    		 os.flush();
    		 os.close();
    		 fos.close();
    	 } 
     }
     /**
	  * 
	  * @Title: Concat 
	  * @Description: 
	  * 保存路段号并和原始文本串联
      * 此外还保存segmentID和补全的edge序列
      * 格式 time,lat,lng,...., edgeID, segmentID#edgeID1,edgeID2,edgeID3,..
	  * @param outFile
	  * @param graph
	  * @throws IOException 
	  * @author JiahaiWu
	  * @return String 
	  * @throws
	  */
     public String Concat(List<String> srcLines, Graph graph) throws IOException
     {
		 StringBuilder sb=new StringBuilder();
		 long nextEdgeID = -1;
         if(this.size() > 1)
             nextEdgeID = this.get(1).getEdgeId();
         for (int i = 0; i < this.size(); i++)
         {
             String bline = srcLines.get(i);
             List<String> fields= StringUtils.SimpleSplit(bline, ',');
             if (fields.size() >= 3)
             {
            	 Long edgeId=this.get(i).getEdgeId();
            	 Double lat = Double.valueOf(fields.get(1));
            	 Double lng = Double.valueOf(fields.get(2));
                 //获得SegmentID
                 Integer segmentID = this.getSegmentID(new GeoPoint(lat, lng),this.get(i).getEdgeId(), graph);
                 if(edgeId.equals(0L) && segmentID.equals(-1)) 
                 {   //失效匹配点
                	 if (i < (this.size() - 2))
                         nextEdgeID = this.get(i+2).getEdgeId();
                     else
                         nextEdgeID = -1;
                	 continue;  
                 }
                 Edge from = null;
                 Edge to = null;
                 List<Edge> edges = null;
                 
                 if (nextEdgeID != -1) 
                 {
                     //获得补全的路径
                     if (edgeId != nextEdgeID && graph.getEdges().containsKey(edgeId) && graph.getEdges().containsKey(nextEdgeID))
                     {
                         from = graph.getEdges().get(edgeId);
                         to = graph.getEdges().get(nextEdgeID);
//                         if (edgeId-60564L==0L)
//                         {
//                             System.out.println(from.End().getOutEdges().size());
//                             System.out.println(from.End().getOutEdges().toString());
//                         }
                         //1.get difference
                         double dist = GeoPoint.GetDistance(from.End().getLat(), from.End().getLng(),to.Start().getLat(), to.Start().getLng());
                         double maxDist = Math.max(dist + 300, dist * 1.5);
                         edges = graph.FindPath(from.End(), to.Start(), maxDist);
                     }

                     if (i < (this.size() - 2))
                         nextEdgeID = this.get(i+2).getEdgeId();
                     else
                         nextEdgeID = -1;
                 }
                 sb.append(bline).append(',').append(edgeId).append(',').append(segmentID);
                 if (edges!=null && edges.size()>0)
                 {
                	 String edgeList = "";
                     for(int j = 0; j < edges.size(); j++)
                     {
                         if (j == 0)
                             edgeList = "" + edges.get(j).ID();
                         else
                             edgeList += "," + edges.get(j).ID();
                     }
                	 sb.append('#').append(edgeList);
                 }
                 sb.append("\r\n");
             }
         }
		 return sb.toString();
     }
     
   /// Remove outliers. <br/>
     /// We assume that the first mv is not outlier
     /// </summary>
     /// <returns></returns>
     public Trajectory RemoveOutlier()
     {
         int trjSize = this.size();
         if (trjSize <= 1)
         {
             return this;
         }
         MotionVector[] mvs = (MotionVector[]) this.toArray();
         double maxSpeed = 60; //60m/s
         this.clear();
         this.add(mvs[0]);
         for (int i = 1; i < trjSize; i++)
         {
             double distance = GeoPoint.GetDistance(mvs[i - 1].point, mvs[i].point);
             double inteval = (mvs[i].getTimeSeconds() - mvs[i - 1].getTimeSeconds());
             double speed = distance / inteval;
             if (speed <= maxSpeed)
             {
                 this.add(mvs[i]);
             }
             else
             {
                 //Debug.Assert(false);
             }
         }
         return this;
     }

     /// <summary>
     /// Separate the trajectory into serveral smaller ones
     /// </summary>
     /// <param name="maxInteval"></param>
     /// <returns></returns>
     public List<Trajectory> Separate(int maxInteval)
     {
         List<Trajectory> list = new ArrayList<Trajectory>();
         int trjSize = this.size();
         if (trjSize <= 1)
         {
             list.add(this);
             return list;
         }
         MotionVector[] mvs = (MotionVector[]) this.toArray();
         Trajectory trj = new Trajectory();
         trj.add(mvs[0]);
         for (int i = 1; i < trjSize; i++)
         {
             double inteval = (mvs[i].getTimeSeconds() - mvs[i - 1].getTimeSeconds());
             if (inteval > maxInteval)
             {
                 list.add(trj);
                 trj = new Trajectory();
             }
             trj.add(mvs[i]);
         }
         return list;
     }
     public EdgePath getPath()
     {
             EdgePath path = new EdgePath();
             Edge lastEdge = null;
             for (int i = 0; i < this.size(); i++)
             {
                 if (this.get(i).e != lastEdge)
                 {
                     path.add(this.get(i).e);
                     lastEdge = this.get(i).e;
                 }
             }
             return path;
     }

     public Trajectory Slice(int from, int length)
     {
         if (from < 0 || length <= 0 || from + length >= this.size())
         {
             return null;
         }
         MotionVector[] mvs = new MotionVector[length];
         System.arraycopy(this, from, mvs, 0, length);
         return new Trajectory(mvs);
     }
     public double SpeedAt(long timeSeconds)
     {
         double speed = -1;
         MotionVector result = new MotionVector();
         result.point.setINVALID();
         if (this.size() == 0
             || timeSeconds < this.get(0).getTimeSeconds() || timeSeconds > this.get(this.size() - 1).getTimeSeconds())
         {
             return speed;
         }
         for (int i = 1; i < this.size(); i++)
         {
             if (timeSeconds >= this.get(i-1).getTimeSeconds() && timeSeconds <= this.get(i).getTimeSeconds())
             {
             }
         }
         return speed;
     }
     
     public List<MotionVector>  getMotionVectors()
     {
    	 return this;
     }
}
