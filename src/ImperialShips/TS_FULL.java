/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ImperialShips;

import ai.Choice;
import data.Transform;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class TS_FULL extends MASTER_DRIVE_AIRBORNE {

    @Override
    public void setup() {
        super.setup();
        myType = "TS";
        useAlias = true;
        logger.onEcho();
        this.setFrameDelay(10);
//        this.closeRemote();
        this.openRemote();
        this.recruitByCFP=false;
        this.recruitByREQUEST=true;
//        sessionAlias="CHOCOLATE";
    }

}
