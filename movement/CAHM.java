/*
 * Written by Zunnun Narmawala
 *
 */
package movement;

import core.Coord;
import core.Settings;
import core.powerLawRNG;
import core.DTNHost;
import core.DTNSim;
import core.SimClock;
import core.Tuple;
import core.powerLawRNG1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

/**
 * Community Aware Heterogeneous Human Mobility Model (CAHM). Associated
 * Publication:
 */
public class CAHM extends MovementModel {

    public static final String K_CLIQUE = "k-clique";
    public static final String MEMBERSHIP_EXPONET = "MN-Exponent";
    public static final String OVERLAP_EXPONET = "OV-Exponent";
    public static final String COMMUNITY_SIZE_EXPONET = "CSize-Exponent";
    public static final String LOCAL_DEGREE_EXPONET = "Local-Exponent";
    public static final String PAUSE_TIME_EXPONENT = "PT-Exponent";
    public static final String NO_OF_PERIODS = "noofPeriods";
    public static final String PERIODS = "periods";
    public static final String HUB_THRESHOLD = "hubThreshold";
    public static final String GATEWAY_THRESHOLD = "gatewayThreshold";
    public static int[] nrofComm;
    private Coord lastWaypoint;
    public static int nrofHosts;
    private int k_clique;
    private double MN;
    private double OV;
    private double CSize;
    private double Local;
    private double lambda;
    private double PT;
    private double FL;
    private double ADT;
    private double timeConstant[] = new double[2];
    private double expConstant[] = new double[2];
    private double flightLengthThreshold;
    private int cellSize;
    public static int noofPeriods;
    public int[] periods;
    private int totalPeriod;
    public static community[] cmnty;
    public static ArrayList<overlap>[] overlapList;
    public static double[] minDistance;
    private int address;
    public int currentPeriod = 0;
    private int iteration = 0;
    private int previousIndex;
    private ArrayList<Tuple<Integer, Double>> distanceList[][];
    private ArrayList<Tuple<Integer, Double>> communityList[][];
    public int currentCommunity;
    private int previousCommunity;
    private double previousTime;
    private double avgSpeed = 0;
    public boolean isHub;
    public boolean isGateway;
    public static double lastUpdate = 0;
    /**
     * namespace for host group settings ({@value})
     */
    public static final String GROUP_NS = "Group";
    /**
     * group id -setting id ({@value})
     */
    public static final String GROUP_ID_S = "groupID";
    /**
     * number of hosts in the group -setting id ({@value})
     */
    public static final String NROF_HOSTS_S = "nrofHosts";

    static {
        DTNSim.registerForReset(CAHM.class.getCanonicalName());
    }

    public CAHM(Settings settings) {
        super(settings);
        settings.setNameSpace(MOVEMENT_MODEL_NS);
        k_clique = settings.getInt("k-clique");
        MN = settings.getDouble("MN-Exponent");
        OV = settings.getDouble("OV-Exponent");
        CSize = settings.getDouble("CSize-Exponent");
        Local = settings.getDouble("Local-Exponent");
        lambda = settings.getDouble("noofCells-Constant");
        PT = settings.getDouble("PT-Exponent");
        FL = settings.getDouble("FL-Exponent");
        timeConstant = settings.getCsvDoubles("timeConstant");
        expConstant = settings.getCsvDoubles("exponentConstant");
        flightLengthThreshold = settings.getDouble("flightLengthThreshold");
        ADT = settings.getDouble("ADT");
        noofPeriods = settings.getInt(NO_OF_PERIODS);
        periods = new int[noofPeriods];
        periods = settings.getCsvInts(PERIODS, noofPeriods);
        cellSize = settings.getInt("cellSize");
        settings.restoreNameSpace();
        totalPeriod = 0;
        for (int i = 0; i < noofPeriods; i++) {

            totalPeriod += periods[i] * 3600;
            periods[i] = totalPeriod;
        }
        previousCommunity = -1;
        previousTime = 0;
        Settings s = new Settings(GROUP_NS + 1);
        s.setSecondaryNamespace(GROUP_NS);
        nrofHosts = s.getInt(NROF_HOSTS_S);
        CAHM.cmnty = new community[noofPeriods];
        CAHM.overlapList = new ArrayList[noofPeriods];
        CAHM.nrofComm = new int[noofPeriods];
        CAHM.minDistance = new double[noofPeriods];
        for (int j = 0; j < noofPeriods; j++) {
            CAHM.overlapList[j] = new ArrayList<overlap>();
            CAHM.cmnty[j] = new community();
            CAHM.minDistance[j] = Double.MAX_VALUE;
        }
        for (int k = 0; k < noofPeriods; k++) {
            generateCommunity(k);
            geographicMapping(k);
        }
    }

    protected CAHM(CAHM cahm) {
        super(cahm);
        this.k_clique = cahm.k_clique;
        this.MN = cahm.MN;
        this.OV = cahm.OV;
        this.CSize = cahm.CSize;
        this.Local = cahm.Local;
        this.PT = cahm.PT;
        this.FL = cahm.FL;
        this.ADT = cahm.ADT;
        this.timeConstant = cahm.timeConstant;
        this.expConstant = cahm.expConstant;
        this.flightLengthThreshold = cahm.flightLengthThreshold;
        this.cellSize = cahm.cellSize;
        this.periods = new int[noofPeriods];
        this.periods = cahm.periods;
        this.totalPeriod = cahm.totalPeriod;
        this.previousCommunity = -1;
        this.previousTime = 0;
        this.lambda = cahm.lambda;
    }

    public static void reset() {
    }

    /**
     * Generates and returns a suitable power-law waiting time at the end of a
     * path. (i.e. random variable whose value is between min and max of the
     * {@link #WAIT_TIME} setting).
     *
     * @return The time as a double
     */
    @Override
    public double generateWaitTime() {
        powerLawRNG1 PLRngPT;
        if (rng == null) {
            return 0;
        } else {
            PLRngPT = new powerLawRNG1(rng, PT, minWaitTime, maxWaitTime);
        }
        double PTTime = PLRngPT.getDouble1();
        return PTTime;
    }

    /**
     * Returns a possible (random) placement for a host
     *
     * @return Random position on the map
     */
    @Override
    public Coord getInitialLocation() {
        calculateSortDistances();
        assert rng != null : "MovementModel not initialized!";
        int index = rng.nextInt(cmnty[currentPeriod].coord[address].size());
        coordinate coordnt = new coordinate();
        coordnt = cmnty[currentPeriod].coord[address].get(index);
        previousIndex = index;
        Coord c = new Coord((coordnt.x * cellSize) + (rng.nextDouble() * cellSize), (coordnt.y * cellSize) + (rng.nextDouble() * cellSize));
        this.lastWaypoint = c;
        return c;
    }

    @Override
    public Path getPath() {
        Coord c = lastWaypoint;
        c = generateCoordLevyWalk();
        Path p;
        p = new Path(generateSpeedLevyWalk(c));
        p.addWaypoint(lastWaypoint.clone());
        p.addWaypoint(c);
        this.lastWaypoint = c;
        return p;
    }

    @Override
    public CAHM replicate() {
        return new CAHM(this);
    }

    protected Coord generateCoordLevyWalk() {
        Boolean flag = false;
        if (SimClock.getTime() > ((iteration * totalPeriod) + periods[currentPeriod])) {
            flag = true;
            if (currentPeriod < noofPeriods - 1) {
                currentPeriod++;
            } else {
                iteration++;
                currentPeriod = 0;
            }
        }
        int index;
        coordinate coordnt = new coordinate();
        if (flag) {
            index = rng.nextInt(cmnty[currentPeriod].coord[address].size());
            coordnt = cmnty[currentPeriod].coord[address].get(index);

            previousIndex = index;
            flag = false;
        } else {
            if (cmnty[currentPeriod].coord[address].size() > 1) {
                double distance;
                powerLawRNG PLRngFL = new powerLawRNG(rng, FL, distanceList[currentPeriod][previousIndex].get(distanceList[currentPeriod][previousIndex].size() - 1).getValue());
                distance = PLRngFL.getDouble();
                int i;
                for (i = 0; i < distanceList[currentPeriod][previousIndex].size(); i++) {
                    if (distanceList[currentPeriod][previousIndex].get(i).getValue() >= distance) {
                        break;
                    }
                }
                if (i == 0) {
                    index = i;
                } else if (Math.abs(distanceList[currentPeriod][previousIndex].get(i).getValue() - distance) > Math.abs(distanceList[currentPeriod][previousIndex].get(i - 1).getValue() - distance)) {
                    index = i - 1;
                } else {
                    index = i;
                }
                coordnt = cmnty[currentPeriod].coord[address].get(distanceList[currentPeriod][previousIndex].get(index).getKey());

                previousIndex = distanceList[currentPeriod][previousIndex].get(index).getKey();
            } else {
                index = previousIndex;
                coordnt = cmnty[currentPeriod].coord[address].get(index);

            }
        }
        return new Coord((coordnt.x * cellSize) + (rng.nextDouble() * cellSize), (coordnt.y * cellSize) + (rng.nextDouble() * cellSize));
    }

    protected double generateSpeedLevyWalk(Coord c) {
        if (rng == null) {
            return 1;
        }
        double time;
        double distance = Math.sqrt(Math.pow(c.getX() - lastWaypoint.getX(), 2) + Math.pow(c.getY() - lastWaypoint.getY(), 2));
        if (distance < flightLengthThreshold) {
            time = timeConstant[0] * Math.pow(distance, (1 - expConstant[0]));
        } else {
            time = timeConstant[1] * Math.pow(distance, (1 - expConstant[1]));
        }
        return (distance / time);
    }

    protected Coord randomCoord() {
        return new Coord(rng.nextDouble() * getMaxX(),
                rng.nextDouble() * getMaxY());
    }

    protected int membershipNo(int no, powerLawRNG PLRng) {
        int S1 = 0;
        for (int i = 0; i < nrofHosts; i++) {
            cmnty[no].MNo[i] = (int) (Math.floor(PLRng.getDouble()) + 0.5);
            S1 += cmnty[no].MNo[i];
        }
        return S1;
    }

    public void setAddress(DTNHost host) {
        address = host.getAddress();
    }

    public void generateCommunity(int no) {
        powerLawRNG PLRngMN = new powerLawRNG(rng, MN, nrofHosts);
        powerLawRNG PLRngCSize = new powerLawRNG(rng, CSize, nrofHosts);
        powerLawRNG PLRngOV = new powerLawRNG(rng, OV, nrofHosts);

        /*---Initial Empty Community and Membership Number Establishment---*/
        int S1 = membershipNo(no, PLRngMN);
        int S2 = 0, maxCommSize = 0;
        Integer i = 0;
        do {

            cmnty[no].commSize.add(i, (int) (Math.floor(PLRngCSize.getDouble() + 0.5)) + k_clique - 2);
            cmnty[no].availableSpace.add(i, cmnty[no].commSize.get(i));
            if (maxCommSize < cmnty[no].commSize.get(i)) {
                maxCommSize = cmnty[no].commSize.get(i);
            }
            S2 += cmnty[no].commSize.get(i++);
            if (S2 == S1) {
                boolean flag = false;
                for (int j = 0; j < nrofHosts; j++) {
                    if (cmnty[no].MNo[j] > (i - 1)) {
                        flag = true;
                    }
                }
                if (flag == false) {
                    break;
                } else {
                    S1 = membershipNo(no, PLRngMN);
                    i = 0;
                    S2 = 0;
                    cmnty[no].commSize.clear();
                    maxCommSize = 0;
                }
            } else if (S2 > S1) {
                i = 0;
                S2 = 0;
                cmnty[no].commSize.clear();
                cmnty[no].availableSpace.clear();
                maxCommSize = 0;
            }

        } while (true);
        nrofComm[no] = i;

        /* Constructing Initial Overlap */
        Integer[] arrayMNo = new Integer[nrofHosts];
        Integer[] indexMNo = new Integer[nrofHosts];
        arrayMNo = Arrays.copyOf(cmnty[no].MNo, nrofHosts);
        for (i = 0; i < nrofHosts; i++) {
            indexMNo[i] = i;
        }
        int tmp;
        for (i = 0; i < nrofHosts - 1; i++) {
            for (int j = i + 1; j < nrofHosts; j++) {
                if (arrayMNo[i] < arrayMNo[j]) {
                    tmp = arrayMNo[i];
                    arrayMNo[i] = arrayMNo[j];
                    arrayMNo[j] = tmp;

                    tmp = indexMNo[i];
                    indexMNo[i] = indexMNo[j];
                    indexMNo[j] = tmp;
                }
            }
        }
        int count = 0;
        cmnty[no].membership = new ArrayList[nrofComm[no]];
        cmnty[no].partofOverlap = new ArrayList[nrofComm[no]];

        for (i = 0; i < nrofComm[no]; i++) {
            cmnty[no].membershipCount.add(i, 0);
            cmnty[no].membership[i] = new ArrayList<Integer>();
            cmnty[no].partofOverlap[i] = new ArrayList<Integer>();
        }
        for (i = 0; i < nrofHosts; i++) {
            cmnty[no].nodeInCommunities[i] = new ArrayList<Integer>();
            cmnty[no].localDegree[i] = new ArrayList<Integer>();
        }

        overlap oLap = new overlap();
        overlap oLap1;
        boolean exists = false;
        for (int p = 0; p < nrofHosts; p++) {
            i = indexMNo[p];
            if (cmnty[no].MNo[i] > 2) {
                int unSatuComm[] = new int[cmnty[no].MNo[i]];
                count = 0;
                ArrayList<Integer> tmpArray = new ArrayList<Integer>();
                for (int tmpIterate = 0; tmpIterate < nrofComm[no]; tmpIterate++) {
                    tmpArray.add(tmpIterate);
                }
                Collections.shuffle(tmpArray, rng);
                for (int tmpIterate = 0; tmpIterate < nrofComm[no]; tmpIterate++) {
                    Integer tempComm = tmpArray.get(tmpIterate);
                    if (cmnty[no].membershipCount.get(tempComm) < cmnty[no].commSize.get(tempComm) && !(cmnty[no].membership[tempComm].contains(i))) {
                        cmnty[no].availableSpace.set(tempComm, cmnty[no].availableSpace.get(tempComm) - 1);
                        cmnty[no].membership[tempComm].add(cmnty[no].membershipCount.get(tempComm), i);
                        cmnty[no].membershipCount.set(tempComm, cmnty[no].membershipCount.get(tempComm) + 1);
                        unSatuComm[count++] = tempComm;
                        cmnty[no].nodeInCommunities[i].add(tempComm);
                    }
                    if (count == cmnty[no].MNo[i]) {
                        break;
                    }
                }
                if (count != cmnty[no].MNo[i]) {
                    cmnty[no].commSize.clear();
                    cmnty[no].availableSpace.clear();
                    cmnty[no].membershipCount.clear();
                    overlapList[no].clear();
                    generateCommunity(no);
                    return;
                }

                for (int j = 0; j < count; j++) {
                    for (int l = j + 1; l < count; l++) {

                        for (int m = 0; m < overlapList[no].size(); m++) {
                            oLap1 = overlapList[no].get(m);
                            if ((oLap1.x == unSatuComm[j] && oLap1.y == unSatuComm[l]) || (oLap1.y == unSatuComm[j] && oLap1.x == unSatuComm[l])) {
                                oLap1.noofM3Nodes++;
                                oLap1.overlapMembership.add(i);
                                overlapList[no].set(m, oLap1);
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            oLap = new overlap();
                            oLap.x = unSatuComm[j];
                            oLap.y = unSatuComm[l];
                            oLap.overlapMembership.add(i);
                            oLap.noofM3Nodes = 1;
                            oLap.modified = false;
                            overlapList[no].add(oLap);
                            cmnty[no].partofOverlap[unSatuComm[j]].add(overlapList[no].size() - 1);
                            cmnty[no].partofOverlap[unSatuComm[l]].add(overlapList[no].size() - 1);
                        } else {
                            exists = false;
                        }
                    }
                }
            }
        }

        /*---Satisfying Power-Law Overlap Size Distribution---*/
        int noofM2Nodes = 0;
        ArrayList<Integer> M2NodesList = new ArrayList();
        for (i = 0; i < nrofHosts; i++) {
            if (cmnty[no].MNo[i] == 2) {
                noofM2Nodes++;
                M2NodesList.add(i);
            }
        }
        int size = overlapList[no].size();
        int countM2 = 0, modifiedCount = 0;

        int RV, index = 0, tempNode;
        boolean flag1 = false, flag2 = false;
        do {
            do {
                RV = (int) (Math.floor(PLRngOV.getDouble() + 0.5));
            } while (RV > maxCommSize);

            for (i = 0; i < overlapList[no].size(); i++) {
                oLap = overlapList[no].get(i);
                if (oLap.modified) {
                    continue;
                }
                if (oLap.noofM3Nodes == RV) {
                    oLap.modified = true;
                    modifiedCount++;
                    overlapList[no].set(i, oLap);
                    flag1 = true;
                    break;
                }
                if (oLap.noofM3Nodes < RV && (RV - oLap.noofM3Nodes) < cmnty[no].availableSpace.get(oLap.x) && (RV - oLap.noofM3Nodes) < cmnty[no].availableSpace.get(oLap.y)) {
                    index = i;
                    flag2 = true;
                }
            }
            if (flag1) {
                flag1 = false;
                flag2 = false;
            } else if (flag2) {
                oLap = overlapList[no].get(index);
                for (i = 0; i < RV - oLap.noofM3Nodes; i++) {
                    do {
                        tempNode = rng.nextInt(CAHM.nrofHosts);
                    } while (!M2NodesList.contains(tempNode));
                    cmnty[no].membership[oLap.x].add(cmnty[no].membershipCount.get(oLap.x), tempNode);
                    cmnty[no].availableSpace.set(oLap.x, cmnty[no].availableSpace.get(oLap.x) - 1);
                    cmnty[no].membershipCount.set(oLap.x, cmnty[no].membershipCount.get(oLap.x) + 1);
                    cmnty[no].membership[oLap.y].add(cmnty[no].membershipCount.get(oLap.y), tempNode);
                    cmnty[no].availableSpace.set(oLap.y, cmnty[no].availableSpace.get(oLap.y) - 1);
                    cmnty[no].membershipCount.set(oLap.y, cmnty[no].membershipCount.get(oLap.y) + 1);
                    cmnty[no].nodeInCommunities[tempNode].add(oLap.x);
                    cmnty[no].nodeInCommunities[tempNode].add(oLap.y);
                    oLap.overlapMembership.add(tempNode);
                    for (int j = 0; j < M2NodesList.size(); j++) {
                        if (M2NodesList.get(j) == tempNode) {
                            M2NodesList.remove(j);
                            break;
                        }
                    }
                    countM2++;
                    if (countM2 == noofM2Nodes) {
                        break;
                    }
                }
                oLap.modified = true;
                modifiedCount++;
                overlapList[no].set(index, oLap);
                flag2 = false;
            }

        } while (modifiedCount < size && countM2 < noofM2Nodes);

        if (countM2 < noofM2Nodes) {
            int comm1, comm2;
            boolean flag;
            int maxAvailable = 0;
            Integer[] tempArray = new Integer[cmnty[no].availableSpace.size()];
            cmnty[no].availableSpace.toArray(tempArray);
            Arrays.sort(tempArray, Collections.reverseOrder());
            maxAvailable = tempArray[1];
            boolean availableFlag = false;

            while (!M2NodesList.isEmpty()) {
                do {
                    do {
                        RV = (int) (Math.floor(PLRngOV.getDouble() + 0.5));
                    } while (RV > maxAvailable);
                    flag = false;
                    do {
                        comm1 = rng.nextInt(CAHM.nrofComm[no]);
                        if (cmnty[no].availableSpace.get(comm1) >= RV) {
                            break;
                        }
                    } while (true);
                    do {
                        comm2 = rng.nextInt(CAHM.nrofComm[no]);
                        if (comm1 != comm2 && cmnty[no].availableSpace.get(comm2) >= RV) {
                            break;
                        }
                    } while (true);
                    overlap tempOverlap = new overlap();
                    availableFlag = false;
                    for (int j = 0; j < overlapList[no].size(); j++) {
                        tempOverlap = overlapList[no].get(j);
                        if ((tempOverlap.x == comm1 && tempOverlap.y == comm2) || (tempOverlap.x == comm2 && tempOverlap.y == comm1)) {
                            flag = true;
                            tempArray[0] = 0;
                            Arrays.sort(tempArray, Collections.reverseOrder());
                            maxAvailable = tempArray[1];
                            if (maxAvailable == 0) {
                                availableFlag = true;
                                flag = false;
                            }
                            break;
                        }
                    }
                } while (flag);
                if (availableFlag) {
                    break;
                }
                oLap = new overlap();
                oLap.x = comm1;
                oLap.y = comm2;
                for (int j = 0; j < RV; j++) {
                    int temp = rng.nextInt(M2NodesList.size());
                    cmnty[no].membership[oLap.x].add(M2NodesList.get(temp));
                    cmnty[no].availableSpace.set(oLap.x, cmnty[no].availableSpace.get(oLap.x) - 1);
                    cmnty[no].membershipCount.set(oLap.x, cmnty[no].membershipCount.get(oLap.x) + 1);
                    cmnty[no].membership[oLap.y].add(M2NodesList.get(temp));
                    cmnty[no].availableSpace.set(oLap.y, cmnty[no].availableSpace.get(oLap.y) - 1);
                    cmnty[no].membershipCount.set(oLap.y, cmnty[no].membershipCount.get(oLap.y) + 1);
                    cmnty[no].nodeInCommunities[M2NodesList.get(temp)].add(oLap.x);
                    cmnty[no].nodeInCommunities[M2NodesList.get(temp)].add(oLap.y);
                    oLap.overlapMembership.add(M2NodesList.get(temp));
                    overlapList[no].add(oLap);
                    M2NodesList.remove(temp);
                }
            }
            if (availableFlag) {
                float total = 0;
                for (overlap item : overlapList[no]) {
                    total += item.overlapMembership.size();
                }
                Random oLapRand = new Random();
                while (!M2NodesList.isEmpty()) {
                    int nextRand = oLapRand.nextInt((int) total);
                    int l;
                    int tmpTotal = 0;
                    for (l = 0; l < overlapList[no].size(); l++) {
                        tmpTotal += overlapList[no].get(l).overlapMembership.size();
                        if (tmpTotal > nextRand) {
                            break;
                        }
                    }
                    oLap = overlapList[no].get(l);
                    int temp = rng.nextInt(M2NodesList.size());
                    cmnty[no].membership[oLap.x].add(M2NodesList.get(temp));
                    cmnty[no].availableSpace.set(oLap.x, cmnty[no].availableSpace.get(oLap.x) - 1);
                    cmnty[no].membershipCount.set(oLap.x, cmnty[no].membershipCount.get(oLap.x) + 1);
                    cmnty[no].membership[oLap.y].add(M2NodesList.get(temp));
                    cmnty[no].availableSpace.set(oLap.y, cmnty[no].availableSpace.get(oLap.y) - 1);
                    cmnty[no].membershipCount.set(oLap.y, cmnty[no].membershipCount.get(oLap.y) + 1);
                    cmnty[no].nodeInCommunities[M2NodesList.get(temp)].add(oLap.x);
                    cmnty[no].nodeInCommunities[M2NodesList.get(temp)].add(oLap.y);
                    oLap.overlapMembership.add(M2NodesList.get(temp));
                    overlapList[no].set(l, oLap);
                    M2NodesList.remove(temp);
                }
            }
        }

        int temp;
        for (i = 0; i < nrofHosts; i++) {
            if (cmnty[no].MNo[i] == 1) {
                do {
                    temp = rng.nextInt(nrofComm[no]);
                    if (cmnty[no].availableSpace.get(temp) > 0) {
                        cmnty[no].membership[temp].add(i);
                        cmnty[no].availableSpace.set(temp, cmnty[no].availableSpace.get(temp) - 1);
                        cmnty[no].membershipCount.set(temp, cmnty[no].membershipCount.get(temp) + 1);
                        cmnty[no].nodeInCommunities[i].add(temp);
                        break;
                    }
                } while (true);
            }
        }

        /*---- Generating Local Degrees ----*/
        for (i = 0; i < nrofHosts; i++) {
            for (int j = 0; j < cmnty[no].nodeInCommunities[i].size(); j++) {
                powerLawRNG PLRngLocal = new powerLawRNG(rng, Local, cmnty[no].commSize.get(cmnty[no].nodeInCommunities[i].get(j)) - k_clique + 2);
                cmnty[no].localDegree[i].add((int) Math.floor(PLRngLocal.getDouble() + 0.5) + k_clique - 2);
            }
        }
    }

    public void geographicMapping(int no) {
        int noofXCells = (int) (this.getMaxX() / cellSize);
        int noofYCells = (int) (this.getMaxX() / cellSize);
        boolean[][] available = new boolean[noofXCells][noofYCells];
        for (int i = 0; i < noofXCells; i++) {
            for (int j = 0; j < noofYCells; j++) {
                available[i][j] = true;
            }
        }
        ArrayList<coordinate> exploreQueue = new ArrayList();
        ArrayList<coordinate> tempZone = new ArrayList();
        int index, count;
        coordinate tempCoord;
        coordinate seed;

        cmnty[no].commCells = new ArrayList[nrofComm[no]];
        cmnty[no].coord = new ArrayList[nrofHosts];
        cmnty[no].community = new ArrayList[nrofHosts];
        for (int i = 0; i < nrofHosts; i++) {
            cmnty[no].coord[i] = new ArrayList<coordinate>();
            cmnty[no].community[i] = new ArrayList<Integer>();
        }

        for (int i = 0; i < nrofComm[no]; i++) {
            cmnty[no].commCells[i] = new ArrayList<coordinate>();
        }

        for (int i = 0; i < nrofComm[no]; i++) {

            exploreQueue.clear();
            tempZone.clear();
            count = 0;
            seed = new coordinate();
            do {
                seed.x = rng.nextInt(noofXCells);
                seed.y = rng.nextInt(noofYCells);
            } while (!available[seed.x][seed.y]);
            exploreQueue.add(seed);
            double noofCells;
            noofCells = cmnty[no].commSize.get(i);
            do {
                index = rng.nextInt(exploreQueue.size());
                seed = new coordinate();
                seed = exploreQueue.get(index);
                exploreQueue.remove(index);
                tempZone.add(seed);
                count++;
                for (int l = -1; l < 2; l += 2) {
                    if ((seed.x + l) > 0 && (seed.x + l) < noofXCells && available[seed.x + l][seed.y]) {
                        tempCoord = new coordinate();
                        tempCoord.x = seed.x + l;
                        tempCoord.y = seed.y;
                        if (!tempZone.contains(tempCoord) && !exploreQueue.contains(tempCoord)) {
                            exploreQueue.add(tempCoord);
                        }
                    }
                    if ((seed.y + l) > 0 && (seed.y + l) < noofYCells && available[seed.x][seed.y + l]) {
                        tempCoord = new coordinate();
                        tempCoord.x = seed.x;
                        tempCoord.y = seed.y + l;
                        if (!tempZone.contains(tempCoord) && !exploreQueue.contains(tempCoord)) {
                            exploreQueue.add(tempCoord);
                        }
                    }
                }
            } while (count < noofCells && !exploreQueue.isEmpty());
            if (count == noofCells) {
                for (int l = 0; l < tempZone.size(); l++) {
                    tempCoord = new coordinate();
                    tempCoord = tempZone.get(l);
                    available[tempCoord.x][tempCoord.y] = false;
                }
                cmnty[no].commCells[i].addAll(tempZone);
            } else {
                i--;
            }
        }
        for (int i = 0; i < nrofHosts; i++) {
            for (int j = 0; j < cmnty[no].localDegree[i].size(); j++) {
                tempZone.clear();
                tempZone.addAll(cmnty[no].commCells[cmnty[no].nodeInCommunities[i].get(j)]);
                for (int k = 0; k < cmnty[no].localDegree[i].get(j); k++) {
                    tempCoord = new coordinate();
                    index = rng.nextInt(tempZone.size());
                    tempCoord = tempZone.get(index);
                    tempZone.remove(index);
                    cmnty[no].coord[i].add(tempCoord);
                    cmnty[no].community[i].add(cmnty[no].nodeInCommunities[i].get(j));
                }
            }

        }
    }

    public void calculateSortDistances() {
        double distance;
        coordinate refCd;
        coordinate otherCd;
        distanceList = new ArrayList[noofPeriods][];
        communityList = new ArrayList[noofPeriods][];
        for (int p = 0; p < noofPeriods; p++) {
            distanceList[p] = new ArrayList[cmnty[p].coord[address].size()];
            communityList[p] = new ArrayList[cmnty[p].coord[address].size()];
            for (int i = 0; i < cmnty[p].coord[address].size(); i++) {
                distanceList[p][i] = new ArrayList<Tuple<Integer, Double>>();
                communityList[p][i] = new ArrayList<Tuple<Integer, Double>>();
                refCd = cmnty[p].coord[address].get(i);
                for (int j = 0; j < cmnty[p].coord[address].size(); j++) {
                    if (j != i) {
                        otherCd = cmnty[p].coord[address].get(j);
                        distance = Math.sqrt(Math.pow(refCd.x - otherCd.x, 2) + Math.pow(refCd.y - otherCd.y, 2));
                        if (distance < minDistance[p]) {
                            minDistance[p] = distance;
                        }
                        distanceList[p][i].add(new Tuple<Integer, Double>(j, distance));
                        communityList[p][i].add(new Tuple<Integer, Double>(cmnty[p].community[address].get(j), distance));
                    }
                }

                Collections.sort(distanceList[p][i], new TupleComparator());
                Collections.reverse(distanceList[p][i]);

                Collections.sort(communityList[p][i], new TupleComparator());
                Collections.reverse(communityList[p][i]);

            }
        }
    }

    private class TupleComparator implements Comparator<Tuple<Integer, Double>> {

        public int compare(Tuple<Integer, Double> tuple1,
                Tuple<Integer, Double> tuple2) {
            if (tuple2.getValue() - tuple1.getValue() < 0) {
                return -1;
            } else if (tuple2.getValue() - tuple1.getValue() > 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
