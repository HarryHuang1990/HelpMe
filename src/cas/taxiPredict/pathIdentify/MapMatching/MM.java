package cas.taxiPredict.pathIdentify.MapMatching;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sun.org.apache.xpath.internal.operations.Gte;

import cas.taxiPredict.pathIdentify.outWrapper.TypeObject;
import cas.taxiPredict.trjTools.roadNetwork.Edge;
import cas.taxiPredict.trjTools.roadNetwork.GeoPoint;
import cas.taxiPredict.trjTools.roadNetwork.Graph;
import cas.taxiPredict.trjTools.roadNetwork.MotionVector;
import cas.taxiPredict.trjTools.roadNetwork.Polyline;
import cas.taxiPredict.trjTools.roadNetwork.Trajectory;

public class MM extends BaseMM {

	public MM(Graph g) {
		super(g);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Trajectory match(Trajectory trj) {
	
		mvs= trj.getMotionVectors();
        HashMap<Long, Node> T = new HashMap<Long, Node>();
        int trjSize = trj.size();
        GeoPoint startPoint = trj.get(0).point;
        double radius = RADIUS;
        double maxRadius = MAX_RADIUS;
        //1. initialize T
        //1.1. get nearest edges
        HashSet<Edge> currentStates = null;
        //currentStates = graph.rangeQuery(startPoint, radius, maxRadius, 4);
        currentStates = getCandidateEdges(startPoint, radius);
        for (Edge e : currentStates)
        {
            double prob = getEmissionProbility(e, startPoint);
            T.put(e.ID(), new Node(prob, e.ID(), 0, null));
 //         System.out.println("CandidateEdges:"+e.ID()+" @prob="+prob);
        }

        //2. Forward viterbri
        for (int output = 0; output < trjSize - 1; output++)
        {
        	HashMap<Long, Node> U = new HashMap<Long, Node>();// new dictionary of state prob
            double highest = Double.NEGATIVE_INFINITY;
            HashSet<Edge> nextStates = null;
            double currentRadius = radius;
            while (currentRadius <= maxRadius && Double.isInfinite(highest))
            {
                nextStates = getCandidateEdges(trj.get(output + 1).point, currentRadius);
                for(Edge nextState : nextStates)
                {
                    //long argMax = 0;
                    double valMax = Double.NEGATIVE_INFINITY;
                    Node argMax = null;
                    for (Edge state : currentStates)
                    {
                        Node n = T.get(state.ID());
                        double vProb = n.prob;
                        if (Double.isInfinite(vProb))
                        {
                            continue;
                        }
                        double ep = getEmissionProbility(state, trj.get(output).point);
                        if (!Double.isInfinite(ep))
                        {
                            double tp = getTransitionProbility(state, trj.get(output).point, nextState, trj.get(output+1).point);
                            vProb += ep + tp;
                            if (vProb > valMax)
                            {
                                valMax = vProb;
                                argMax = n;
                            }
                            if (vProb > highest)
                            {
                                highest = vProb;
                            }
                        }

                    }
                    U.put(nextState.ID(), new Node(valMax, nextState.ID(), output + 1, argMax));
//                    System.out.println(U.get(nextState.ID()).toString());
                }
                currentRadius *= 2;
            }
            if (Double.isInfinite(highest))
            {
                startPoint = trj.get(output + 1).point;

                //Console.WriteLine("Makov is interrupted at idx:{0},({1:.######},{2:.######})", output + 1, startPoint.Lng, startPoint.Lat);
//                System.out.println("Makov is interrupted at idx:"+(output + 1)+",("+startPoint.getLng()+","+startPoint.getLat()+")");
                //1. set match result
                setMatchResult(T);

                //2. Set probability
                for(Edge e : nextStates)
                {
                    double prob = getEmissionProbility(e, startPoint);
                    U.put(e.ID(), new Node(prob, e.ID(), output + 1, null));
 //                   System.out.println("UCandidateEdges:"+e.ID()+" @prob="+prob);
                }
            }
            T = U;
            currentStates = nextStates;
//            for(Edge e : currentStates)
//            {
//                System.out.println("T2CandidateEdges:"+e.ID()+" @prob="+T.get(e.ID()).prob);
//            }
        }
//      for(Map.Entry<Long,Node> entry : T.entrySet())
//      {
//          System.out.println("T3CandidateEdges:"+entry.getKey()+" @prob="+entry.getValue().prob);
//      }
        setMatchResult(T);
        return new Trajectory(mvs);
	}
	
	/// Get the simplified ln version of transportation prob
    /// </summary>
    /// <param name="e1"></param>
    /// <param name="p1"></param>
    /// <param name="e2"></param>
    /// <param name="p2"></param>
    /// <returns></returns>
    private double getTransitionProbility(Edge e1, GeoPoint p1, Edge e2, GeoPoint p2)
    {
        double prob = Double.NEGATIVE_INFINITY;
        double diff = 0;

        //1.get difference
        double dist = GeoPoint.GetDistance(p1, p2);
        //double maxDist = Math.Min(dist + 200, dist * 2 + 25);
        double maxDist = Math.max(dist + 300, dist * 1.5);
        Polyline route = graph.FindPath(e1, p1, e2, p2, maxDist);
        if (route != null)
        {
            double routeLength = route.getLength();
            if (routeLength < maxDist)
            {
                diff = Math.abs(dist - routeLength);
                //get prob with diff
                //prob = 1 / beta * Math.Exp(-diff / beta);
                prob = diff * sBeta;
            }
        }
        return prob;
    }
    /// <summary>
    /// Get the simplified ln version of emission prob
    /// </summary>
    /// <param name="e"></param>
    /// <param name="point"></param>
    /// <returns></returns>
    private double getEmissionProbility(Edge e, GeoPoint point)
    {
        double prob = Double.NEGATIVE_INFINITY;
        TypeObject typeObj = new TypeObject(0);

        double distance2 = e.Dist2From(point, typeObj);

        if (Math.abs(typeObj.type) < 1)
        {
            //penalty
            if (typeObj.type != 0)
            {
                distance2 *= 1.44;
            }
            prob = -0.5 * distance2 * sSigma;
        }
        else
        {
            prob = Double.NEGATIVE_INFINITY;
        }
        return prob;
    }

    private HashSet<Edge> getCandidateEdges(GeoPoint p, double radius)
    {
        double maxRadius = MAX_RADIUS;
        HashSet<Edge> cands = graph.RangeQuery(p, radius, maxRadius);
        Iterator<Edge> candsIter = cands.iterator();
        while(candsIter.hasNext())
        {
        	GeoPoint result=GeoPoint.INVALID();
            int  type = candsIter.next().projectFrom(p, result);
            if (type != 0)
            {
            	candsIter.remove();
            }
        }
        return cands;
    }
    private void setMatchResult(HashMap<Long, Node> T)
    {
        //Debug.Assert(n != null);
        //1. Find the path with maximum prob
        Node maxNode = null;
        double maxVal = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Long, Node> pair : T.entrySet())
        {
            if (pair.getValue().prob > maxVal)
            {
                maxVal = pair.getValue().prob;
                maxNode = pair.getValue();
            }
        }
        //Debug.Assert(maxNode != null);
        //2. set match result
        while (maxNode != null)
        {
            mvs.get(maxNode.idx).e = graph.getEdges().get(maxNode.edgeId);
            mvs.get(maxNode.idx).type = MotionVector.MatchType.SingleMatched;
            maxNode = maxNode.parent;
        }
    }
	
	 //Set match result
    //private MotionVector[] mvs = null;
    private List<MotionVector> mvs=null;
    //Parameter of emission prob
    final static double sigma = 10;
    final static double sSigma = 1 / (sigma * sigma);    // deviation
    final double alpha = -Math.log(Math.sqrt(2 * Math.PI) * sigma);
    final static double beta = 5;
    final static double sBeta = -1 / beta;
    final double lnBeta = Math.log(beta);
    private static final Logger log=Logger.getLogger(MM.class);

    int maxInterval = 300;//5min
    final static int MAX_RADIUS = 100;
    final static int RADIUS = 40;

	 private class Node
     {
         public Node parent = null;
         public double prob;
         public long edgeId;
         public int idx;
         public Node(double prob, long edgeId, int idx, Node parent)
         {
             this.parent = parent;
             this.prob = prob;
             this.idx = idx;
             this.edgeId = edgeId;
         }
         public String toString()
         {
        	 return (new MessageFormat("Edge:{0},Prob:{1},Idx:{2}")).format(new Object[]{edgeId, prob, idx});
         }
     }

}
