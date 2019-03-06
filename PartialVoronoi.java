package mass;

import edu.uw.bothell.css.dsl.MASS.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author seane
 */
public class PartialVoronoi extends Place {
    
    //this class contains:
    // point_set: set of points in this partial voronoi diagram
    // edge_list: set of voronoi edges in this PVD
    
    private class Edge {
        //an edge is one of the following:
        // a line (bisecting a line segment at a midpoint Point)
        // a half line (from x1,y1 to infinity in slope direction)
        // a line segment from x1,y1 to x2, y2
        public double x1;
        public double x2;
        public double y1;
        public double y2;
        public double slope;
        Point midpoint= null;
        
        public Edge(double x1, double y1, double x2, double y2) {
            this.x1= x1;
            this.y1= y1;
            this.x2= x2;
            this.y2= y2;
        }
        
        public Edge(double slope, Point midpoint) {
            
        }
    }
    
    private class EdgeList {
        private ArrayList<Edge> list;
        
        public EdgeList() {
           list= new ArrayList<Edge>();
        }
        
        public void addToList(Edge e) {
            list.add(e);
        }
    }
    
    private ArrayList<Point> point_set;
    private EdgeList convex_hull;
    private EdgeList voronoi_edges;
    Vector<int[]> neighbors;
    
    public static final int COMPUTE_VORONOI= 0;
    public static final int MERGE_VORONOI= 1;
    public static final int GET_HOSTNAME= 2;
    public static final int FILL_NEIGHBOR_BUFFER= 3;
    
    
    public PartialVoronoi(Object o) {
        point_set= new ArrayList<Point>();
        voronoi_edges= new EdgeList();
        init_neighbors();
    } 
    
    private PartialVoronoi(ArrayList<Point> points, EdgeList e) {
        point_set= points;
        voronoi_edges= e;
        init_neighbors();
    }
    
    //Places instantiatiion
    public PartialVoronoi(ArrayList<ArrayList<Point>> points) {
        //retrieve corresponding arraylist of points
        point_set= points.get(getIndex()[0]);
        init_neighbors();  //specigfy before exchagne all
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
    
    private ArrayList<ArrayList<Point>> cut_in_half(ArrayList<Point> p) {
        ArrayList<ArrayList<Point>> ret= new ArrayList<ArrayList<Point>>();
        int half= p.size() / 2;
        Iterator<Point> it= p.iterator();
        ret.add(new ArrayList<Point>());
        ret.add(new ArrayList<Point>());
        for(int i= 0; i < half; ++i) ret.get(0).add(p.get(i));
        for(int i= half; i < p.size(); ++i) ret.get(1).add(p.get(i));
        return ret;
    }
    
    private PartialVoronoi bisect(ArrayList<Point> p) {
        double x1= p.get(0).x;
        double y1= p.get(0).y;
        double x2= p.get(1).x;
        double y2= p.get(1).y;
        EdgeList edge_list= new EdgeList();
        edge_list.addToList(new Edge(-1/(y2-y1)/(x2-x1), new Point((x2-x1)/2, (y2-y1)/2)));
        return new PartialVoronoi(p, edge_list);
    }
    
    //compute voronio map for a given set of points
    //given a PointSet with n points, this function returns an EdgeList object holding n-1 edges
    public PartialVoronoi compute_voronoi(Object pointset) {
        ArrayList<Point> ps= (ArrayList<Point>) pointset;
        if(ps.size() == 2 || ps.size() == 1)  return bisect(ps);
        if(ps.size() < 1) return null;
        ArrayList<ArrayList<Point>> splitPoints= cut_in_half(ps);
        ArrayList<Point> s_left= splitPoints.get(0);
        ArrayList<Point> s_right= splitPoints.get(1);
        PartialVoronoi v_left= compute_voronoi(s_left);
        PartialVoronoi v_right= compute_voronoi(s_right);
        return merge_voronoi(v_left, v_right);
    }
    
    //merge 2 voronoi maps
    public PartialVoronoi merge_voronoi(PartialVoronoi v1, PartialVoronoi v2) {        
        ConvexHull ch1= get_convex_hull(v1.point_set);
        ConvexHull ch2= get_convex_hull(v2.point_set);
        Edge csl= compute_support_line(ch1,ch2);
        Point p= csl[0];
        Point q= csl[1];
        Edge bisector= new Edge();
        while(csl != null) {
            bisector= bisect(csl);
            Point sl_intersection= v1.intersection(bisector);  //bisector intersection with vl
            Point sr_intersection= v2.intersection(bisector);  //bisector intersection with v2
            if(sl_intersection.y < sr_intersection.y) {  //intersected with v1
                p= sl_intersection;
            }
            else {              //intersected with v2
                q= sr_intersection;
            }
            csl= make_new_csl(p,q)
        }
        return trim(v1, v2);
    }
    
    public PartialVoronoi merge_voronoi_wrapper(Object global_step) {       
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
    
    public PartialVoronoi getNeighborPartialVoronoi() {
        return this;
    }
    
}