package cas.taxiPredict.pathIdentify.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cas.taxiPredict.trjTools.roadNetwork.GeoPoint;
import cas.taxiPredict.trjTools.roadNetwork.MBR;
import cas.taxiPredict.trjTools.roadNetwork.Vertex;

public class GridPoint {

	public GridPoint(Collection<Vertex> vertices, MBR mbr, double cellSize)
    {
        nCol = (int)(Math.ceil(mbr.Width() / cellSize));
        nRow = (int)(Math.ceil(mbr.Height() / cellSize));
        this.cellSize = cellSize;
        this.mbr = mbr;
        buildIndex(vertices);
    }
	private int getCell(GeoPoint p)
    {
        int row = getRow(p.getLat());
        int col = getColumn(p.getLng());
        return row * nCol + col;
    }
	
	///  Given a longitude, get its column index  
    private int getColumn(double lng)
    {
        //Debug.Assert(lng >= mbr.MinLng && lng <= mbr.MaxLng);
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
    
    private void buildIndex(Collection<Vertex> vertices)
    {
        //insert edges into the grid
        for(Vertex v : vertices)
        {
            //List<int> ids = getCells(v.ToPoint());
            int id = getCell(v.ToPoint());
            List<Vertex> list = dict.get(id);
            if (list==null)
            {
                list = new ArrayList<Vertex>();
                dict.put(id, list);
            }
            list.add(v);
        }
    }
    
    public HashSet<Vertex> RangeQuery(MBR rect)
    {
        HashSet<Vertex> result = new HashSet<Vertex>();
        List<Integer> cands = getCells(rect);
        int cands_count = cands.size();
        for (int i = 0; i < cands_count; i++)
        {
            List<Vertex> vertices = dict.get(cands.get(i));
            if (vertices!=null)
            {
                for (Vertex v : vertices)
                {
                    if (rect.Cover(v.ToPoint()))
                    {
                        result.add(v);
                    }
                }
                //result.UnionWith(vertices);
            }
        }
        return result;
    }
	private HashMap<Integer, List<Vertex>> dict = new HashMap<Integer, List<Vertex>>();

    private final int nCol;
    private final int nRow;
    private final double cellSize;
    private final MBR mbr;
}
