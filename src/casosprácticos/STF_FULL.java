/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

import Environment.Environment;
import ai.Choice;
import ai.DecisionSet;

public class STF_FULL extends STF_DIRECT_DRIVE {
    
    String wall, nextwall;
    double distance, nextdistance;
    
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
        problems = new String[]{
            "SandboxTesting",
            "SandboxFlatN",
            "SandboxFlat-1-1",
            "SandboxFlatS",
            "SandboxBumpy0",
            "SandboxBumpy1",
            "SandboxBumpy2",
            "SandboxBumpy3",
            "SandboxBumpy4",
            "SandboxBumpy4UPS",
            "SandboxHalfmoon1",
            "SandboxHalfmoon1Inv",
            "SandboxHalfmoon3",
            "SandboxTatooine-A",
            "SandboxTatooine-B",
            "SandboxEndor"
        };
    }
    
    @Override
    public Status MyJoinSession() {
        nextwall = wall = "NONE";
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
    
    protected double UAvoid(Environment E, Choice a) {
        if (E.isTargetLeft()) {
            if (a.getName().equals("LEFT")) {
                nextwall = "RIGHT";
                nextdistance = E.getDistance();
                return Choice.ANY_VALUE;
            }
        } else if (E.isTargetRight()) {
            if (a.getName().equals("RIGHT")) {
                nextwall = "LEFT";
                nextdistance = E.getDistance();
                return Choice.ANY_VALUE;
            }
        }
        return Choice.MAX_UTILITY;
        
    }
    
    protected double UTargetAhead(Environment E, Choice a) {
        if (E.getDistance() > 0
                && E.getGPS().getZ() < E.getMaxlevel()) {
            if (a.getName().equals("UP")) {
                return Choice.ANY_VALUE;
            }
        } else if (E.getDistance() == 0 && E.getGround() > 0) {
            if (a.getName().equals("DOWN")) {
                return Choice.ANY_VALUE;
            }
        } else if (a.getName().equals("MOVE")) {
            return U(S(E, a));
        } else if (a.getName().equals("LEFT") || a.getName().equals("RIGHT")) {
            return U(S(E, a), new Choice("MOVE"));
        }
        return Choice.MAX_UTILITY;
    }
    
    protected double UTargetBack(Environment E, Choice a) {
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
    
    protected double UWallRight(Environment E, Choice a) {
        if (E.isFreeFrontRight()) {
            if (a.getName().equals("RIGHT")) {
                return Choice.ANY_VALUE;
            }
        } else if (E.isFreeFront()) {
            if (E.isTargetFrontLeft() && E.isFreeFrontLeft() && E.getDistance() < distance) {
                if (a.getName().equals("LEFT")) {
                    nextwall = "NONE";
                    nextdistance = Integer.MAX_VALUE;
                    return Choice.ANY_VALUE;
                }
            } else {
                if (a.getName().equals("MOVE")) {
                    return Choice.ANY_VALUE;
                }
            }
        } else {
            if (a.getName().equals("LEFT")) {
                return Choice.ANY_VALUE;
            }
        }
        return Choice.MAX_UTILITY;
    }
    
    protected double UWallLeft(Environment E, Choice a) {
        if (E.isFreeFrontLeft()) {
            if (a.getName().equals("LEFT")) {
                return Choice.ANY_VALUE;
            }
        } else if (E.isFreeFront()) {
            if (E.isTargetFrontRight() && E.isFreeFrontRight() && E.getDistance() < distance) {
                if (a.getName().equals("RIGHT")) {
                    nextwall = "NONE";
                    nextdistance = Integer.MAX_VALUE;
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
        return Choice.MAX_UTILITY;
    }
    
    @Override
    protected double U(Environment E, Choice a) {
        if (E.getEnergy() * 100 / E.getAutonomy() < 10) {
            if (E.getGround() > 0) {
                if (a.getName().equals("DOWN")) {
                    return -Choice.ANY_VALUE;
                }
            } else if (E.getGround() == 0) {
                if (a.getName().equals("RECHARGE")) {
                    return -Choice.ANY_VALUE;
                }
            }
        } else if (!wall.equals("NONE")) {
            if (wall.equals("LEFT")) {
                return UWallLeft(E, a);
            } else if (wall.equals("RIGHT")) {
                return UWallRight(E, a);
            }
        } else if (E.isTargetBack()) {
            return UTargetBack(E, a);
        } else if (E.isFreeFront()) {
            return UTargetAhead(E, a);
        } else {
            return UAvoid(E, a);
        }
        return Choice.MAX_UTILITY;
    }
    
    @Override
    public String easyPrintPerceptions() {
        return super.easyPrintPerceptions()
                + "\nWall:\n" + wall + "\n";
    }
    
}
