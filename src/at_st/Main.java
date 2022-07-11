package at_st;

import appboot.LARVABoot;
import casosprácticos.AT_ST_BASIC_AVOID;
import casosprácticos.AT_ST_BASIC_SURROUND;
import casosprácticos.AT_ST_DELIBERATIVE;
import casosprácticos.AT_ST_DIRECTDRIVE;
import casosprácticos.AT_ST_FULL;
import casosprácticos.STF_BASIC_AVOID;
import casosprácticos.STF_BASIC_SURROUND;
import casosprácticos.STF_DELIBERATIVE;
import casosprácticos.STF_DIRECT_DRIVE;
import casosprácticos.STF_FULL;

public class Main {

    public static void main(String[] args) {
        LARVABoot boot = new LARVABoot();
        boot.Boot("150.214.190.126", 1099);
        boot.loadAgent("ATST", AT_ST.class);
        boot.loadAgent("ATST-DD", AT_ST_DIRECTDRIVE.class);
        boot.loadAgent("ATST-BA", AT_ST_BASIC_AVOID.class);
        boot.loadAgent("ATST-BS", AT_ST_BASIC_SURROUND.class);
        boot.loadAgent("ATST-FULL", AT_ST_FULL.class);
        boot.loadAgent("ATST-PLAN", AT_ST_DELIBERATIVE.class);
        boot.loadAgent("STF-DD", STF_DIRECT_DRIVE.class);
        boot.loadAgent("STF-BA", STF_BASIC_AVOID.class );
        boot.loadAgent("STF-BS", STF_BASIC_SURROUND.class);
        boot.loadAgent("STF-FULL", STF_FULL.class);
        boot.loadAgent("STF-PLAN", STF_DELIBERATIVE.class);
        boot.WaitToShutDown();
    }
    
}
