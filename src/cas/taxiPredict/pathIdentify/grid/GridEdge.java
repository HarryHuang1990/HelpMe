package cas.taxiPredict.pathIdentify.grid;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


import java.util.Set;

import cas.taxiPredict.pathIdentify.base.Constants;
import cas.taxiPredict.trjTools.roadNetwork.Edge;
import cas.taxiPredict.trjTools.roadNetwork.GeoPoint;
import cas.taxiPredict.trjTools.roadNetwork.MBR;

public class GridEdge {

	 public GridEdge(Collection<Edge> edges, MBR mbr, double cellSize)
     {
         nCol = (int)(Math.ceil(mbr.Width() / cellSize));
         nRow = (int)(Math.ceil(mbr.Height() / cellSize));
         this.cellSize = cellSize;
         this.mbr = mbr;
         buildIndex(edges);
     }
	 
	 private void buildIndex(Collection<Edge> edges)
     {
         //insert edges into the grid
         for(Edge e : edges)
         {
             List<Integer> ids = getCells(e);
             for (int j = 0; j < ids.size(); j++)
             {
                 List<Edge> list = dict.get(ids.get(j));
                 if (list==null)
                 {
                     list = new ArrayList<Edge>();
                     dict.put(ids.get(j), list);
                 }
                 list.add(e);
             }
         }
     }
	 private int getCell(GeoPoint p)
     {
         int row = getRow(p.getLat());
         int col = getColumn(p.getLng());
         return row * nCol + col;
     }

	/// Get the cells that might contain Edge e
     private List<Integer> getCells(Edge e)
     {
         return getCells(e.getMBR());
     }
     private List<Integer> getCells(MBR mbr)
     {
         List<Integer> rst = new ArrayList<Integer>();
         int c1 = getCell(mbr.TopLeft());
         int c2 = getCell(mbr.BottomRight());
         int c1col = c1 % nCol;
         int c2col = c2 % nCol;

         int c1row = (c1 - c1col) / nCol;
         int c2row = (c2 - c2col) / nCol;

         int ncol = c2col - c1col + 1;
         int nrow = c2row - c1row + 1;
         for (int i = 0; i < nrow; i++)
         {
             for (int j = 0; j < ncol; j++)
             {
                 rst.add(c1col + j + (c1row + i) * nCol);
             }
         }
         return rst;
     }
     
   ///  Given a longitude, get its column index  
     private int getColumn(double lng)
     {
         if (lng <= mbr.MinLng())
         {
             return 0;
         }
         if (lng >= mbr.MaxLng())
         {
             return nCol - 1;
         }
         return (int)((lng - mbr.MinLng()) / cellSize);
     }
   /// Given a latitude, get its row index  
     private int getRow(double lat)
     {
         //Debug.Assert(lat >= mbr.MinLat && lat <= mbr.MaxLat);
         if (lat <= mbr.MinLat())
         {
             return 0;
         }
         if (lat >= mbr.MaxLat())
         {
             return nRow - 1;
         }
         return (int)((lat - mbr.MinLat()) / cellSize);
     }
     
     /// Get the edge with a distance roughly lower than radius from point p 
     public HashSet<Edge> RangeQuery(GeoPoint p, double radius)
     {
         HashSet<Edge> result = new HashSet<Edge>();
         List<Integer> cands = null;
         //get mbr
         double d_radius = radius * Constants.D_PER_M;	//radius in degree
         double minLat, minLng, maxLat, maxLng;
         double radius2 = radius * radius;
         minLng = p.getLng() - d_radius;
         maxLng = p.getLng() + d_radius;
         minLat = p.getLat() - d_radius;
         maxLat = p.getLat() + d_radius;
         MBR rect = new MBR(minLng, minLat, maxLng, maxLat);
         cands = getCells(rect);
         int cands_count = cands.size();
         for (int i = 0; i < cands_count; i++)
         {
             List<Edge> edges = dict.get(cands.get(i));
             if (edges!=null)
             {
                 int count = edges.size();
                 for (int j = 0; j < count; j++)
                 {
                     if (edges.get(j).Dist2From(p) <= radius2)
                     {
                         result.add(edges.get(j));
                     }
                     
                 }
             }
         }
         return result;
     }
     
     /// Get the edge with a distance lower than radius from point p 
     public HashSet<Edge> rangeQuery(MBR mbr)
     {
         return null;
     }
     
    private HashMap<Integer, List<Edge>> dict = new HashMap<Integer,List<Edge>>();

    private final int nCol;
    private final int nRow;
    private final double cellSize;
    private final MBR mbr;
}
