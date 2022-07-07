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

public class AT_ST_REACTIVE_V3 extends AT_ST_REACTIVE {

    boolean wall = false;
    double distance = Integer.MAX_VALUE;

    @Override
    protected double U(Environment E, Choice a) {
        if (wall) {
            if (E.isFreeFrontLeft()) {
                if (a.getName().equals("LEFT")) {
                    return Choice.ANY_VALUE;
                }
            } else if (E.isFreeFront()) {
                if (E.isTargetFrontRight() && E.isFreeFrontRight() && E.getDistance() < distance) {
                    if (a.getName().equals("RIGHT")) {
                        wall = false;
                        distance = Integer.MAX_VALUE;
                        return Choice.ANY_VALUE;
                    }
                } else {
                    if (a.getName().equals("MOVE")) {
                        return Choice.ANY_VALUE;
                    }
                }
            } else {
                if (E.isFreeFrontLeft()) {
                    if (a.getName().equals("LEFT")) {
                        return Choice.ANY_VALUE;
                    }
                } else if (a.getName().equals("RIGHT")) {
                    return Choice.ANY_VALUE;
                }
            }
        } else if (E.isFreeFront()) {
            if (a.getName().equals("MOVE")) {
                return U(S(E, a));
            } else {
                return U(S(E, a), new Choice("MOVE"));
            }
        } else {
            if (a.getName().equals("RIGHT")) {
                wall = true;
                distance = E.getDistance();
                return Choice.ANY_VALUE;
            }

        }
        return Choice.MAX_UTILITY;
    }

}
