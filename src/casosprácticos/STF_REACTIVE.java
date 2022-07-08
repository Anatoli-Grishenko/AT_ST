/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

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

public class STF_REACTIVE extends AT_ST_REACTIVE {

    @Override
    public void setup() {
        super.setup();
        A = new DecisionSet();
        A.
                addChoice(new Choice("DOWN")).
                addChoice(new Choice("UP")).
                addChoice(new Choice("MOVE")).
                addChoice(new Choice("LEFT")).
                addChoice(new Choice("RIGHT"));

    }

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
        this.openRemote();
        this.setFrameDelay(100);
        Info(this.easyPrintPerceptions());
        return Status.SOLVEPROBLEM;
    }
    
    @Override
    protected double U(Environment E, Choice a) {
        if (E.getDistance() > 0
                && E.getGPS().getZ() < E.getMaxlevel()) {
            if (a.getName().equals("UP")) {
                return Choice.ANY_VALUE;
            }
        }
        if (E.getDistance() == 0 && E.getGround() > 0) {
            if (a.getName().equals("DOWN")) {
                return Choice.ANY_VALUE;
            }
        } else {
            if (E.isFreeFront()) {
                if (a.getName().equals("MOVE")) {
                    return U(S(E, a));
                } else if (a.getName().equals("LEFT") || 
                        a.getName().equals("RIGHT")) {
                    return U(S(E, a), new Choice("MOVE"));
                }
            } else {
                if (a.getName().equals("RIGHT")) {
                    return Choice.ANY_VALUE;
                }

            }
        }
        return Choice.MAX_UTILITY;
    }

}
