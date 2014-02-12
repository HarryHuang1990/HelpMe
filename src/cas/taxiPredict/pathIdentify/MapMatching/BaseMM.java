package cas.taxiPredict.pathIdentify.MapMatching;

import cas.taxiPredict.trjTools.roadNetwork.Graph;
import cas.taxiPredict.trjTools.roadNetwork.Trajectory;

public abstract class BaseMM {
     protected Graph graph = null;
     protected Trajectory trj = null;
     public BaseMM(Graph g)
     {
         this.graph = g;
     }

     public void setTrj(Trajectory trj)
     {
         this.trj = trj;
     }

     /// <summary>
     /// Peform mapmatching on a trajectory
     /// </summary>
     /// <param name="trj"></param>
     public abstract Trajectory match(Trajectory trj);
}
