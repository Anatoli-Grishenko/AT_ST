/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ImperialShips;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class YT_FULL extends MASTER_DRIVE_AIRBORNE {

    @Override
    public void setup() {
        super.setup();
        myType = "TS";
        useAlias = true;
        logger.offEcho();
        this.setFrameDelay(10);
//        this.closeRemote();
        this.openRemote();
    }

}
