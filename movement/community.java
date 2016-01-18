/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package movement;
import java.util.ArrayList;
import core.Tuple;

/**
 *
 * @author Zunnun Narmawala
 */
public class community {
    public Integer MNo[]=new Integer[CAHM.nrofHosts];
    ArrayList<Integer> availableSpace=new ArrayList();
    public ArrayList<Integer> commSize=new ArrayList();
    ArrayList<Integer> membershipCount=new ArrayList();
    public ArrayList<Integer> membership[];
    public ArrayList<Integer> nodeInCommunities[]=new ArrayList[CAHM.nrofHosts];
    public ArrayList<Integer> localDegree[]=new ArrayList[CAHM.nrofHosts];
    ArrayList<coordinate> coord[]=new ArrayList[CAHM.nrofHosts];
    ArrayList<Integer> community[]=new ArrayList[CAHM.nrofHosts];
    ArrayList<Integer> partofOverlap[];
    ArrayList<coordinate> commCells[];
    public double [][] avgDegree=new double[CAHM.nrofHosts][];
    ArrayList<Integer> normals[];
}
