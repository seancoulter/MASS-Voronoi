/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mass;

import java.awt.geom.Point2D;

/**
 *
 * @author seane
 */
public class Edge {
        //an edge is one of the following:
        // a line (bisecting a line segment at a midpoint Point)
        // a half line (from x1,y1 to infinity in slope direction)
        // a line segment from x1,y1 to x2, y2
        public double x1;
        public double x2;
        public double y1;
        public double y2;
        public double slope;
        Point2D.Double midpoint= null;
        
        public Edge() {
        
        }
        
        public Edge(double x1, double y1, double x2, double y2) {
            this.x1= x1;
            this.y1= y1;
            this.x2= x2;
            this.y2= y2;
        }
        
        public Edge(Point2D.Double left_point, Point2D.Double right_point) {
            setPoints(left_point, right_point);
        }
        
        public Edge(double slope, Point2D.Double midpoint) {
            
        }
        
        public Point2D.Double getLeftPoint() {
            return new Point2D.Double(x1, y1);
        }
        
        public Point2D.Double getRightPoint() {
            return new Point2D.Double(x2, y2);
        }
        
        public void setPoints(Point2D.Double left_point, Point2D.Double right_point) {
            x1= left_point.getX();
            y1= left_point.getY();
            x2= right_point.getX();
            y2= right_point.getY();
        }
    }
