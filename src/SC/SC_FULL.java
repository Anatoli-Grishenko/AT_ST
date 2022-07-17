package SC;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import ATST.AT_ST_FULL;
import ai.Choice;
import master.MASTER_DRIVE_GROUND;

public class SC_FULL extends MASTER_DRIVE_GROUND {

//    @Override
//    public Status MyJoinSession() {
//        nextWhichwall = whichWall = "NONE";
//        nextdistance = distance = Choice.MAX_UTILITY;
//        this.DFAddMyServices(new String[]{"TYPE SC"});
//        outbox = session.createReply();
//        outbox.setContent("Request join session " + sessionKey);
//        this.LARVAsend(outbox);
//        session = this.LARVAblockingReceive();
//        if (!session.getContent().startsWith("Confirm")) {
//            Error("Could not join session " + sessionKey + " due to " + session.getContent());
//            return Status.CLOSEPROBLEM;
//        }
//        this.DFAddMyServices(new String[]{"SESSION " + sessionKey});
//        this.MyReadPerceptions();
//        this.openRemote();
//        this.setFrameDelay(10);
//        Info(this.easyPrintPerceptions());
//        return chooseMission();
//    }

}
