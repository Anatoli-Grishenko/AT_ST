package ImperialShips;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import ATST.AT_ST;
import ATST.AT_ST_DIRECTDRIVE;
import Environment.Environment;
import STF.STF_FULL;
import agents.LARVAFirstAgent;
import ai.Choice;
import ai.DecisionSet;
import console.Console;
import static crypto.Keygen.getHexaKey;
import data.Transform;
import geometry.Compass;
import geometry.Point3D;
import static glossary.Goals.MOVETO;
import static glossary.Mission.CAPTURE;
import static glossary.Mission.MOVEIN;
import static glossary.Mission.RECRUIT;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.Collections;
import messaging.ACLMessageTools;
import tools.TimeHandler;
import tools.emojis;
import world.Perceptor;

/**
 *
 * @author lcv
 */
public class MASTER_DRIVE_AIRBORNE extends STF_FULL {

    protected String myType;
    protected boolean recruitByCFP, recruitByREQUEST, retryRecruitment, AUTO = false;
    protected String cities[], baseCity, myReplyWith;
    protected ArrayList<String> knownAgents;

    @Override
    public void setup() {
        super.setup();
        useAlias = true;
        knownAgents = new ArrayList();
    }

    @Override
    public Status MyOpenProblem() {
        if (this.DFGetAllProvidersOf(service).isEmpty()) {
            Error("Service PMANAGER is down");
            return Status.CHECKOUT;
        }
        problemManager = this.DFGetAllProvidersOf(service).get(0);
        Info("Found problem manager " + problemManager);
        AUTO = !this.DFGetAllProvidersOf("OPEN ALIAS " + sessionAlias).isEmpty()
                && !this.DFGetAllProvidersOf("OWNER " + sessionAlias).isEmpty();
        String opener;
        if (AUTO) {
            opener = this.DFGetAllProvidersOf("OPEN ALIAS " + sessionAlias).get(0);
            if (!useAlias || sessionAlias == null || sessionAlias.length() == 0) {
                Alert("AUTO mode only available with session Alias");
                return Status.CHECKOUT;
            } else {
                Info("Session " + sessionAlias + " seems to be already open");
                for (String service : this.DFGetAllServicesProvidedBy(opener)) {
                    if (service.startsWith(sessionAlias)) {
                        sessionKey = service.split(" ")[1];
                        if (this.DFGetAllProvidersOf("SESSION MANAGER " + this.sessionKey).isEmpty()) {
                            Error("Sorry service SESSION MANAGER not found");
                            return Status.CLOSEPROBLEM;
                        }
                        this.sessionManager = this.DFGetAllProvidersOf("SESSION MANAGER " + this.sessionKey).get(0);
                        Info("Assigned to " + sessionManager + " in problem " + problemName + " during session " + sessionKey);
                        return Status.JOINSESSION;
                    }
                }
                Error("Sorry service SESSION MANAGER not found");
                return Status.CHECKOUT;

            }
        } else {
            if (useAlias) {
                problem = this.inputSelect("Please select problem to solve", problems, problem);
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
                    this.DFAddMyServices(new String[]{"OWNER " + sessionAlias});
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

    public @Override
    Status MyCloseProblem() {
        if (AUTO) {
            return Status.CHECKOUT;
        }
        outbox = open.createReply();
        outbox.setContent("Cancel session " + sessionKey);
        Info("Closing problem " + problem + ", session " + sessionKey);
        Info("PLAN: " + preplan);
        this.LARVAsend(outbox);
        inbox = LARVAblockingReceive();
        Info(problemManager + " says: " + inbox.getContent());
        return Status.CHECKOUT;
    }

    @Override
    public Status MyJoinSession() {
        nextWhichwall = whichWall = "NONE";
        nextdistance = distance = Choice.MAX_UTILITY;
        this.DFAddMyServices(new String[]{"TYPE " + myType});
        Info("Checking list of cities with " + sessionManager);
        this.doQueryCities();
        if (!AUTO) {
            String city = this.inputSelect("Please select a city to9start from",
                    E.getCityList(), "");
            if (city == null) {
                return Status.CLOSEPROBLEM;
            }
            outbox = session.createReply();
            outbox.setContent("Request join session " + sessionKey + " in "
                    + city);
//                + E.getCityList()[(int) (Math.random() * E.getCityList().length)]);
            this.LARVAsend(outbox);
            session = this.LARVAblockingReceive();
            if (!session.getContent().startsWith("Confirm")) {
                Error("Could not join session " + sessionKey + " due to " + session.getContent());
                return Status.CLOSEPROBLEM;
            }
            this.DFAddMyServices(new String[]{"SESSION " + sessionKey});
            this.MyReadPerceptions();
            this.doQueryPeople("PEOPLE");
//        this.openRemote();
            this.setFrameDelay(10);
            Info(this.easyPrintPerceptions());
            String m = chooseMission();
            return translateStatus(activateMission(m));
        } else {
            if (sessionKey.length() == 0) {
                Error("Sorry service SESSION MANAGER not found for alias " + sessionAlias);
                return Status.CHECKOUT;
            }
            cities = E.getCityList();
            if (cities.length == 0) {
                Error("Sorry this agent can only join worlds with cities");
                return Status.CHECKOUT;
            }
            baseCity = cities[(int) (Math.random() * cities.length)];
            Info("Joining session with base in  " + baseCity);
            outbox = session.createReply();
            outbox.setContent("Request join session " + sessionKey + " in " + baseCity);
            LARVAsend(outbox);
             if (!session.getContent().toUpperCase().startsWith("CONFIRM")) {
                Error("Could not join session " + sessionKey + " due to " + session.getContent());
                return Status.CHECKOUT;
            }
            this.MyReadPerceptions();
            this.doQueryPeople("PEOPLE");
//        this.openRemote();
            this.setFrameDelay(10);
            Info(this.easyPrintPerceptions());
            String m = chooseMission();
            return translateStatus(activateMission(m));
        }
    }

    @Override
    public Status MySolveProblem() {
        // Analizar objetivo
        Info(this.easyPrintPerceptions());
        if (G(E)) {
            Info("Target reached");
            if (isOverCurrentMission()) {
                Info("The problem is over");
                Message("The problem " + problem + " has been solved");
                return Status.CLOSEPROBLEM;
            } else {
                Info("Moving on to the next task");
                nextWhichwall = whichWall = "NONE";
                nextdistance = distance = Choice.MAX_UTILITY;
                return translateStatus(activateNextTask());
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
    ///////////////////////////////////////////////

    @Override
    protected String activateNextTask() {
        String parameters[];
        if (this.isOverCurrentMission()) {
            return Status.CLOSEPROBLEM.name();
        }
        nextTask();
        parameters = taskName.split(" ");
        if (parameters[0].equals(MOVEIN.name())) {
            if (parameters[1].equals(getEnvironment().getCurrentCity())) {
                return Status.SOLVEPROBLEM.name();
            }
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
        } else if (parameters[0].equals(MOVETO.name())) {
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
        } else if (parameters[0].equals(CAPTURE.name())) {
            this.doQueryPeople(parameters[1]);
            String who = E.getPeople()[0];
            outbox = session.createReply();
            outbox.setContent("Request capture " + who + " Session " + sessionKey);
            this.LARVAsend(outbox);
            session = this.LARVAblockingReceive();
            if (!session.getContent().toUpperCase().startsWith("FAILURE")) {
                E.setExternalPerceptions(session.getContent());
                Info("Successfully captured " + who);
                return Status.SOLVEPROBLEM.name();
            } else {
                Info("Failed to capture " + who);
                return Status.CLOSEPROBLEM.name();
            }
        } else if (parameters[0].equals(RECRUIT.name())) {
            String myCity = getEnvironment().getCurrentCity();
            if (this.recruitByCFP) {
                this.CFPDroids("MOVEIN " + myCity, "BYDISTANCE");
            } else if (this.recruitByREQUEST) {
                this.REQUESTDroids("MOVEIN " + myCity, "BYDISTANCE");
            }
            return activateNextTask();
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
            if (AUTO) {
                m = this.getAllMissions()[(int) (this.getAllMissions().length * Math.random())];
            } else {
                m = this.inputSelect("Please chhoose a mission", getAllMissions(), "");
            }
        }
        return m;
    }

    protected ArrayList<String> getDroidShips() {
        ArrayList<String> droids = this.DFGetAllProvidersOf("DROIDSHIP");
        droids.retainAll(this.DFGetAllProvidersOf(sessionKey));
        return droids;
    }

    protected void REQUESTDroids(String task, String criterium) {
        ArrayList<String> droidlist = this.getDroidShips();
        TimeHandler tini, tend;
        String contracted;
        double best;
        double propose;
        boolean agree = false;

        if (droidlist.isEmpty()) {
            // Message("No droidships found. Continuing");
            return;
        }
        logger.onEcho();
        Collections.shuffle(droidlist);
        // Message("Found droidships " + droidlist.toString());
        for (String s : droidlist) {
            outbox = new ACLMessage(ACLMessage.CFP);
            outbox.setSender(getAID());
            outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
            outbox.setContent("Request BYDISTANCE " + task);
            outbox.setReplyWith(task);
            // Message("Sending Request " + task + " to " + droidlist.toString());
            this.LARVAsend(outbox);
            inbox = this.LARVAblockingReceive();
            if (inbox.getContent().toUpperCase().startsWith("AGREE")) {
                Info(inbox.getSender().getLocalName() + " agreed");
                agree = true;
                break;
            }
            if (inbox.getContent().toUpperCase().startsWith("REFUSE")) {
                Print(inbox.getSender().getLocalName() + " agreed");
                break;
            }
        }
        if (!agree) {
            // Message("No one agreeed. Abandon");
            return;
        }
        // Message("Waiting INFORM");
        inbox = this.LARVAblockingReceive();
        // Message("Releasing contract");
        outbox = inbox.createReply();
        outbox.setContent("Cancel " + task);
        this.LARVAsend(outbox);
        logger.offEcho();
    }

    protected void CFPDroids(String task, String criterium) {
        ArrayList<String> droidlist = this.getDroidShips();
        TimeHandler tini, tend;
        String contracted;
        double best;
        double propose;
        int ncfp, npropose, naccept, nreject;

        if (droidlist.isEmpty()) {
            // Message("No droidships found. Continuing");
            return;
        }
        // Message("Found droidships " + droidlist.toString());
        outbox = new ACLMessage(ACLMessage.CFP);
        outbox.setSender(getAID());
        ncfp = droidlist.size();
        droidlist.forEach(s -> {
            outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
        });
        outbox.setContent("CFP BYDISTANCE " + task);
        outbox.setReplyWith(outbox.getContent());
        // Message("Sending CFP " + task + " to " + droidlist.toString());
        this.LARVAsend(outbox);
        tini = new TimeHandler();
        contracted = null;
        best = 10000;
        npropose = 0;
        while (ncfp > 0) {
            inbox = this.LARVAblockingReceive();
            if (inbox.getContent().toUpperCase().startsWith("PROPOSE ")) {
                ncfp--;
                npropose++;
                // Message(inbox.getSender().getLocalName() + " proposed");
                try {
                    propose = Double.parseDouble(inbox.getContent().split(" ")[1]);
                } catch (Exception ex) {
                    propose = Choice.MAX_UTILITY;
                }
                if (propose < best) {
                    contracted = inbox.getSender().getLocalName();
                    best = propose;
                }
            }
            if (inbox.getContent().toUpperCase().startsWith("REFUSE")) {
                ncfp--;
                // Message(inbox.getSender().getLocalName() + " refused");
                droidlist.remove(inbox.getSender().getLocalName());
            }
        }
        if (contracted != null) {
            outbox = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            outbox.setSender(getAID());
            outbox.addReceiver(new AID(contracted, AID.ISLOCALNAME));
            outbox.setContent("ACCEPT " + task);
            // Message("Sending ACCEPT " + task + " to " + droidlist.toString());
            this.LARVAsend(outbox);
            inbox = this.LARVAblockingReceive(); //this.LARVAblockingReceive(MessageTemplate.MatchSender(new AID(contracted, AID.ISLOCALNAME)));
            // Message("Received agree from " + contracted);
        } else {
            // Message("No one answered. Abandon");
            return;
        }

        droidlist.remove(contracted);

        if (!droidlist.isEmpty()) {
            // Message("Sending REJECT to " + droidlist.toString());
            outbox = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            outbox.setSender(getAID());
            outbox.setContent("REJECT " + task);
            for (String s : droidlist) {
                outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
            }
            this.LARVAsend(outbox);
        }
        // Message("Wait for INFORM");
        inbox = this.LARVAblockingReceive(); //this.LARVAblockingReceive(MessageTemplate.MatchSender(new AID(contracted, AID.ISLOCALNAME)));
        // Message("Releasing contract with " + contracted);
        outbox = new ACLMessage(ACLMessage.CANCEL);

        outbox.setSender(getAID());
        outbox.addReceiver(
                new AID(contracted, AID.ISLOCALNAME));
        outbox.setContent("CANCEL " + task);

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

    protected boolean isUnexpected(ACLMessage msg) {
        if (!knownAgents.contains(msg.getSender().getLocalName())) {
            if (this.DFHasService(msg.getSender().getLocalName(), "SESSION MANAGER")) {
                knownAgents.add(msg.getSender().getLocalName());
            }
        }
        return !knownAgents.contains(msg.getSender().getLocalName());
    }

    @Override
    protected void LARVAsend(ACLMessage msg) {
        for (String s: ACLMessageTools.getAllReceivers(msg).split(",")) {
            knownAgents.add(s);
        }
        super.LARVAsend(msg);
    }

    @Override
    protected ACLMessage LARVAblockingReceive() {
        ACLMessage res;
        while (true) {
            res = super.LARVAblockingReceive();
            if (isUnexpected(res)) {
                this.processUnexpectedMessage(res);
            } else {
                return res;
            }
        }
    }

    @Override
    protected ACLMessage LARVAblockingReceive(long milis) {
        ACLMessage res;
        TimeHandler tini = new TimeHandler(), tend = tini;
        while (tini.elapsedTimeMilisecsUntil(tend) < milis) {
            res = super.LARVAblockingReceive(milis);
            if (res != null) {
                if (isUnexpected(res)) {
                    this.processUnexpectedMessage(res);
                } else {
                    return res;
                }
            }
            tend = new TimeHandler();
        }
        return null;
    }

    protected void processUnexpectedMessage(ACLMessage msg) {
        logger.onEcho();
        String tokens[] = msg.getContent().split(",")[0].split(" ");
        Info("Unexpected " + msg.getContent());
        if (tokens[0].toUpperCase().equals("TRANSPONDER")) {
            outbox = msg.createReply();
            outbox.setContent("GPS " + getEnvironment().getGPS().toString());
            this.LARVAsend(outbox);
        } else {
            Message("Refuse to " + msg.getContent());
            Info("Refuse " + msg.getContent());
            outbox = msg.createReply();
            outbox.setContent("Refuse");
            this.LARVAsend(outbox);
        }
    }
}
/*
        [
            "MISSION Hiro (R)",
            "MOVEIN Hiro",
            "RECRUIT Droidship"

        ],
        [
            "MISSION Spine (R)",
            "MOVEIN Spine",
            "RECRUIT Droidship"
        ],
        [
            "MISSION Lamaredd (R)",
            "MOVEIN Lamaredd", 
            "RECRUIT Droidship"

        ],
        [
            "MISSION OuterRim",
            "MOVEIN OuterRim"
        ],
        [
            "MISSION Spine-OuterRim",
            "MOVEIN Spine",
            "MOVEIN OuterRim"
        ],
        [
            "MISSION Lamared-Jakku",
            "MOVEIN Lamaredd",
            "MOVEIN Jakku"
        ],
        [
            "MISSION Lamared-Jakku-Hiro-Lamaredd",
            "MOVEIN Lamaredd",
            "RECRUIT Droidship",
            "MOVEIN Jakku",
            "MOVEIN Hiro",
            "MOVEIN Lamaredd",
            "RECRUIT Droidship",
            "MOVEIN Jakku",
            "MOVEIN Hiro",
            "MOVEIN Lamaredd",
            "RECRUIT Droidship",
            "MOVEIN Jakku",
            "MOVEIN Hiro"

        ],
        [
            "MISSION Spine-OuterRim-Spine",
            "MOVEIN Spine",
            "RECRUIT Droidship",
            "MOVEIN OuterRim",
            "MOVEIN Spine",
            "RECRUIT Droidship",
            "MOVEIN OuterRim",
            "MOVEIN Spine",
            "RECRUIT Droidship",
            "MOVEIN OuterRim",
            "MOVEIN Spine",
            "RECRUIT Droidship",
            "MOVEIN OuterRim"
        ]
 */
