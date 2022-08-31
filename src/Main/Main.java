package Main;

import ATST.AT_ST_FULL;
import Lab1.ITT_FULL;
import Lab1.SC_FULL;
import STF.STF_FULL;
import Lab1.TS_FULL;
import Lab1.YT_FULL;
import STF.STF_DELIBERATIVE;
import agents.BB1F;
import agents.DEST;
import agents.VAAT;
import agents.YV;
import appboot.LARVABoot;
import crypto.Keygen;

public class Main {
    public static void main(String[] args) {
        LARVABoot boot = new LARVABoot();
//        boot.Boot("150.214.190.126", 1099);
        boot.Boot("localhost", 1099);
//        boot.loadAgent("ATST", AT_ST.class);
//        boot.loadAgent("ATST-DD", AT_ST_DIRECTDRIVE.class);
//        boot.loadAgent("ATST-BA", AT_ST_BASIC_AVOID.class);
//        boot.loadAgent("ATST-BS", AT_ST_BASIC_SURROUND.class);
//        boot.loadAgent("ATST-FULL", AT_ST_FULL.class);
//        boot.loadAgent("ATST-PLAN", AT_ST_DELIBERATIVE.class);
//        boot.loadAgent("STF-DD", STF_DIRECT_DRIVE.class);
//        boot.loadAgent("STF-BA", STF_BASIC_AVOID.class );
//        boot.loadAgent("STF-BS", STF_BASIC_SURROUND.class);
        boot.loadAgent("STF-FULL", STF_FULL.class);
//        boot.loadAgent("STF-PLAN", STF_DELIBERATIVE.class);
//        boot.loadAgent("ITT-FULL", ITT_FULL.class);
        boot.loadAgent("TS-1", TS_FULL.class);
//        boot.loadAgent("TS-2", TS_FULL.class);
        boot.loadAgent("YT-1", YT_FULL.class);
        boot.loadAgent("SC-1", SC_FULL.class);
        boot.loadAgent("ITT-1", ITT_FULL.class);
//        boot.loadAgent("SC-FULL", SC_FULL.class);
//        boot.loadAgent("DROID-1", VAAT.class);
//        boot.loadAgent("DROID-2", BB1F.class);
//        boot.loadAgent("DROID-3", YV.class);
//        boot.loadAgent(Keygen.getHexaKey(4), DEST.class);
        boot.WaitToShutDown();
    }

}
