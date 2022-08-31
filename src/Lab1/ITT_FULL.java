package Lab1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

public class ITT_FULL extends MASTER_DRIVE_GROUND {

    @Override
    public void setup() {
        super.setup();
        myType = "ITT";
        useAlias = true;
        logger.offEcho();
        this.setFrameDelay(1);
//        this.closeRemote();
        this.openRemote();
        this.recruitByCFP=false;
        this.recruitByREQUEST=true;
        this.showPerceptions=false;
    }
}
