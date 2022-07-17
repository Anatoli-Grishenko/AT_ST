package ATST;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import Environment.Environment;
import ai.Choice;
import ai.DecisionSet;

public class AT_ST_BASIC_SURROUND extends AT_ST_BASIC_AVOID {

    protected String whichWall, nextWhichwall;
    protected double distance, nextdistance;

    @Override
    public Status MyJoinSession() {
        nextWhichwall = whichWall = "NONE";
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
            whichWall = nextWhichwall;
            distance = nextdistance;
            return A.BestChoice();
        }
    }

    @Override
    public double goAvoid(Environment E, Choice a) {
        if (a.getName().equals("RIGHT")) {
            nextWhichwall = "LEFT";
            nextdistance = E.getDistance();
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    public double goKeepOnWall(Environment E, Choice a) {
        if (a.getName().equals("MOVE")) {
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    public double goTurnOnWallLeft(Environment E, Choice a) {
        if (a.getName().equals("LEFT")) {
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;

    }

    public double goRevolveWallLeft(Environment E, Choice a) {
        if (a.getName().equals("RIGHT")) {
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    public double goStopWallLeft(Environment E, Choice a) {
        if (a.getName().equals("RIGHT")) {
            nextWhichwall = "NONE";
            distance = Integer.MAX_VALUE;
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    public double goFollowWallLeft(Environment E, Choice a) {
        if (E.isFreeFrontLeft()) {
            return goTurnOnWallLeft(E, a);
        } else if (E.isTargetFrontRight()
                && E.isFreeFrontRight()
                && E.getDistance() < distance) {
            return goStopWallLeft(E, a);
        } else if (E.isFreeFront()) {
            return goKeepOnWall(E, a);
        } else {
            return goRevolveWallLeft(E, a);
        }
    }

    @Override
    protected double U(Environment E, Choice a) {
        if (whichWall.equals("LEFT")) {
            return goFollowWallLeft(E, a);
        } else if (!E.isFreeFront()) {
            return goAvoid(E, a);
        } else {
            return goAhead(E, a);
        }
    }

    @Override
    public String easyPrintPerceptions() {
        return super.easyPrintPerceptions()
                + "\nWall:\n" + whichWall + "\n";
    }

}
