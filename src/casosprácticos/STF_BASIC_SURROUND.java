/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

import Environment.Environment;
import ai.Choice;
import ai.DecisionSet;

public class STF_BASIC_SURROUND extends STF_DIRECT_DRIVE {

    boolean wall, nextwall;
    double distance, nextdistance;

    @Override
    public Status MyJoinSession() {
        nextwall = wall = false;
        nextdistance = distance = Choice.MAX_UTILITY;
        return super.MyJoinSession();
    }

    @Override
    protected Choice Ag(Environment E, DecisionSet A) {
        if (G(E)) {
            return null;
        } else if (A.isEmpty()) {
            return null;
        } else {
            A = Prioritize(E, A);
            wall = nextwall;
            distance = nextdistance;
            return A.BestChoice();
        }
    }

    @Override
    protected double U(Environment E, Choice a) {
        if (wall) {
            if (E.isFreeFrontLeft()) {
                if (a.getName().equals("LEFT")) {
                    return Choice.ANY_VALUE;
                }
            } else if (E.isFreeFront()) {
                if (E.isTargetFrontRight() && 
                        E.isFreeFrontRight() && 
                        E.getDistance() < distance) {
                    if (a.getName().equals("RIGHT")) {
                        nextwall = false;
                        distance = Integer.MAX_VALUE;
                        return Choice.ANY_VALUE;
                    }
                } else {
                    if (a.getName().equals("MOVE")) {
                        return Choice.ANY_VALUE;
                    }
                }
            } else {
                if (a.getName().equals("RIGHT")) {
                    return Choice.ANY_VALUE;
                }
            }
        } else if (E.getDistance() > 0
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
                    wall = true;
                    distance = E.getDistance();
                    return Choice.ANY_VALUE;
                }

            }
        }
        return Choice.MAX_UTILITY;
    }

    @Override
    public String easyPrintPerceptions() {
        return super.easyPrintPerceptions()
                + "\nWall:\n" + wall + "\n";
    }

}
