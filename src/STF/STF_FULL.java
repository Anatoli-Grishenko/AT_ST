/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STF;

import Environment.Environment;
import ai.Choice;
import ai.DecisionSet;
import static disk.Logger.trimFullString;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class STF_FULL extends STF_BASIC_SURROUND {

    double minimumEnergy = 20; // Percepntage

    @Override
    public void setup() {
        super.setup();
        this.setFrameDelay(10);
        this.logger.offEcho();
//        this.openRemote();
        useAlias = true;
        A = new DecisionSet();
        A.
                addChoice(new Choice("RECHARGE")).
                addChoice(new Choice("UP")).
                addChoice(new Choice("DOWN")).
                addChoice(new Choice("MOVE")).
                addChoice(new Choice("LEFT")).
                addChoice(new Choice("RIGHT"));
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
    public double goAvoid(Environment E, Choice a) {
        double dl = S(S(E, new Choice("LEFT")), new Choice("MOVE")).getDistance(), dr = S(S(E, new Choice("RIGHT")), new Choice("MOVE")).getDistance();
        if (dl < dr || (dl == dr && (E.isTargetLeft() || E.isFreeFrontLeft()))) {
            if (a.getName().equals("LEFT")) {
                nextWhichwall = "RIGHT";
                nextdistance = E.getDistance();
                a.setAnnotation(this.myMethod());

                return Choice.ANY_VALUE;
            }
        } else if (dr < dl || (dl == dr && (E.isTargetRight() || E.isFreeFrontRight()))) {
            if (a.getName().equals("RIGHT")) {
                nextWhichwall = "LEFT";
                nextdistance = E.getDistance();
                a.setAnnotation(this.myMethod());

                return Choice.ANY_VALUE;
            }
        }
        return Choice.MAX_UTILITY;

    }

    @Override
    protected double U(Environment E, Choice a) {
        if (E.getEnergy() * 100 / E.getAutonomy() < minimumEnergy) {
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
        } else if (!E.isFreeFront()
                && (S(E, new Choice("MOVE")).getDistance() < S(S(E, new Choice("LEFT")), new Choice("MOVE")).getDistance()
                && S(E, new Choice("MOVE")).getDistance() < S(S(E, new Choice("RIGHT")), new Choice("MOVE")).getDistance()) //                ||!Ve(S(E, new Choice("MOVE")))
                ) {
            return goAvoid(E, a);
        } else {
            return goAhead(E, a);
        }
    }

    @Override
    public Status MyOpenProblem() {
        if (useAlias) {
            if (this.DFGetAllProvidersOf(service).isEmpty()) {
                Error("Service PMANAGER is down");
                return Status.CHECKOUT;
            }
            problemManager = this.DFGetAllProvidersOf(service).get(0);
            Info("Found problem manager " + problemManager);
            problem = this.inputSelect("PLease select problem to solve", problems, problem);
            if (problem == null) {
                return Status.CHECKOUT;
            }
            this.outbox = new ACLMessage();
            outbox.setSender(getAID());
            outbox.addReceiver(new AID(problemManager, AID.ISLOCALNAME));
            outbox.setContent("Request open " + problem + " alias " + sessionAlias);
            this.LARVAsend(outbox);
            Info("Request opening problem " + problem + " to " + problemManager + " alias " + sessionAlias);
            open = LARVAblockingReceive();
            Info(problemManager + " says: " + open.getContent());
            content = open.getContent();
            contentTokens = content.split(" ");
            if (contentTokens[0].toUpperCase().equals("AGREE")) {
                sessionKey = contentTokens[4];
                session = LARVAblockingReceive();
                sessionManager = session.getSender().getLocalName();
                Info(sessionManager + " says: " + session.getContent());
                return Status.JOINSESSION;
            } else {
                Error(content);
                return Status.CHECKOUT;
            }
        } else {
            return super.MyOpenProblem();
        }
    }

}

//
//    public double goAvoid2(Environment E, Choice a) {
//        double dl = S(S(E, new Choice("LEFT")), new Choice("MOVE")).getDistance(), dr = S(S(E, new Choice("RIGHT")), new Choice("MOVE")).getDistance();
//        if (dl < dr || (dl == dr && (E.isTargetLeft() || E.isFreeFrontLeft()))) {
//            if (a.getName().equals("LEFT")) {
//                nextWhichwall = "RIGHT";
//                nextdistance = E.getDistance();
//                a.setAnnotation(this.myMethod());
//
//                return Choice.ANY_VALUE;
//            }
//        } else if (dr < dl || (dl == dr && (E.isTargetRight() || E.isFreeFrontRight()))) {
//            if (a.getName().equals("RIGHT")) {
//                nextWhichwall = "LEFT";
//                nextdistance = E.getDistance();
//                a.setAnnotation(this.myMethod());
//
//                return Choice.ANY_VALUE;
//            }
//        }
//        return Choice.MAX_UTILITY;
//
//    }
//
//    protected double goAhead2(Environment E, Choice a) {
//        if (a.getName().equals("MOVE")) {
//            if (E.isFreeFront()) {
//                a.setAnnotation(this.myMethod());
//                return U(S(E, a));
//            } else {
//                return goAvoid(E, a);
//            }
//        } else if (a.getName().equals("LEFT")) {
//            a.setAnnotation(this.myMethod());
//            return U(S(E, a), new Choice("MOVE"));
//        } else if (a.getName().equals("RIGHT")) {
//            a.setAnnotation(this.myMethod());
//            return U(S(E, a), new Choice("MOVE"));
//        }
//        return Choice.MAX_UTILITY;
//
//    }
//
//    @Override
//    protected double U(Environment E, Choice a) {
//        if (E.getEnergy() * 100 / E.getAutonomy() < 15) {
//            return goLowEnergy(E, a);
//        }
//        if (E.getDistance() > 0
//                && E.getGPS().getZ() < E.getMaxlevel()) {
////                && E.getGPS().getZ() < Math.min(E.getVisualFront() + 15, E.getMaxlevel())) {
//            return goTakeOff(E, a);
//        } else if (E.getDistance() == 0 && E.getGround() > 0) {
//            return goLanding(E, a);
//        } else if (!whichWall.equals("NONE")) {
//            return goFollowWall(E, a);
//        } else if (E.isTargetBack()) {
//            return goTurnBack(E, a);
//        } //        else if (E.getDistance() > 0 && !E.isFreeFront()) {
//        //            return goAvoid(E, a);
//        //        } 
//        else {
//            return goAhead(E, a);
//        }
//    }
//
