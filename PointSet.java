package mass;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Iterator;


/**
 *
 * @author seane
 */
public class PointSet extends Place {
    
    private class Edge {
        //an edge is one of the following:
        // a line (bisecting a line segment at a midpoint Point)
        // a half line (from x1,y1 in slope direction)
        // a line segment from x1,y1 to x2, y2
        public double x1;
        public double x2;
        public double y1;
        public double y2;
        public double slope;
        Point midpoint= null;
        
        public Edge(double x, double y) {
            this.x= x;
            this.y= y;
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
   
    private ArrayList<Point> points;
    private Edge[] convex_hull;
    private Edge[] voronoi_edges;
    
    public static final int COMPUTE_VORONOI= 0;
    public static final int MERGE_VORONOI= 1;
    public static final int GET_HOSTNAME= 2;
    public static final int FINAL_MERGE= 3;
    
    public PointSet(Object o) {
        points= new ArrayList<Point>();
    } 
    
    public Object callMethod(int method, Object o) {
        switch(method) {
            case COMPUTE_VORONOI:
                return compute_voronoi(o);
            case MERGE_VORONOI:
                return merge_voronoi(o);
            case GET_HOSTNAME:
                return null;
            case FINAL_MERGE:
                return final_merge(o);
            default:
                return new String("Error in callMethod");
        }
    }
    
    private int length() {
        return points.size();
    }
    
    private PointSet[] cut_in_half(PointSet p) {
        PointSet[] ret= new PointSet[2];
        int half= p.length() / 2;
        Iterator<Point> it= p.points.iterator();
    }
    
    private EdgeList bisect(PointSet p) {
        double x1= p.points.get(0).x;
        double y1= p.points.get(0).y;
        double x2= p.points.get(1).x;
        double y2= p.points.get(1).y;
        EdgeList el= new EdgeList();
        el.addToList(new Edge(-1/(y2-y1)/(x2-x1), new Point((x2-x1)/2, (y2-y1)/2)));
        return el;
    }
    
    //compute voronio map for a given set of points
    //given a PointSet with n points, this function returns an EdgeList object holding n-1 edges
    public EdgeList compute_voronoi(PointSet ps) {
        if(ps.length() == 2 || ps.length() == 1) {
            return ps.bisect(ps);
        }
        PointSet[] splitPoints= cut_in_half(ps);
        PointSet Sl= splitPoints[0];
        PointSet Sr= splitPoints[1];
        EdgeList Vl= compute_voronoi(Sl);
        EdgeList Vr= compute_voronoi(Sr);
        return merge_voronoi(Vl, Vr, Sl, Sr);
    }
    
    //merge 2 voronoi maps
    public EdgeList merge_voronoi(EdgeList v1, EdgeList v2, PointSet s1, PointSet s2) {
        ConvexHull ch1= get_convex_hull(s1);
        ConvexHull ch2= get_convex_hull(s2);
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
    
    
    public Object[] merge_voronoi(Object[] unmergedEdges, int leftsubarray, int rightsubarray) {        
        
    }
    
    public void final_merge(Object o) {
        //i+1 to i for every pair
        //i+2 to i for every 3rd
        //i+4
        
    }
    
}