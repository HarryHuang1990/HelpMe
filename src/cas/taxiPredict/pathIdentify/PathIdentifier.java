package cas.taxiPredict.pathIdentify;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;


public class PathIdentifier {

	/**
	 * @Title: displayHelpInfo 
	 * @Description: 命令行帮助说明 
	 * void
	 * @throws
	 */
	private static void displayHelpInfo()
	{
		System.out.println();
		System.out.println("*****************GPS记录路线轨迹匹配工具********************");
		System.out.println("Usage: java -jar PathIdentifier.jar [-source TrainData/] [-out outData/] [-map mapDir/]");
		System.out.println();
		System.out.println("where options include:");
		System.out.printf("%1$-30s %2$s", "-source,-sourcePath","输入数据文本文件路径或包含数据文本的目录.\n");
		System.out.printf("%1$-30s %2$s", "-out,-outPath","输出数据文本文件名或输出目录，该设置需与sourcePath同为文件或者同为目录.\n");
		System.out.printf("%1$-30s %2$s", "-map,-mapDir","地图包.\n");
		System.out.println();
		System.out.println("生成的数据示例:20110414160613,116.4078674,40.2220650,21,1,0,644078674");
		System.out.println("对应的数据项：GPS时间,GPS纬度,GPS经度,GPS速度,运营状态,GPS方向,触发事件,路段号,SegementID");
		System.out.println("属性说明：");
		System.out.println("GPS时间");
		System.out.println("GPS纬度");
		System.out.println("GPS经度");
		System.out.println("GPS速度：取值000-255内整数，以公里/小时为单位");
		System.out.println("运营状态：0=空车，1=载客，2=驻车，3=停运，4=其它");
		System.out.println("触发事件：0=变空车，1=变载客，2=设防，3=撤防，4=其它");
		System.out.println("GPS方向");
		System.out.println("路段号");
		System.out.println("SegementID");
		System.out.println("结束串：回车符+换行符");
		System.out.println();
	}
	
	/**
	 * @Title: receiveParmas 
	 * @Description: 接收命令行参数配置 
	 * @param args
	 * @param sysProp
	 * @return
	 * boolean
	 * @throws
	 */
	private static boolean receiveParmas(String[] args,Properties sysProp)
	{
		for(int i=0;i<args.length;i++)
		{
			String arg= args[i].trim();
			if(arg.equalsIgnoreCase("-help"))
			{
				displayHelpInfo();
				return false;
			}
			if(arg.startsWith("-") && args.length>i+1)
			{
				if(arg.equalsIgnoreCase("-source")||arg.equalsIgnoreCase("-sourcePath"))
				{
					sysProp.setProperty("sourcePath", args[i+1].trim());
				}
				if(arg.equalsIgnoreCase("-map")||arg.equalsIgnoreCase("-mapDir"))
				{
					sysProp.setProperty("mapDir", args[i+1].trim());
				}
				if(arg.equalsIgnoreCase("-out")||arg.equalsIgnoreCase("-outPath"))
				{
					sysProp.setProperty("outPath", args[i+1].trim());
				}
			}
		}
		return true;
	}
	
	/**
	 * @Title: checkSysProp 
	 * @Description: 配置参数格式验证 
	 * @param sysProp
	 * @return
	 * boolean
	 * @throws
	 */
	private static boolean checkSysProp(Properties sysProp)
	{
		boolean doContinue=true;
		try
		{
			File tempFile=new File(sysProp.getProperty("sourcePath"));
			if(!tempFile.exists())
				doContinue=false;
			if(tempFile.isDirectory())
			{
				File tempFile2=new File(sysProp.getProperty("outPath"));
				if(!tempFile2.exists())
					tempFile2.mkdirs();
			}
		}catch(Exception e)
		{
			doContinue=false;
			log.error("配置参数有误,异常抛出：",e);
		}
		if(doContinue==false)
		{
			log.error("配置参数有误，请检查！");
		}
		return doContinue;
	}
	
	/**
	 * @Title: 主调用Main方法
	 * @Description: 启动任务执行过程(分配工作资源，任务调度)
	 * @param args
	 * @return void 
	 * @throws
	 */
	public static void main(String[] args)
	{
		BufferedInputStream sysPropIn=null;
		Properties sysProp = new Properties();
		boolean doContinue=true;
		String basePath= PathIdentifier.class.getClass().getResource("/").getPath();
		try {
			sysPropIn = new BufferedInputStream(new FileInputStream(basePath+sysPropFilePath));
			sysProp.load(sysPropIn);
		} catch (IOException e) {
			log.error("无法正常读取资源配置源文件! @path="+basePath+sysPropFilePath,e);
			return;
		}
		if(args.length>0)
		{
			doContinue=receiveParmas(args,sysProp); //接收外部参数
		}
		if(doContinue==true)
		{
			doContinue=checkSysProp(sysProp);
		}
		if(doContinue==true)
		{
			PathAnalyzeHandler pathAnalyzeHandler;
			try {
				pathAnalyzeHandler = new PathAnalyzeHandler(sysProp.getProperty("mapDir"));
			} catch (Exception e) {
				log.error("加载地图错误，请检查地图数据及参数配置!",e);
				return;
			}
			long startTimestamp= System.currentTimeMillis();
			log.info("开始执行GPS记录路线轨迹匹配任务...");
			pathAnalyzeHandler.process(sysProp.getProperty("sourcePath"), sysProp.getProperty("outPath"));
			double processTime=(System.currentTimeMillis()-startTimestamp)/1000.0;
			log.info("GPS记录路线轨迹匹配任务执行结束，耗时 "+processTime+"s");
		}
	}
	
	/**
	 * 日志实例对象
	 */
	private static final Logger log = Logger.getLogger(PathIdentifier.class);
	/**
	 * 默认资源配置文件地址
	 */
	public static String sysPropFilePath="GPSPathIdentify.properties";
}
