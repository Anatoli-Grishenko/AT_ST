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

public class STF_BASIC_AVOID extends STF_DIRECT_DRIVE {

    @Override
    protected double U(Environment E, Choice a) {
        if (E.getDistance() > 0
                && E.getGPS().getZ() < E.getMaxlevel()) {
            if (a.getName().equals("UP")) {
                return Choice.ANY_VALUE;
            }
        } else if (E.getDistance() == 0 && E.getGround() > 0) {
            if (a.getName().equals("DOWN")) {
                return Choice.ANY_VALUE;
            }
        } else {
            if (E.isFreeFront()) {
                if (a.getName().equals("MOVE")) {
                    return U(S(E, a));
                } else if (a.getName().equals("LEFT") || a.getName().equals("RIGHT")) {
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
