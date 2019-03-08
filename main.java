package mass;


import edu.uw.bothell.css.dsl.MASS.*;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import mass.PartialVoronoi;

import java.awt.geom.Point2D.Double;

public class main {

    private static final String NODE_FILE = "nodes.xml";
    public static int NUM_PLACES;
    
    public static void main(String[] args) {
            
        MASS.setNodeFilePath(NODE_FILE);
        MASS.setLoggingLevel( LogLevel.DEBUG );

        //start a process at each computing node. each process spawns same # threads as that of its CPU cores
        //map grid to processess within MASS.init
        MASS.getLogger().debug( "main.java initializing MASS..." );
        MASS.init();
        MASS.getLogger().debug( "MASS initialized" );
        
        //holds x,y coordinates of each point in the input file
        ArrayList<Double> xy_points= new ArrayList<Double>();
        
        //read input file, save contents to point_arr
        if(!read_infile(args[0], xy_points)) {
            MASS.getLogger().debug("problem parsing input file");
            System.exit(-1);
        }
        //sort individual points by increasing x value
        Collections.sort(xy_points, new Sorter());
        
        //number of Place objects is log2 of the total number of points
        int num_places= (int)Math.ceil(Math.log10(xy_points.size()) / Math.log10(2));
        
        NUM_PLACES= num_places;
        
        ArrayList<ArrayList<Double>> grouped_points= new ArrayList<ArrayList<Double>>();
        
        // add first pair of Points as adding 1 pointset to grouped_points
        
        //group remaining points into PointSet objects and fill point_list with these PointSets
        for(int i= 0, j=xy_points.size()/num_places; i < xy_points.size()/num_places; i++) {
            for(int k= i*num_places; k <= j; k++)
                grouped_points.get(i).add(xy_points.get(k));      //add each pair to the group
        }
        //long start = System.currentTimeMillis();
        
        Places places = new Places(1, PartialVoronoi.class.getName(), grouped_points, num_places);
        // instruct all places to return the hostnames of the machines on which they reside
	Object[] placeCallAllObjs = new Object[num_places];
	MASS.getLogger().debug( "Voronoi sending callAll to Places..." );
	Object[] calledPlacesResults = ( Object[] ) places.callAll( PartialVoronoi.GET_HOSTNAME, placeCallAllObjs );
	MASS.getLogger().debug( "Places callAll operation complete" );


        // 8 nodes 4 cores
        places.callAll(PartialVoronoi.COMPUTE_VORONOI);         //obtain voronoi edges from all computing nodes
        Vector<int[]> neighbors= new Vector<int[]>();

        for(int i= 1; i < num_places; i*=2) {
            
            //fill each PointSet's neighbor buffer with neighbor data
            // to gather 2 poinstsets into the smaller index
            places.exchangeAll(places.getHandle(), PartialVoronoi.FILL_NEIGHBOR_BUFFER); // and ignore message from i to i+1, only get message from i+1 to i

            //each index of merged contains a merged PartialVoronoi from node i and its corresponding node (i+1, i+2, i+4, i+8..)
            Object[] merged= places.callAll(PartialVoronoi.MERGE_VORONOI, new Object[num_places]);
            //set all of the merged PartialVoronoi to their correct indeces in Places
            //places[0]= merged[0];

            //for(int j= i; j < num_places; j*=i+1) places[j]= merged[j];
        }

        //places[0] now contains complete voronoi edges

        //long finish = System.currentTimeMillis();

        MASS.getLogger().debug( "We're through" );
        MASS.finish();
    
    }
    
    private static boolean read_infile(String inp, ArrayList<Double> points) {
        File input= new File(inp);
        Pattern x_y_coord= Pattern.compile("(0|1)\\.(\\d)*,(0|1)\\.(\\d)*");
        try {
            Scanner s= new Scanner(input);
            while(s.hasNextLine()) {                           
                try {
                    String point= s.next(x_y_coord);
                    System.out.println(point);
                    String[] a= point.split(",");
                    points.add(new Double(java.lang.Double.parseDouble(a[0]), java.lang.Double.parseDouble(a[1])));
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
    
    static class Sorter implements Comparator<Double> {
        @Override
        public int compare(Double a, Double b) {
            return java.lang.Double.compare(a.getX(), b.getX());
        }
    }
    
}