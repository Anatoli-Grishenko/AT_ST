package master;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import Environment.Environment;
import STF.STF_FULL;
import agents.LARVAFirstAgent;
import ai.Choice;
import ai.DecisionSet;
import console.Console;
import data.Transform;
import geometry.Compass;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import tools.TimeHandler;
import tools.emojis;
import world.Perceptor;

/**
 *
 * @author lcv
 */
public class MASTER_DRIVE_AIRBORNE extends STF_FULL {

    protected String myType;

    @Override
    public void setup() {
        super.setup();
    }

    @Override
    public Status MyJoinSession() {
        this.DFAddMyServices(new String[]{"TYPE " + myType});
        nextWhichwall = whichWall = "NONE";
        nextdistance = distance = Choice.MAX_UTILITY;
        this.doQueryCities();
        outbox = session.createReply();
        outbox.setContent("Request join session " + sessionKey + " in "
                + "GuildHouse");
//                + E.getCityList()[(int) (Math.random() * E.getCityList().length)]);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().startsWith("Confirm")) {
            Error("Could not join session " + sessionKey + " due to " + session.getContent());
            return Status.CLOSEPROBLEM;
        }
        this.DFAddMyServices(new String[]{"SESSION " + sessionKey});
        this.MyReadPerceptions();
//        this.openRemote();
        this.setFrameDelay(10);
        Info(this.easyPrintPerceptions());
        String m = chooseMission();
        return translateStatus(activateMission(m));
    }
    ///////////////////////////////////////////////

    @Override
    protected String activateTask() {
        String parameters[];
        if (this.isOverCurrentMission()) {
            return Status.CLOSEPROBLEM.name();
        }
        setTask();
        parameters = taskName.split(" ");
        if (parameters[0].equals("MOVEIN")) {
            outbox = session.createReply();
            outbox.setContent("Request course in " + parameters[1] + " Session " + sessionKey);
            this.LARVAsend(outbox);
            session = this.LARVAblockingReceive();
            if (!session.getContent().toUpperCase().startsWith("FAILURE")) {
                E.setExternalPerceptions(session.getContent());
                Info("Successfully found a rote in " + taskName);
                return Status.SOLVEPROBLEM.name();
            } else {
                Info("Failed to find a rote to " + taskName);
                return Status.CLOSEPROBLEM.name();
            }
        } else if (parameters[0].equals("MOVETO")) {
            outbox = session.createReply();
            outbox.setContent("Request course to " + Integer.parseInt(parameters[1]) + " "
                    + Integer.parseInt(parameters[2]) + " Session " + sessionKey);
            this.LARVAsend(outbox);
            session = this.LARVAblockingReceive();
            if (!session.getContent().toUpperCase().startsWith("FAILURE")) {
                E.setExternalPerceptions(session.getContent());
                Info("Successfully found a rote to " + taskName);
                return Status.SOLVEPROBLEM.name();
            } else {
                Info("Failed to find a rote to " + taskName);
                return Status.CLOSEPROBLEM.name();
            }
        } else {
            return Status.CLOSEPROBLEM.name();
        }
    }

    ///////////////////////////////////////////////
    protected String chooseMission() {
        Info("Querying the missions");
        outbox = session.createReply();
        outbox.setContent("Query missions session " + sessionKey);
        this.LARVAsend(outbox);
        session = LARVAblockingReceive();
        this.decodeMissions(session.getContent());
        String m;
        if (this.getNumMissions() == 1) {
            m = this.getAllMissions()[0];
        } else {
            m = this.inputSelect("Please chhoose a mission", getAllMissions(), "");
        }
        return m;
    }

    protected ArrayList<String> getDroidShips() {
        ArrayList<String> droids = this.DFGetAllProvidersOf("DROIDSHIP");
        droids.retainAll(this.DFGetAllProvidersOf(sessionKey));
        return droids;
    }

    protected void requestDroids(String city, String criterium) {
        ArrayList<String> droidlist = this.getDroidShips();
        TimeHandler tini, tend;
        String contracted;
        double best;
        double propose;

        if (droidlist.isEmpty()) {
            //Message("No droidships found. Continuing");
            return;
        }
        //Message("Found droidships " + droidlist.toString());
        outbox = new ACLMessage(ACLMessage.CFP);
        outbox.setSender(getAID());
        droidlist.forEach(s -> {
            outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
        });
        outbox.setContent("CFP BYDISTANCE " + city);
        outbox.setReplyWith(outbox.getContent());
        this.LARVAsend(outbox);
        //Message("Sending CFP MOVEIN " + city + " to " + droidlist.toString());
        tini = new TimeHandler();
        contracted = null;
        best = 10000;
        int nmessages;
        nmessages = 0;
        while (!droidlist.isEmpty()) {
            tend = new TimeHandler();
            if (nmessages == droidlist.size() || tini.elapsedTimeMilisecsUntil(tend) > 5000) {
                break;
            }
            inbox = this.LARVAblockingReceive();
            if (inbox != null && inbox.getContent().toUpperCase().startsWith("PROPOSE ")) {
                nmessages++;
                try {
                    propose = Double.parseDouble(inbox.getContent());
                } catch (Exception ex) {
                    propose = 0;
                }
                if (propose < best) {
                    contracted = inbox.getSender().getLocalName();
                    best = propose;
                }
            }
            if (inbox != null && inbox.getContent().toUpperCase().startsWith("REFUSE")) {
                nmessages++;
                droidlist.remove(inbox.getSender().getLocalName());
            }
        }
        if (contracted != null) {
            //Message(contracted + " has answered. Sending ACCEPT");
            outbox = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            outbox.setSender(getAID());
            outbox.addReceiver(new AID(contracted, AID.ISLOCALNAME));
            outbox.setContent("ACCEPT MOVEIN " + city);
            this.LARVAsend(outbox);
            inbox = this.LARVAblockingReceive(); //this.LARVAblockingReceive(MessageTemplate.MatchSender(new AID(contracted, AID.ISLOCALNAME)));
//            Message("Sending ACCEPT MOVEIN " + city);

        } else {
//            Message("No one answered. Abandon");
            return;
        }

        droidlist.remove(contracted);

        if (!droidlist.isEmpty()) {
            //Message("Sending REJECT to "+droidlist.toString());
            outbox = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            outbox.setSender(getAID());
            outbox.setContent("REJECT MOVEIN " + city);
            for (String s : droidlist) {
                outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
            }
            this.LARVAsend(outbox);
        }
        //Message("Wait to be done");
        inbox = this.LARVAblockingReceive(30000); //this.LARVAblockingReceive(MessageTemplate.MatchSender(new AID(contracted, AID.ISLOCALNAME)));
        outbox = new ACLMessage(ACLMessage.CANCEL);

        outbox.setSender(getAID());
        outbox.addReceiver(
                new AID(contracted, AID.ISLOCALNAME));
        outbox.setContent("CANCEL MOVEIN " + city);

        this.LARVAsend(outbox);
        //Message("Done");
    }

    public Status translateStatus(String s) {
        try {
            return Status.valueOf(s);
        } catch (Exception ex) {
            return Status.EXIT;
        }
    }

}
