package Main;

import ATST.AT_ST;
import ATST.AT_ST_BASIC_AVOID;
import ATST.AT_ST_BASIC_SURROUND;
import ATST.AT_ST_DELIBERATIVE;
import ATST.AT_ST_DIRECTDRIVE;
import STF.STF_BASIC_AVOID;
import STF.STF_BASIC_SURROUND;
import STF.STF_DELIBERATIVE;
import STF.STF_DIRECT_DRIVE;
import appboot.LARVABoot;
import static crypto.Keygen.getHexaKey;

public class Main {
    public static void main(String[] args) {
        LARVABoot boot = new LARVABoot();
//        boot.Boot("150.214.190.126", 1099);
        boot.Boot("localhost", 1099);
        boot.loadAgent("ATST-"+getHexaKey(4), AT_ST.class);
        boot.loadAgent("ATST-DD-"+getHexaKey(4), AT_ST_DIRECTDRIVE.class);
        boot.loadAgent("ATST-BA-"+getHexaKey(4), AT_ST_BASIC_AVOID.class);
        boot.loadAgent("ATST-BS-"+getHexaKey(4), AT_ST_BASIC_SURROUND.class);
        boot.loadAgent("ATST-PLAN-"+getHexaKey(4), AT_ST_DELIBERATIVE.class);
        boot.loadAgent("STF-DD-"+getHexaKey(4), STF_DIRECT_DRIVE.class);
        boot.loadAgent("STF-BA-"+getHexaKey(4), STF_BASIC_AVOID.class );
        boot.loadAgent("STF-BS-"+getHexaKey(4), STF_BASIC_SURROUND.class);
        boot.loadAgent("STF-PLAN-"+getHexaKey(4), STF_DELIBERATIVE.class);
        boot.WaitToShutDown();
    }

}
