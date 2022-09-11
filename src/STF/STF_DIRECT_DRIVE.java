/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STF;

import ATST.AT_ST_DIRECTDRIVE;
import Environment.Environment;
import agents.LARVAFirstAgent;
import ai.Choice;
import ai.DecisionSet;
import console.Console;
import geometry.Compass;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import tools.emojis;
import world.Perceptor;

// This class extends the AT_ST_DIRECT_DRIVE to oen a new branch for flying vehicles
public class STF_DIRECT_DRIVE extends AT_ST_DIRECTDRIVE {

    @Override
    public void setup() {
        // Setup of AT_ST_DIURECT_DRIVE is still valid
        super.setup();

        // But we refactor the decision set to take into account the new
        //flying capabilities: UP and DOWN
        A = new DecisionSet();
        A.
                addChoice(new Choice("DOWN")).
                addChoice(new Choice("UP")).
                addChoice(new Choice("MOVE")).
                addChoice(new Choice("LEFT")).
                addChoice(new Choice("RIGHT"));

    }

    // Refactor myJoinSession just to register in the DF as a flying agent (STF)
    @Override
    public Status MyJoinSession() {

        this.DFAddMyServices(new String[]{"TYPE STF"});
        outbox = session.createReply();
        outbox.setContent("Request join session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().startsWith("Confirm")) {
            Error("Could not join session " + sessionKey + " due to " + session.getContent());
            return Status.CLOSEPROBLEM;
        }
        this.MyReadPerceptions();
        //Info(this.easyPrintPerceptions());
        return Status.SOLVEPROBLEM;
    }

    /*
    Refactor the utility function to split new branches for take off and landing
    */
    @Override
    protected double U(Environment E, Choice a) {
        /*
        Take off might be interpreted in many different ways. 
        Take off to the max flying level? Take off just a little above the ground, take off just the minimum level above the ground?
        */ 
        if (E.getDistance() > 0
                && E.getGPS().getZ() < E.getMaxlevel()) { // Take off to the max flying level
//                && E.getGPS().getZ() < Math.min(E.getVisualFront() + 15, E.getMaxlevel())) { // Take off just a little (15 units) above the ground
            return goTakeOff(E, a);
        } else if (E.getDistance() == 0 && E.getGround() > 0) {
            return goLanding(E, a);
        } else {
            return goAhead(E, a);
        }
    }

    // Move up to the flying level
    public double goTakeOff(Environment E, Choice a) {
        if (a.getName().equals("UP")) {
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    // Go down towards the ground level
    public double goLanding(Environment E, Choice a) {
        if (a.getName().equals("DOWN")) {
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    protected double goAhead(Environment E, Choice a) {
        if (a.getName().equals("MOVE")) {
            
            return U(S(E, a));
        } else if (a.getName().equals("LEFT") || a.getName().equals("RIGHT")) {
            
            return U(S(E, a), new Choice("MOVE"));
        }
        return Choice.MAX_UTILITY;

    }

}
