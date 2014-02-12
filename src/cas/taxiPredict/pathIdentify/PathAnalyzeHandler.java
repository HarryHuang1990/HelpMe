package cas.taxiPredict.pathIdentify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cas.taxiPredict.pathIdentify.MapMatching.MM;
import cas.taxiPredict.trjTools.roadNetwork.Graph;
import cas.taxiPredict.trjTools.roadNetwork.Trajectory;


public class PathAnalyzeHandler {

	public PathAnalyzeHandler(String mapDir) throws NumberFormatException, IOException {
		this.MAP_DATASET_DIR = mapDir;
		File edgeFile = new File(MAP_DATASET_DIR, "edges.txt");
		File vertexFile = new File(MAP_DATASET_DIR, "vertices.txt");
		File geoFile =new File(MAP_DATASET_DIR, "geos.txt");
        graph = new Graph(vertexFile.getAbsolutePath(), edgeFile.getAbsolutePath(), geoFile.getAbsolutePath());
	}
	
	 /**
     * 提交路径路段拟合处理过程 
     * 该方法适用于指定文件处理或文件夹批量处理过程
     */
    public void process(String sourcePath,String outPath)
    {
    	File sourceFile=new File(sourcePath);
    	if(sourceFile.isFile())
    	{
    		File outFile=new File(outPath);
    		if(!outFile.getParentFile().exists())
    			outFile.getParentFile().mkdirs();
    		callTrjTools(sourceFile,outFile);
    	}else if(sourceFile.isDirectory())
    	{
    		for(File subFile:sourceFile.listFiles())
    		{
    			if(subFile.isFile())
    			{
    				callTrjTools(subFile,new File(outPath,subFile.getName()));
    			}
    			if(subFile.isDirectory())
    			{
    				File newOutFile=new File(outPath,subFile.getName());
    				newOutFile.mkdirs();
    				process(subFile.getAbsolutePath(),newOutFile.getAbsolutePath());
    			}
    		}
    	}
    }
    /**
     * 调用GPS路径查找方法
     * 指定源数据文件，返回动态生成数据文件
     */
    public void callTrjTools(File sourceFile,File outFile)
    {
    	if(sourceFile.exists() && sourceFile.isFile())
    	{
    		log.info("Read Records File: "+sourceFile.getName());
    		try
    		{
    			List<String> cacheLines=new ArrayList<String>();
    			FileInputStream fis=new FileInputStream(sourceFile);
    			InputStreamReader is=new InputStreamReader(fis, Charset.defaultCharset().toString());
    			BufferedReader reader = new BufferedReader(is);
    			String Line = null;
    			while ((Line = reader.readLine()) != null)
    			{
    				if(Line.startsWith("***"))
    				{
    					if(cacheLines.size()>0)
    					{
    						MM mm = new MM(graph);
    			            Trajectory trj = new Trajectory();
    			            trj.Load(cacheLines, graph);
    			            Trajectory newTrj = mm.match(trj);
    			            newTrj.SaveAndConcat(outFile, cacheLines,graph);
    			            cacheLines=new ArrayList<String>();
    					}
    				}else
    					cacheLines.add(Line);
    			}
    			if(cacheLines.size()>0)
    			{
    				MM mm = new MM(graph);
    	            Trajectory trj = new Trajectory();
    	            trj.Load(cacheLines, graph);
    	            Trajectory newTrj = mm.match(trj);
    	            newTrj.SaveAndConcat(outFile, cacheLines,graph);
    	            //sourceFile.delete();
    			}
    			reader.close();
    			is.close();
    			fis.close();
    		}catch(Exception e)
    		{
    			log.error("数据处理异常！",e);
    		}
    	}else{
    		log.error("无效的输入文件！  @sourceFile=" + sourceFile.getAbsolutePath());
    	}
    }
    /**
     * 地图包目录地址
     */
    private String MAP_DATASET_DIR=null;
    /**
     * 地图对象
     */
    private Graph graph=null;
	/**
	 * 日志实例对象
	 */
	private static final Logger log = Logger.getLogger(PathAnalyzeHandler.class);
}
