/* 

 */
package report;

import java.util.List;

import core.DTNHost;
import core.SimError;
import core.SimScenario;
import core.UpdateListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import movement.CAHM;
import movement.MovementModel;

/**
 * @author Zunnun Narmawala
 *
 */
public class CommunityStructureReport extends Report
        implements UpdateListener {

    public CommunityStructureReport() {
        super();
        init();

    }

    @Override
    protected void init() {
        super.init();
    }

    public void updated(List<DTNHost> hosts) {

    }

    @Override
    public void done() {
        write("Period\tCommunity\tMemberNode");
        for (int k = 0; k < CAHM.noofPeriods; k++) {
            for (int i = 0; i < CAHM.nrofComm[k]; i++) {
                for (int j = 0; j < CAHM.cmnty[k].membership[i].size(); j++) {
                    write(k + "\t" + i + "\t" + CAHM.cmnty[k].membership[i].get(j));
                }
            }
        }
        super.done();
    }
}
