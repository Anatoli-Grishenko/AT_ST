package Lab1;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import ATST.AT_ST_FULL;
import Environment.Environment;
import agents.DEST;
import agents.VAAT;
import ai.Choice;
import ai.Mission;
import data.Transform;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.HashMap;
import tools.TimeHandler;

/**
 *
 * @author lcv
 */
public class MASTER_DRIVE_GROUND extends AT_ST_FULL {
    protected String myType;
    protected boolean recruitByCFP, recruitByREQUEST, retryRecruitment, AUTO = false;
    protected String cities[], baseCity, myReplyWith;
    protected HashMap<String, HashMap<String, ArrayList<String>>> census; // Cities x Type x list of people

    @Override
    public void setup() {
        super.setup();
        useAlias = true;
        census = new HashMap();
    }

    @Override
    public void takeDown() {
        this.doDestroyNPC();
        super.takeDown();
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
        if (AUTO) {
            return AutoMyOpenProblem();
        }
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

    @Override
    public Status MyCloseProblem() {
        if (AUTO) {
            return AutoMyCloseProblem();
        }
        outbox = open.createReply();
        outbox.setContent("Cancel session " + sessionKey);
        Info("Closing problem " + problem + ", session " + sessionKey);
        Info("PLAN: " + preplan);
        this.LARVAsend(outbox);
        inbox = LARVAblockingReceive();
        Info(problemManager + " says: " + inbox.getContent());
        this.doDestroyNPC();
        return Status.CHECKOUT;
    }

    @Override
    public Status MyJoinSession() {
        this.DFAddMyServices(new String[]{"TYPE " + myType});
        Info("Checking list of cities with " + sessionManager);
        this.doQueryCities();
        this.doQueryMissions();
        if (AUTO) {
            return AutoMyJoinSession();
        } else {
            String city = this.inputSelect("Please select a city to start from",
                    E.getCityList(), "");
            if (city == null) {
                return Status.CLOSEPROBLEM;
            }
            baseCity = city;
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
            this.doPrepareNPC(1, DEST.class);
            this.doPrepareNPC(1, VAAT.class);
//            this.doPrepareNPC(1, BB1F.class);
//            this.doPrepareNPC(1, YV.class);
            return SelectMission();
        }
    }

    public Status MyMoveProblem() {
        // Analizar objetivo
        Info(this.easyPrintPerceptions());
        if (G(E)) {
            Info("Target reached");
            return Status.CLOSEPROBLEM;
        }
        Choice a = Ag(E, A);
        if (a == null) {
            a = this.goRevolve(E);
        }
//            Alert("Found no action to execute");
//            return Status.CLOSEPROBLEM;
//        } else {// Execute
        Info("Excuting " + a);
//        System.out.println("Alternatives " + A.toString());
//        System.out.println("Excuting " + a);
        this.MyExecuteAction(a.getName());
        this.MyReadPerceptions();
        Info(this.easyPrintPerceptions());
        if (!Ve(E)) {
            this.Error("The agent is not alive: " + E.getStatus());
            return Status.CLOSEPROBLEM;
        }
        return Status.SOLVEPROBLEM;
//        }
    }

//    public Status SelectMission() {
//        String m = chooseMission();
//        if (m == null) {
//            return Status.CLOSEPROBLEM;
//        }
//        getEnvironment().setCurrentMission(m);
//        return Status.SOLVEPROBLEM;
//    }

    public Status SelectMission() {
        String m = chooseMission();
        if (m == null) {
            return Status.CLOSEPROBLEM;
        }
        if (m.equals("CUSTOM")) {
            E.setCurrentMission("CUSTOM",
                    new String[]{"MOVEIN " + this.inputSelect("Please select a custom destination", E.getCityList(), ""), "LIST Jedi", "LIST SITH"});

        } else {
            E.setCurrentMission(m);
        }
        resetAutoNAV();
        this.DFPublishMyStatus();
        this.MyReadPerceptions();
        census.clear();
        return Status.SOLVEPROBLEM;
    }

    @Override
    public Status MySolveProblem() {
        if (getEnvironment().getCurrentMission().isOver()) {
            Message("Mission " + getEnvironment().getCurrentMission().getName() + " has been solved in problem " + problemName);
            return SelectMission();
        }
        String goal[] = getEnvironment().getCurrentGoal().split(" ");
        switch (goal[0]) {
            case "MOVETO":
                if (E.getGPS().getX() == Integer.parseInt(goal[1])
                        && E.getGPS().getY() == Integer.parseInt(goal[2])
                        && E.getGround() == 0) {
                    getEnvironment().getCurrentMission().nextGoal();
                    this.DFPublishMyStatus();
                    resetAutoNAV();
                    return Status.SOLVEPROBLEM;
                } else if (this.doFindCourseTo(Integer.parseInt(goal[1]), Integer.parseInt(goal[2]))) {
                    return MyMoveProblem();
                } else {
                    Alert("Sorry, I cannot find a route in " + goal[0]);
                }
                return Status.SOLVEPROBLEM;
            case "MOVEIN":
                if (E.getCurrentCity().equals(goal[1])) {
                    E.getCurrentMission().nextGoal();
                    this.DFPublishMyStatus();
                    resetAutoNAV();
                    return Status.SOLVEPROBLEM;
                } else if (this.doFindCourseIn(E.getCurrentGoal().replaceAll("MOVEIN ", ""))) {
                    return MyMoveProblem();
                } else {
                    Alert("Sorry, I cannot find a route in " + goal[0]);
                    return this.SelectMission();
                }
            case "LIST":
                String type = goal[1];
                this.doQueryPeople(type);
                if (census.get(E.getCurrentCity()) == null) {
                    census.put(E.getCurrentCity(), new HashMap());
                }
                if (census.get(E.getCurrentCity()).get(type) == null) {
                    census.get(E.getCurrentCity()).put(type, new ArrayList());
                }
                this.census.get(E.getCurrentCity()).get(type).addAll(new ArrayList(Transform.toArrayList(E.getPeople())));
                E.getCurrentMission().nextGoal();
                this.DFPublishMyStatus();
                return Status.SOLVEPROBLEM;
            case "INFORM":
                String who = goal[1];
                String toReport = "INFORM;";
                for (String scity : census.keySet()) {
                    toReport += scity;

                    for (String stype : census.get(scity).keySet()) {
                        toReport += " " + stype + " " + census.get(scity).get(stype).size();
                    }
                    toReport += Mission.sepMissions;
                }
                outbox = new ACLMessage(ACLMessage.INFORM);
                outbox.setSender(getAID());
                outbox.addReceiver(new AID(who, AID.ISLOCALNAME));
                outbox.setContent(toReport);
                this.LARVAsend(outbox);
                inbox = LARVAblockingReceive();
                if (inbox.getContent().startsWith("Confirm")) {
                    E.getCurrentMission().nextGoal();
                    this.DFPublishMyStatus();
                    return Status.SOLVEPROBLEM;
                } else {
                    return Status.CLOSEPROBLEM;
                }
            case "REPORT":
                String destType = (goal.length > 1 ? goal[1] : "DEST");
                ArrayList<String> typelist = this.DFGetAllProvidersOf("TYPE " + destType);
                if (typelist.isEmpty()) {
                    Message("I have not found any " + destType + " to report to");
                    E.getCurrentMission().nextGoal();
                    return Status.SOLVEPROBLEM;
                }
                String destName = typelist.get(0),
                 destCity = "";
                for (String service : this.DFGetAllServicesProvidedBy(destName)) {
                    if (service.startsWith("GROUND")) {
                        destCity = service.split(" ")[1];
                    }
                }
                if (destCity.length() == 0) {
                    Message("Agent " + destName + " is not grounded yet");
                    this.LARVAwait(500);
                    return myStatus;
                }
                E.setCurrentMission("REPORT", new String[]{"MOVEIN " + destCity, "INFORM " + destName});
                this.DFPublishMyStatus();
                this.resetAutoNAV();
                return Status.SOLVEPROBLEM;
            case "EXIT":
                this.DFPublishMyStatus();
                return Status.CLOSEPROBLEM;
            default:
                Alert("Sorry I do not know how to reach goal " + E.getCurrentGoal());
                return Status.CLOSEPROBLEM;
        }
    }

    public boolean doFindCourseIn(String destination) {
        if (E.getDestinationCity().equals(destination)) {
            return true;
        }
        if (E.getCurrentCity().equals(destination)) {
            Message("I am already there");
            return true;
        }
        Info("Searching a route in " + destination);
        outbox = session.createReply();
        outbox.setContent("Request course in " + destination + " Session " + sessionKey);
        Info("Request course in " + destination + " Session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().toUpperCase().startsWith("FAILURE")) {
            E.setExternalPerceptions(session.getContent());
            Info("Successfully found a route in " + destination);
            return true;
        } else {
            Info("Failed to find a route to " + destination);
            return false;
        }
    }

    public boolean doFindCourseTo(int x, int y) {
        Info("Searching a route to " + x + " " + y);
        outbox = session.createReply();
        outbox.setContent("Request course to " + x + " " + y + " Session " + sessionKey);
        Info("Request course to " + x + " " + y + " Session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().toUpperCase().startsWith("FAILURE")) {
            E.setExternalPerceptions(session.getContent());
            Info("Successfully found a route");
            return true;
        } else {
            Info("Failed to find a route");
            return false;
        }
    }

//////// AUTO MODE
    public Status AutoMyOpenProblem() {
        String opener;
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
    }

    public Status AutoMyCloseProblem() {
        return Status.CHECKOUT;
    }

    public Status AutoMyJoinSession() {
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
        session = LARVAblockingReceive();
        if (!session.getContent().toUpperCase().startsWith("CONFIRM")) {
            Error("Could not join session " + sessionKey + " due to " + session.getContent());
            return Status.CHECKOUT;
        }
        if (E.getAllMissions().length == 0) {
            Message("AUTO mode only available in worlds with missions defined");
            return Status.CLOSEPROBLEM;
        }
        E.setCurrentMission(E.getAllMissions()[(int) (Math.random() * E.getAllMissions().length)]);
        resetAutoNAV();
        this.DFPublishMyStatus();
        this.DFAddMyServices(new String[]{"SESSION " + sessionKey});
        this.MyReadPerceptions();
        this.setFrameDelay(10);
        nextWhichwall = whichWall = "NONE";
        nextdistance = distance = Choice.MAX_UTILITY;
        Info(this.easyPrintPerceptions());
        census.clear();
        return Status.SOLVEPROBLEM;
    }

    @Override
    public double U(Environment E, Choice a) {
        switch (E.getCurrentGoal().split(" ")[0]) {
            case "MOVEIN":
            case "MOVETO":
                return goMove(E, a);
            case "LIST":
            case "REPORT":
                if (a.getName().equals(E.getCurrentGoal())) {
                    return Choice.ANY_VALUE;
                } else {
                    return Choice.MAX_UTILITY;
                }
            default:
            case "":
                return Choice.MAX_UTILITY;
        }
    }

    public double goMove(Environment E, Choice a) {
        if (E.getEnergy() * 100 / E.getAutonomy() < minimumEnergy && E.getVisualHere() > 5) {
            return goLowEnergy(E, a);
        }
        if (whichWall.equals("NONE") && E.isTargetBack()) {
            return goTurnBack(E, a);
        } else if (!whichWall.equals("NONE")) {
            return goFollowWall(E, a);
        } else if (!E.isFreeFront()
                && (S(E, new Choice("MOVE")).getDistance() < S(S(E, new Choice("LEFT")), new Choice("MOVE")).getDistance()
                && S(E, new Choice("MOVE")).getDistance() < S(S(E, new Choice("RIGHT")), new Choice("MOVE")).getDistance()) //                ||!Ve(S(E, new Choice("MOVE")))
                ) {
            return goAvoid(E, a);
        } else {
            return goAhead(E, a);
        }
    }


    public String doTranspond(String agentName) {
        if (this.AMSIsConnected(agentName)) {
            outbox = new ACLMessage();
            outbox.setSender(getAID());
            outbox.addReceiver(new AID(agentName, AID.ISLOCALNAME));
            outbox.setContent("TRANSPOND");
            this.LARVAsend(outbox);
            inbox = LARVAblockingReceive();
            return inbox.getContent();
        }
        return "";
    }
    public void DFPublishMyStatus() {
//        for (String service : DFGetAllServicesProvidedBy(getLocalName())) {
//            if (service.startsWith("MISSION")) {
//                DFRemoveMyServices(new String[]{service});
//            }
//            if (service.startsWith("GOAL")) {
//                DFRemoveMyServices(new String[]{service});
//            }
//        }
//        DFAddMyServices(new String[]{"MISSION " + E.getCurrentMission().getName()});
//        DFAddMyServices(new String[]{"GOAL " + E.getCurrentGoal()});
        outbox = session.createReply();
        outbox.setPerformative(ACLMessage.INFORM);
        outbox.setContent("INFORM "+Mission.sepMissions+"MISSION "+E.getCurrentMission().getName()+Mission.sepMissions+"GOAL "+E.getCurrentGoal());
        this.send(outbox);
    }

}
//    
//    @Override
//    public void setup() {
//        super.setup();
//    }
//
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
////        this.openRemote();
//        this.setFrameDelay(10);
//        Info(this.easyPrintPerceptions());
//        String m = chooseMission();
//        return translateStatus(activateCurrentMission(m));
//    }
//
//   ///////////////////////////////////////////////
//    @Override
//    protected String setCurrentGoal() {
//        String parameters[];
//        if (this.isOverCurrentMission()) {
//            return Status.CLOSEPROBLEM.name();
//        }
//        nextGoal();
//        parameters = taskName.split(" ");
//        if (parameters[0].equals("MOVEIN")) {
//            outbox = session.createReply();
//            outbox.setContent("Request course in " + parameters[1] + " Session " + sessionKey);
//            this.LARVAsend(outbox);
//            session = this.LARVAblockingReceive();
//            if (!session.getContent().toUpperCase().startsWith("FAILURE")) {
//                E.setExternalPerceptions(session.getContent());
//                Info("Successfully found a rote in " + taskName);
//                return Status.SOLVEPROBLEM.name();
//            } else {
//                Info("Failed to find a rote to " + taskName);
//                return Status.CLOSEPROBLEM.name();
//            }
//        } else if (parameters[0].equals("MOVETO")) {
//            outbox = session.createReply();
//            outbox.setContent("Request course to " + Integer.parseInt(parameters[1]) + " "
//                    + Integer.parseInt(parameters[2]) + " Session " + sessionKey);
//            this.LARVAsend(outbox);
//            session = this.LARVAblockingReceive();
//            if (!session.getContent().toUpperCase().startsWith("FAILURE")) {
//                E.setExternalPerceptions(session.getContent());
//                Info("Successfully found a rote to " + taskName);
//                return Status.SOLVEPROBLEM.name();
//            } else {
//                Info("Failed to find a rote to " + taskName);
//                return Status.CLOSEPROBLEM.name();
//            }
//        } else {
//            return Status.CLOSEPROBLEM.name();
//        }
//    }
//
//
//    ///////////////////////////////////////////////
//   
//
//    protected String chooseMission() {
//        Info("Querying the missions");
//        outbox = session.createReply();
//        outbox.setContent("Query missions session " + sessionKey);
//        this.LARVAsend(outbox);
//        session = LARVAblockingReceive();
//        this.decodeMissions(session.getContent());
//        String m;
//        if (this.getNumMissions() == 1) {
//            m = this.getAllMissions()[0];
//        } else {
//            m = this.inputSelect("Please chhoose a mission", getAllMissions(), "");
//        }
//        return m;
//    }
//
//    protected ArrayList<String> getDroidShips() {
//        ArrayList<String> droids = this.DFGetAllProvidersOf("DROIDSHIP");
//        droids.retainAll(this.DFGetAllProvidersOf(sessionKey));
//        return droids;
//    }
//
//    protected void requestDroids(String city, String criterium) {
//        ArrayList<String> droidlist = this.getDroidShips();
//        TimeHandler tini, tend;
//        String contracted;
//        int maxCargo, mindistance, proposeCargo;
//
//        if (droidlist.isEmpty()) {
//            return;
//        }
//        // Message("Found droidships " + droidlist.toString());
//        outbox = new ACLMessage(ACLMessage.CFP);
//        outbox.setSender(getAID());
//        droidlist.forEach(s -> {
//            outbox.addReceiver(new AID(s, AID.ISLOCALNAME));
//        });
//        outbox.setContent("CFP BYDISTANCE "+city);
//        outbox.setReplyWith(outbox.getContent());
//        this.LARVAsend(outbox);
//        // Message("CFP " + task);
//        tini = new TimeHandler();
//        contracted = null;
//        maxCargo = 0;
//        mindistance=100000;
//        int nmessages;
//        nmessages = 0;
//        while (!droidlist.isEmpty()) {
//            inbox = this.LARVAblockingReceive(500);
//            tend = new TimeHandler();
//            if (nmessages == droidlist.size() || tend.elapsedTimeMilisecsUntil(tini) > 5000) {
//                break;
//            }
//            if (inbox != null) {
//                nmessages++;
//                try {
//                    proposeCargo = Integer.parseInt(inbox.getContent());
//                } catch (Exception ex) {
//                    proposeCargo = 0;
//                }
//                if (proposeCargo > maxCargo) {
//                    contracted = inbox.getSender().getLocalName();
//                    maxCargo = proposeCargo;
//                }
//            }
//        }
//        if (contracted != null) {
//            // Message(contracted + " has answered");
//            outbox = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
//            outbox.setSender(getAID());
//            outbox.addReceiver(new AID(contracted, AID.ISLOCALNAME));
//            outbox.setContent("ACCEPT MOVEIN " + city);
//            this.LARVAsend(outbox);
////            inbox = this.LARVAblockingReceive(); //this.LARVAblockingReceive(MessageTemplate.MatchSender(new AID(contracted, AID.ISLOCALNAME)));
////            Message("ACCEPT " + task);
//
//        } else {
////            Message("No one answered");
//            return;
//        }
//        droidlist.remove(contracted);
//        if (!droidlist.isEmpty()) {
//            outbox = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
//            outbox.setSender(getAID());
//            outbox.setContent("REJECT MOVEIN "+city);
//            for (String s : droidlist) {
//                if (!s.equals(contracted)) {
//                    outbox.addReceiver(new AID(contracted, AID.ISLOCALNAME));
//                }
//            }
//            this.LARVAsend(outbox);
////            Message("Reject proposals");
//        }
//        Message("Wait to be done");
//        inbox = this.LARVAblockingReceive(5000); //this.LARVAblockingReceive(MessageTemplate.MatchSender(new AID(contracted, AID.ISLOCALNAME)));
//        outbox = new ACLMessage(ACLMessage.CANCEL);
//        outbox.setSender(getAID());
//        outbox.addReceiver(new AID(contracted, AID.ISLOCALNAME));
//        outbox.setContent("CANCEL MOVEUIN "+city);
//        this.LARVAsend(outbox);
//        Message("done");
//    }
//    
//    public Status translateStatus(String s) {
//        try {
//            return Status.valueOf(s);
//        } catch (Exception ex) {
//            return Status.EXIT;
//        }
//    }

//}
