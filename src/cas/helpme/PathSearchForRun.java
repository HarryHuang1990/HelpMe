package cas.helpme;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import cas.helpme.PathSearching.AA;
import cas.helpme.PathSearching.RunningPath;
import cas.taxiPredict.trjTools.roadNetwork.GeoPoint;
import cas.taxiPredict.trjTools.roadNetwork.Graph;

/**
 * *****************************通往干道的路径搜索工具********************************
 * usage: java -jar PathSearchForRun [-map mapDir]
 * @author dell
 *
 */
public class PathSearchForRun {

	public static final Logger log = Logger.getLogger(PathSearchForRun.class);
	
	/**
	 * @param edgeFile
	 * @param vertexFile
	 * @throws IOException
	 */
	public static void compare(File edgeFile, File vertexFile) throws IOException{
		HashSet<String>setEdge = new HashSet<String>();
		HashSet<String>setVertex = new HashSet<String>();
		BufferedReader reader = new BufferedReader(new FileReader(edgeFile));
		String line = null;
		while((line = reader.readLine()) != null)
		{
			String[]splits = line.split(",");
			setEdge.add(splits[1]);
			setEdge.add(splits[2]);
		}
		reader.close();
		
		reader = new BufferedReader(new FileReader(vertexFile));
		while((line = reader.readLine()) != null)
		{
			String[]splits = line.split(",");
			setVertex.add(splits[0]);
		}
		
//		if(setEdge.size() > setVertex.size())
//		{
			setEdge.removeAll(setVertex);
			System.out.println(setEdge);
//		}
//		else
//		{
//			setVertex.removeAll(setEdge);
//			System.out.println("=" + setVertex);
//		}
	}
	
	public static void aa(Integer b){
		List<AA> aas = new ArrayList<AA>();
		AA a = new AA(1, 3, "a", true);
		aas.add(a);
		aas.add(new AA(1, 33.2, "ndf", false));
		b=9;
		System.out.println(aas.contains(a));
	}
	
	public static void main(String args[]) throws NumberFormatException, IOException{
		
		String mapDir = "F:/beijing gps/map/Beijing_2011_jiahai/";
		if(args.length > 0)
		{
			//接收外部参数
			for(int i=0; i<args.length; i++){
				if(args[i].startsWith("-") && args.length > i+1)
				{
					if("-map".equals(args[i]))
						mapDir = args[i + 1];
				}
			}
		}
		
		log.info("正在加载地图...");
		File edgeFile = new File(mapDir, "edges.csv");
		File vertexFile = new File(mapDir, "vertices.csv");
		File geoFile =new File(mapDir, "geos.csv");
		
//		compare(edgeFile, vertexFile);
		
        Graph graph = new Graph(vertexFile.getAbsolutePath(), edgeFile.getAbsolutePath(), geoFile.getAbsolutePath());
        RunningPath runningPath = new RunningPath(graph);
        
//        while(true)
//        {
//        	Scanner read = new Scanner(System.in);
//        	System.out.print("请输入事发点的GPS坐标, 格式:\"维度,经度\"");
        	String gpsLine = "39.979270, 116.337924";//"39.978616,116.335224";//"39.980629,116.335046";//read.next();
        	String splits[] = gpsLine.split(",");
        	
        	if(splits.length == 2)
        	{
        		double lng = 0, lat = 0;
        		try
        		{
        			lng = Double.parseDouble(splits[1]);
            		lat = Double.parseDouble(splits[0]);
        		}catch (Exception e)
        		{
        			log.error("请输入正确格式的坐标！");
        		}
        		GeoPoint gp = new GeoPoint(lat, lng);
        		runningPath.search(gp);
//        		runningPath.printSerchingResults();
        	}
//        }
		
	}
}
