package at_st;

import appboot.LARVABoot;

public class Main {

    public static void main(String[] args) {
        LARVABoot boot = new LARVABoot();
        boot.Boot("localhost", 1099);
        boot.launchAgent("Anatoli", STF.class);
        boot.WaitToShutDown();
    }
    
}
