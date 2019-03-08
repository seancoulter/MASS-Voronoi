package mass;

import edu.uw.bothell.css.dsl.MASS.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;
import java.awt.geom.Point2D.Double;

/**
 *
 * @author seane
 */
public class PartialVoronoi extends Place {
    //this class contains:
    // point_set: set of points in this partial voronoi diagram
    // edge_list: set of voronoi edges in this PVD
    
    private class EdgeList {
        private ArrayList<Edge> list;
        
        public EdgeList() {
           list= new ArrayList<Edge>();
        }
        
        public void addToList(Edge e) {
            list.add(e);
        }
    }
    
    private ArrayList<Double> point_set;
    private List<Double> convex_hull;
    private EdgeList voronoi_edges;
    Vector<int[]> neighbors;
    
    public static final int COMPUTE_VORONOI= 0;
    public static final int MERGE_VORONOI= 1;
    public static final int GET_HOSTNAME= 2;
    public static final int FILL_NEIGHBOR_BUFFER= 3;
    
    
    public PartialVoronoi(Object o) {
        point_set= new ArrayList<Double>();
        voronoi_edges= new EdgeList();
        init_neighbors();
        Object[] temp= point_set.toArray();
        convex_hull= ConvexHull.convexHull((Double[]) temp, point_set.size());
    } 
    
    private PartialVoronoi(ArrayList<Double> points, EdgeList e) {
        point_set= points;
        voronoi_edges= e;
        init_neighbors();
        Object[] temp= point_set.toArray();
        convex_hull= ConvexHull.convexHull((Double[]) temp, point_set.size());
    }
    
    //Places instantiatiion
    public PartialVoronoi(ArrayList<ArrayList<Double>> points) {
        //retrieve corresponding arraylist of points
        point_set= points.get(getIndex()[0]);
        init_neighbors();  //specigfy before exchagne all
        Object[] temp= point_set.toArray();
        convex_hull= ConvexHull.convexHull((Double[]) temp, point_set.size());
    }
    
    private void init_neighbors() {
        double idx= Math.ceil(Math.log10(getIndex()[0]) / Math.log10(2));
        if(idx != Math.floor(idx)) return;     //idx not a power of 2: has no neighbors
        neighbors= new Vector<int[]>();
        int[] nbr= new int[1];
        for(int i= getIndex()[0]+1; i < main.NUM_PLACES; i*=2) {
            nbr[0]= i;
            neighbors.add(nbr);
        }
    }
    
    public Object callMethod(int method, Object o) {
        switch(method) {
            case COMPUTE_VORONOI:
                return compute_voronoi(o);
            case MERGE_VORONOI:
                return merge_voronoi_wrapper(o);
            case GET_HOSTNAME:
                return null;
            case FILL_NEIGHBOR_BUFFER:
                return getNeighborPartialVoronoi();
            default:
                return new String("Error in callMethod");
        }
    }
    
    private ArrayList<ArrayList<Double>> cut_in_half(ArrayList<Double> p) {
        ArrayList<ArrayList<Double>> ret= new ArrayList<ArrayList<Double>>();
        int half= p.size() / 2;
        Iterator<Double> it= p.iterator();
        ret.add(new ArrayList<Double>());
        ret.add(new ArrayList<Double>());
        for(int i= 0; i < half; ++i) ret.get(0).add(p.get(i));
        for(int i= half; i < p.size(); ++i) ret.get(1).add(p.get(i));
        return ret;
    }
    
    private PartialVoronoi bisect(ArrayList<Double> p) {
        double x1= p.get(0).x;
        double y1= p.get(0).y;
        double x2= p.get(1).x;
        double y2= p.get(1).y;
        EdgeList edge_list= new EdgeList();
        edge_list.addToList(new Edge(-1/(y2-y1)/(x2-x1), new Double((x2-x1)/2, (y2-y1)/2)));
        return new PartialVoronoi(p, edge_list);
    }
    
    private Edge bisect(Edge line) {
        Double left= line.getLeftPoint();
        Double right= line.getRightPoint();
        return new Edge(-1/(right.getY()-left.getY())/(right.getX()-left.getX()), 
                new Double(right.getX()-left.getX()/2, (right.getY()-left.getY())/2));
    }
    
    //compute voronio map for a given set of points
    //given a PointSet with n points, this function returns an EdgeList object holding n-1 edges
    private PartialVoronoi compute_voronoi(Object pointset) {
        ArrayList<Double> ps= (ArrayList<Double>) pointset;
        if(ps.size() == 2 || ps.size() == 1)  return bisect(ps);
        if(ps.size() < 1) return null;
        ArrayList<ArrayList<Double>> splitPoints= cut_in_half(ps);
        ArrayList<Double> s_left= splitPoints.get(0);
        ArrayList<Double> s_right= splitPoints.get(1);
        PartialVoronoi v_left= compute_voronoi(s_left);
        PartialVoronoi v_right= compute_voronoi(s_right);
        return merge_voronoi(v_left, v_right);
    }
    
    private List<Double> get_convex_hull() {
        return convex_hull;
    }
    
    private Edge compute_support_line(List<Double> hull1, List<Double> hull2) {
        int l_idx= hull1.size()-1;
        int r_idx= 0;
        Double u= hull1.get(l_idx);
        Double v= hull2.get(r_idx);
        Edge ret= new Edge(u,v);
        boolean found_left= false, found_right= false;
        while(!found_left && !found_right) {
            Double test_left= hull1.get(l_idx--);
            Double test_right= hull2.get(r_idx++);
            if(test_left.getY() < ret.getLeftPoint().getY() || test_left.getY() < ret.getRightPoint().getY()) {
                ret.setPoints(test_left, v);
                found_left= false;
            }
            else found_left= true;
            if(test_right.getY() < ret.getLeftPoint().getY() || test_right.getY() < ret.getRightPoint().getY()) {
                ret.setPoints(u, test_right);
                found_right= false;
            }
            else found_right = true;
        }
        return ret;
    }
    
    private Double intersection(Edge bisector) {
        //point of intersection between this voronoi edges and bisector
        return null;
    }
    
    //merge 2 voronoi maps
    private PartialVoronoi merge_voronoi(PartialVoronoi v1, PartialVoronoi v2) {        
        List<Double> cvh_lhs= v1.get_convex_hull();
        List<Double> cvh_rhs= v2.get_convex_hull();
        Edge csl= compute_support_line(cvh_lhs, cvh_rhs);
        Double p= csl.getLeftPoint();
        Double q= csl.getRightPoint();
        Edge bisector= new Edge();
        EdgeList stichLine= new EdgeList();
        while(csl != null) {
            bisector= bisect(csl);
            Double sl_intersection= v1.intersection(bisector);  //bisector intersection with vl
            Double sr_intersection= v2.intersection(bisector);  //bisector intersection with v2
            if(sl_intersection.y < sr_intersection.y) {  //intersected with v1
                p= sl_intersection;
            }
            else {              //intersected with v2
                q= sr_intersection;
            }
            stichLine.addToList(bisector);  //todo: cut off bisector at intersection point
            csl.setPoints(p,q);
        }
        return trim(v1, v2, stichLine);
    }
    
    private PartialVoronoi merge_voronoi_wrapper(Object global_step) {       
        int step= (int) global_step;
        PartialVoronoi rhs= null;
        PartialVoronoi[] neighbor= (PartialVoronoi[]) getInMessages();
        if(neighbor != null) {
           if(neighbor[0] != null) {     //retrieve next PartialVoronoi to merge
                rhs= neighbor[0];
            }
        } 
        else return null;               //node is exhausted (no remaining neighbors)
        return merge_voronoi(this, rhs);
    }
    
    private PartialVoronoi getNeighborPartialVoronoi() {
        return this;
    }
    
    private PartialVoronoi trim(PartialVoronoi v1, PartialVoronoi v2, EdgeList stichLine) {
        return this;
    }
    
}


//trim
//intersection