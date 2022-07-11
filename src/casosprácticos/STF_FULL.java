/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

import Environment.Environment;
import ai.Choice;
import ai.DecisionSet;

public class STF_FULL extends STF_BASIC_SURROUND {

    String wall, nextwall;

    @Override
    public void setup() {
        super.setup();
        this.setFrameDelay(10);
        this.logger.offEcho();
        A = new DecisionSet();
        A.
                addChoice(new Choice("RECHARGE")).
                addChoice(new Choice("UP")).
                addChoice(new Choice("DOWN")).
                addChoice(new Choice("MOVE")).
                addChoice(new Choice("LEFT")).
                addChoice(new Choice("RIGHT"));
    }


    @Override
    public double goAvoid(Environment E, Choice a) {
        if (E.isTargetLeft()) {
            if (a.getName().equals("LEFT")) {
                nextWhichwall = "RIGHT";
                nextdistance = E.getDistance();
            a.setAnnotation(this.myMethod());
                
                return Choice.ANY_VALUE;
            }
        } else if (E.isTargetRight()) {
            if (a.getName().equals("RIGHT")) {
                nextWhichwall = "LEFT";
                nextdistance = E.getDistance();
            a.setAnnotation(this.myMethod());
                
                return Choice.ANY_VALUE;
            }
        }
        return Choice.MAX_UTILITY;

    }

    public double goTurnOnWallRight(Environment E, Choice a) {
        if (a.getName().equals("RIGHT")) {
            a.setAnnotation(this.myMethod());
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;

    }

    public double goStopWallRight(Environment E, Choice a) {
        if (a.getName().equals("LEFT")) {
            nextWhichwall = "NONE";
            distance = Integer.MAX_VALUE;
            a.setAnnotation(this.myMethod());
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    public double goRevolveWallRight(Environment E, Choice a) {
        if (a.getName().equals("LEFT")) {
            a.setAnnotation(this.myMethod());
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    public double goFollowWallRight(Environment E, Choice a) {
        if (E.isTargetLeft()
                && E.isFreeFrontLeft()
                && E.getDistance() < distance) {
            return goStopWallRight(E, a);
        } else if (E.isFreeFrontRight()) {
            return goTurnOnWallRight(E, a);
        } else if (E.isFreeFront()) {
            return goKeepOnWall(E, a);
        } else {
            return goRevolveWallRight(E, a);
        }
    }

    public double goFollowWall(Environment E, Choice a) {
        if (whichWall.equals("LEFT")) {
            return goFollowWallLeft(E, a);
        } else if (whichWall.equals("RIGHT")) {
            return goFollowWallRight(E, a);
        }
        return Choice.MAX_UTILITY;
    }

    protected double goTurnBack(Environment E, Choice a) {
        if (E.isTargetLeft()) {
            if (a.getName().equals("LEFT")) {
            a.setAnnotation(this.myMethod());
                
                return Choice.ANY_VALUE;
            }
        } else if (E.isTargetRight()) {
            if (a.getName().equals("RIGHT")) {
            a.setAnnotation(this.myMethod());
                
                return Choice.ANY_VALUE;
            }
        }
        return Choice.MAX_UTILITY;
    }

    protected double goLowEnergy(Environment E, Choice a) {
        if (E.getGround() > 0) {
            return goLanding(E, a);
        } else if (a.getName().equals("RECHARGE")) {
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    @Override
    protected double U(Environment E, Choice a) {
        if (E.getEnergy() * 100 / E.getAutonomy() < 10) {
            return goLowEnergy(E, a);
        }
        if (E.getDistance() > 0
                && E.getGPS().getZ() < E.getMaxlevel()) {
//                && E.getGPS().getZ() < Math.min(E.getVisualFront() + 15, E.getMaxlevel())) {
            return goTakeOff(E, a);
        } else if (E.getDistance() == 0 && E.getGround() > 0) {
            return goLanding(E, a);
        } else if (!whichWall.equals("NONE")) {
            return goFollowWall(E, a);
        } else if (E.isTargetBack()) {
            return goTurnBack(E, a);
        } else if (!E.isFreeFront()) {
            return goAvoid(E, a);
        } else {
            return goAhead(E, a);
        }
    }
}
