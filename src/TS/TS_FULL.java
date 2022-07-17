/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TS;

import Environment.Environment;
import STF.STF_FULL;
import ai.Choice;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import master.MASTER_DRIVE_AIRBORNE;

/**
 *
 * @author Anatoli Grishenko <Anatoli.Grishenko@gmail.com>
 */
public class TS_FULL extends MASTER_DRIVE_AIRBORNE {

    @Override
    public void setup() {
        super.setup();
        myType = "TS";
        useAlias = true;
        logger.offEcho();
        this.setFrameDelay(10);
//        this.closeRemote();
        this.openRemote();
    }

    @Override
    public Status MySolveProblem() {
        // Analizar objetivo
        Info(this.easyPrintPerceptions());
        if (G(E)) {
            Info("Target reached");
           // Message("Starting negotiation");
            this.requestDroids(taskName.split(" ")[1], "bydistance");
            if (isOverCurrentMission()) {
                Info("The problem is over");
               Message("The problem " + problem + " has been solved");
                return Status.CLOSEPROBLEM;
            } else {
                Info("Moving on to the next task");
                nextWhichwall = whichWall = "NONE";
                nextdistance = distance = Choice.MAX_UTILITY;
                return translateStatus(activateTask());
            }
        }
        Choice a = Ag(E, A);
        if (a == null) {
            Alert("Found no action to execute");
            return Status.CLOSEPROBLEM;
        } else {// Execute
            Info("Excuting " + a);
//            System.out.println("Alternatives " + A.toString());
//            System.out.println("Excuting " + a);
            this.MyExecuteAction(a.getName());
            this.MyReadPerceptions();
            Info(this.easyPrintPerceptions());
            if (!Ve(E)) {
                this.Error("The agent is not alive: " + E.getStatus());
                return Status.CLOSEPROBLEM;
            }
            return Status.SOLVEPROBLEM;
        }
    }

    ////// DEPRECATABLE
//    @Override
//    public Status MyJoinSession() {
//        this.DFAddMyServices(new String[]{"TYPE " + myType});
//        nextWhichwall = whichWall = "NONE";
//        nextdistance = distance = Choice.MAX_UTILITY;
//        this.doQueryCities();
//        outbox = session.createReply();
//        outbox.setContent("Request join session " + sessionKey + " in "
//                + "GuildHouse");
////                + E.getCityList()[(int) (Math.random() * E.getCityList().length)]);
//        this.LARVAsend(outbox);
//        session = this.LARVAblockingReceive();
//        if (!session.getContent().startsWith("Confirm")) {
//            Error("Could not join session " + sessionKey + " due to " + session.getContent());
//            return Status.CLOSEPROBLEM;
//        }
//        this.DFAddMyServices(new String[]{"SESSION " + sessionKey});
//        this.MyReadPerceptions();
//        Info(this.easyPrintPerceptions());
//        String m = chooseMission();
//        if (!activateMission(m)) {
//            return Status.CLOSEPROBLEM;
//        } else {
//            return Status.SOLVEPROBLEM;
//        }
//    }

//    @Override
//    protected String chooseMission() {
//        Info("Querying the missions");
//        outbox = session.createReply();
//        outbox.setContent("Query missions session " + sessionKey);
//        this.LARVAsend(outbox);
//        session = LARVAblockingReceive();
//        this.decodeMissions(session.getContent());
//        String m;
////        if (this.getNumMissions() == 1) {
////            m = this.getAllMissions()[0];
////        } else {
////            m = this.inputSelect("Please chhoose a mission", getAllMissions(), "");
////        }
////        return m;
//        return "MISSION SPINE-OUTERRIM-SPINE";
//    }
    
    @Override
    public Status MyOpenProblem() {
        if (useAlias) {
            if (this.DFGetAllProvidersOf(service).isEmpty()) {
                Error("Service PMANAGER is down");
                return Status.CHECKOUT;
            }
            problemManager = this.DFGetAllProvidersOf(service).get(0);
            Info("Found problem manager " + problemManager);
            problem = "SandboxTatooine-A";
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
