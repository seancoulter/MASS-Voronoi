import edu.uw.bothell.css.dsl.MASS.Voronoi;
import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import mass.PointSet;
import mass.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;


/*      
    -executon pipeline:
        compute bisectors -> convex hull -> lcsl -> merge -> exchange



TODO:
    -define compute_voronoi
    -define lcsl finder
    -convex hull method
    -merge method

*/

public class main {

    private static final String NODE_FILE = "nodes.xml";
    
    public static void main(String[] args) {
            
        MASS.setNodeFilePath(NODE_FILE);
        MASS.setLoggingLevel( LogLevel.DEBUG );

        //start a process at each computing node. each process spawns same # threads as that of its CPU cores
        //map grid to processess within MASS.init
        MASS.getLogger().debug( "main.java initializing MASS..." );
        MASS.init();
        MASS.getLogger().debug( "MASS initialized" );
        
        //holds x,y coordinates of each point in the input file
        ArrayList<Point> xy_points= new ArrayList<Point>();
        
        //read input file, save contents to point_arr
        if(!read_infile(args[0], xy_points)) {
            MASS.getLogger().debug("problem parsing input file");
            System.exit(-1);
        }
        //sort individual points by increasing x value
        Collections.sort(xy_points, new Sorter());
        
        //number of Place objects is log2 of the total number of points
        int num_places= (int)Math.ceil(Math.log10(xy_points.size()) / Math.log10(2));
        
        ArrayList<PointSet> grouped_points= new ArrayList<PointSet>();
        
        // add first pair of Points as adding 1 pointset to grouped_points
        grouped_points.add(new PointSet(xy_points[0]));
        
        //group remaining points into PointSet objects and fill point_list with these PointSets
        for(int i= 1; i < xy_points.size(); i*=2+1) {
            PointSet group= new PointSet();        //represents a group of xy pairs
            for(int j= i; j <= i*2; j++)
                group.add(xy_points.get(j));      //add each pair to the group
            grouped_points.add(group);            //add the group to the PointSet list
        }
        //long start = System.currentTimeMillis();
        
        Places places = new Places(1, PointSet.class.getName(), (Object)grouped_points, num_places);
        
        // instruct all places to return the hostnames of the machines on which they reside
	Object[] placeCallAllObjs = new Object[num_places];
	MASS.getLogger().debug( "Voronoi sending callAll to Places..." );
	Object[] calledPlacesResults = ( Object[] ) places.callAll( PointSet.GET_HOSTNAME, placeCallAllObjs );
	MASS.getLogger().debug( "Places callAll operation complete" );
        
        //doall to replace callall
        // 8 nodes 4 cores
        Object[] unmergedEdges = (Object[]) places.callAll(PointSet.COMPUTE_VORONOI);                   //obtain voronoi edges from all computing nodes
        Object voronoi_diagram= places.callAll(PointSet.MERGE_VORONOI, Arrays.copyOfRange(unmergedEdges, 0, 1));
        //reduce unmergedEdges as merging each index with voronoi_diagram
        for(int i= 2; i < num_places; ++i) {
            voronoi_diagram = (Object[]) places.callAll(PointSet.MERGE_VORONOI, voronoi_diagram, unmergedEdges[i]);       //merge voronoi edges from all computing nodes
            places.exchangeall(places.getHandle(), PointSet.FINAL_MERGE); // and ignore message from i to i+1, only get message from i+1 to i
        }
        
        callall(partialvoronoi);
        for(log places)
            exchangeall(receive_, nbr);
            callall(merge)

        //voronoi_diagram now contains complete voronoi edges
        
        //long finish = System.currentTimeMillis();
        
        MASS.getLogger().debug( "We're through" );
        MASS.finish();
              
    }
    
    private static boolean read_infile(String inp, ArrayList<Point> points) {
        File input= new File(inp);
        Pattern x_y_coord= Pattern.compile("(0|1)\\.(\\d)*,(0|1)\\.(\\d)*");
        try {
            Scanner s= new Scanner(input);
            while(s.hasNextLine()) {                           
                try {
                    String point= s.next(x_y_coord);
                    System.out.println(point);
                    String[] a= point.split(",");
                    points.add(new Point(Double.parseDouble(a[0]), Double.parseDouble(a[1])));
                }
                catch(InputMismatchException e) {
                    System.out.println("problem with text file format"); System.exit(-1);
                }
            }
            s.close();
            return true;
        }
        catch(FileNotFoundException e) { 
            System.out.println("invalid file"); 
                return false;
        }
        
    }
    
    static class Sorter implements Comparator<Point> {
        @Override
        public int compare(Point a, Point b) {
            return Double.compare(a.x, b.x);
        }
    }
    
}