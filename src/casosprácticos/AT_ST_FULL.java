package casosprácticos;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import Environment.Environment;
import ai.Choice;
import ai.DecisionSet;

public class AT_ST_FULL extends AT_ST_BASIC_SURROUND {


    @Override
    public void setup() {
        super.setup();
        A = new DecisionSet();
        A.
                addChoice(new Choice("RECHARGE")).
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
                
                return Choice.ANY_VALUE;
            }
        } else if (E.isTargetRight()) {
            if (a.getName().equals("RIGHT")) {
                nextWhichwall = "LEFT";
                nextdistance = E.getDistance();
                
                return Choice.ANY_VALUE;
            }
        }
        return Choice.MAX_UTILITY;

    }

    public double goTurnOnWallRight(Environment E, Choice a) {
        if (a.getName().equals("RIGHT")) {
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;

    }

    public double goStopWallRight(Environment E, Choice a) {
        if (a.getName().equals("LEFT")) {
            nextWhichwall = "NONE";
            distance = Integer.MAX_VALUE;
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    public double goRevolveWallRight(Environment E, Choice a) {
        if (a.getName().equals("LEFT")) {
            
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
                
                return Choice.ANY_VALUE;
            }
        } else if (E.isTargetRight()) {
            if (a.getName().equals("RIGHT")) {
                
                return Choice.ANY_VALUE;
            }
        }
        return Choice.MAX_UTILITY;
    }

    protected double goLowEnergy(Environment E, Choice a) {
        if (a.getName().equals("RECHARGE")) {
            
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    @Override
    protected double U(Environment E, Choice a) {
        if (E.getEnergy() * 100 / E.getAutonomy() < 10) {
            return goLowEnergy(E, a);
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

//        if (a.getName().equals("RECHARGE")) {
//            if (E.getEnergy() * 100 / E.getAutonomy() < 10) {
//                return -Choice.ANY_VALUE;
//            } else {
//                return Choice.MAX_UTILITY;
//            }
//        } 
//
//    protected double UWallRight(Environment E, Choice a) {
//        if (E.isFreeFrontRight()) {
//            if (a.getName().equals("RIGHT")) {
//                return Choice.ANY_VALUE;
//            }
//        } else if (E.isFreeFront()) {
//            if (E.isTargetFrontLeft() && E.isFreeFrontLeft() && E.getDistance() < distance) {
//                if (a.getName().equals("LEFT")) {
//                    nextwall = "NONE";
//                    nextdistance = Integer.MAX_VALUE;
//                    return Choice.ANY_VALUE;
//                }
//            } else {
//                if (a.getName().equals("MOVE")) {
//                    return Choice.ANY_VALUE;
//                }
//            }
//        } else {
//            if (a.getName().equals("LEFT")) {
//                return Choice.ANY_VALUE;
//            }
//        }
//        return Choice.MAX_UTILITY;
//    }
//
//    protected double UWallLeft(Environment E, Choice a) {
//        if (E.isFreeFrontLeft()) {
//            if (a.getName().equals("LEFT")) {
//                return Choice.ANY_VALUE;
//            }
//        } else if (E.isFreeFront()) {
//            if (E.isTargetFrontRight() && E.isFreeFrontRight() && E.getDistance() < distance) {
//                if (a.getName().equals("RIGHT")) {
//                    nextwall = "NONE";
//                    nextdistance = Integer.MAX_VALUE;
//                    return Choice.ANY_VALUE;
//                }
//            } else {
//                if (a.getName().equals("MOVE")) {
//                    return Choice.ANY_VALUE;
//                }
//            }
//        } else {
//            if (a.getName().equals("RIGHT")) {
//                return Choice.ANY_VALUE;
//            }
//        }
//        return Choice.MAX_UTILITY;
//    }
//
//    protected double UFollowWall(Environment E, Choice a) {
//        if (wall.equals("LEFT")) {
//            return UWallLeft(E, a);
//        } else if (wall.equals("RIGHT")) {
//            return UWallRight(E, a);
//        }
//        return Choice.MAX_UTILITY;
//    }
