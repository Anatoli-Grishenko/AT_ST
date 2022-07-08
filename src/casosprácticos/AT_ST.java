/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

import agents.LARVAFirstAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import tools.emojis;
import world.Perceptor;

/**
 *
 * @author lcv
 */
public class AT_ST extends LARVAFirstAgent {

    protected enum Status {
        START, CHECKIN, CHECKOUT, OPENPROBLEM, CLOSEPROBLEM, JOINSESSION, SOLVEPROBLEM, EXIT
    }
    protected Status myStatus;
    protected String service = "PMANAGER", problem = "",
            problemManager = "", content, sessionKey, sessionManager;
    protected String problems[], plan[], actions[];
    protected ACLMessage open, session;
    protected String[] contentTokens;
    protected String action = "", preplan = "";
    protected int indexplan = 0;

    @Override
    public void setup() {
        super.setup();
        logger.onTabular();
        myStatus = Status.START;
        this.setupEnvironment();

        actions = new String[]{
            "LEFT",
            "RIGHT",
            "MOVE",
            "UP",
            "DOWN",
            "EXIT"};
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
            "SandboxHalfmoon3",};
    }

    @Override
    public void Execute() {
        Info("Status: " + myStatus.name());
        switch (myStatus) {
            case START:
                myStatus = Status.CHECKIN;
                break;
            case CHECKIN:
                myStatus = MyCheckin();
                break;
            case OPENPROBLEM:
                myStatus = MyOpenProblem();
                break;
            case JOINSESSION:
                myStatus = MyJoinSession();
                break;
            case SOLVEPROBLEM:
                myStatus = MySolveProblem();
                break;
            case CLOSEPROBLEM:
                myStatus = MyCloseProblem();
                break;
            case CHECKOUT:
                myStatus = MyCheckout();
                break;
            case EXIT:
            default:
                doExit();
                break;
        }
    }

    @Override
    public void takeDown() {
        Info("Taking down...");
        this.saveSequenceDiagram("./" + getLocalName() + ".seqd");
        super.takeDown();
    }

    public Status MyCheckin() {
        Info("Loading passport and checking-in to LARVA");
        //this.loadMyPassport("config/ANATOLI_GRISHENKO.passport");
        if (!doLARVACheckin()) {
            Error("Unable to checkin");
            return Status.EXIT;
        }
        return Status.OPENPROBLEM;
    }

    public Status MyCheckout() {
        this.doLARVACheckout();
        return Status.EXIT;
    }

    public Status MyOpenProblem() {

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
        outbox.setContent("Request open " + problem);
        this.LARVAsend(outbox);
        Info("Request opening problem " + problem + " to " + problemManager);
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
    }

    public Status MyJoinSession() {
        this.DFAddMyServices(new String[]{"TYPE AT_ST"});
        outbox = session.createReply();
        outbox.setContent("Request join session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().startsWith("Confirm")) {
            Error("Could not join session " + sessionKey + " due to " + session.getContent());
            return Status.CLOSEPROBLEM;
        }
        if (problem.equals("SandboxTesting")) {
            this.AssistedNavigation(37, 13);
        }
        this.MyReadPerceptions();
        this.openRemote();
        this.setFrameDelay(100);
        Info(this.easyPrintPerceptions());
        return Status.SOLVEPROBLEM;
    }

    public Status MySolveProblem() {
        this.MyReadPerceptions();
        if (plan == null) {
            action = this.inputSelect("Please select next action to execute: ", actions, action);
            preplan += "\"" + action + "\",";
        } else {
            action = plan[indexplan++];
        }
        if (action == null || action.equals("EXIT")) {
            return Status.CLOSEPROBLEM;
        }
        if (!MyExecuteAction(action)) {
            return Status.CLOSEPROBLEM;
        }
        return Status.SOLVEPROBLEM;
    }

    protected Status AssistedNavigation(int goalx, int goaly) {
        String plan1[] = {};

        Info("Requesting course to " + goalx + " " + goaly);
        outbox = session.createReply();
        outbox.setContent("Request course to " + goalx + " " + goaly + " Session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        getEnvironment().setExternalPerceptions(session.getContent());
        return MySolveProblem();
    }

    protected boolean MyExecuteAction(String action) {
        Info("Executing action " + action);
        outbox = session.createReply();
        outbox.setContent("Request execute " + action + " session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().startsWith("Inform")) {
            Error("Unable to execute action " + action + " due to " + session.getContent());
            return false;
        }
        return true;
    }

    protected boolean MyReadPerceptions() {
        Info("Reading perceptions");
        outbox = session.createReply();
        outbox.setContent("Query sensors session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (session.getContent().startsWith("Failure")) {
            Error("Unable to read perceptions due to " + session.getContent());
            return false;
        }
        getEnvironment().setExternalPerceptions(session.getContent());
//        Info(this.easyPrintPerceptions());
        return true;
    }

    public Status MyCloseProblem() {
        outbox = open.createReply();
        outbox.setContent("Cancel session " + sessionKey);
        Info("Closing problem " + problem + ", session " + sessionKey);
        Info("PLAN: " + preplan);
        this.LARVAsend(outbox);
        inbox = LARVAblockingReceive();
        Info(problemManager + " says: " + inbox.getContent());
        return Status.CHECKOUT;
    }

    public String easyPrintPerceptions() {
        String res;
        int matrix[][];

        if (getEnvironment() == null) {
            Error("Environment is unacessible, please setupEnvironment() first");
            return "";
        }
        res = "\n\nReading of sensors\n";
        if (getEnvironment().getName() == null) {
            res += emojis.WARNING + " UNKNOWN AGENT";
            return res;
        } else {
            res += emojis.ROBOT + " " + getEnvironment().getName();
        }
        res += "\n";
        res += String.format("%10s: %05d W\n", "ENERGY", getEnvironment().getEnergy());
        res += String.format("%10s: %15s\n", "POSITION", getEnvironment().getGPS().toString());
//        res += "PAYLOAD "+getEnvironment().getPayload()+" m"+"\n";
        res += String.format("%10s: %05d m\n", "X", getEnvironment().getGPS().getXInt())
                + String.format("%10s: %05d m\n", "Y", getEnvironment().getGPS().getYInt())
                + String.format("%10s: %05d m\n", "Z", getEnvironment().getGPS().getZInt())
                + String.format("%10s: %05d m\n", "MAXLEVEL", getEnvironment().getMaxlevel())
                + String.format("%10s: %05d m\n", "MAXSLOPE", getEnvironment().getMaxslope());
        res += String.format("%10s: %05d m\n", "GROUND", getEnvironment().getGround());
        res += String.format("%10s: %05d º\n", "COMPASS", getEnvironment().getCompass());
        if (getEnvironment().getTarget() == null) {
            res += String.format("%10s: " + "!", "TARGET");
        } else {
            res += String.format("%10s: %05.2f m\n", "DISTANCE", getEnvironment().getDistance());
            res += String.format("%10s: %05.2f º\n", "ABS ALPHA", getEnvironment().getAngular());
            res += String.format("%10s: %05.2f º\n", "REL ALPHA", getEnvironment().getRelativeAngular());
        }
//        res += "\nVISUAL ABSOLUTE\n";
//        matrix = getEnvironment().getAbsoluteVisual();
//        for (int y = 0; y < matrix[0].length; y++) {
//            for (int x = 0; x < matrix.length; x++) {
//                res += printValue(matrix[x][y]);
//            }
//            res += "\n";
//        }
//        for (int x = 0; x < matrix.length; x++) {
//            if (x != matrix.length / 2) {
//                res += "----";
//            } else {
//                res += "[  ]-";
//            }
//        }
        res += "\nVISUAL RELATIVE\n";
        matrix = getEnvironment().getRelativeVisual();
        for (int y = 0; y < matrix[0].length; y++) {
            for (int x = 0; x < matrix.length; x++) {
                res += printValue(matrix[x][y]);
            }
            res += "\n";
        }
        for (int x = 0; x < matrix.length; x++) {
            if (x != matrix.length / 2) {
                res += "----";
            } else {
                res += "[  ]-";
            }
        }
//        res += "VISUAL POLAR\n";
//        matrix = getEnvironment().getPolarVisual();
//        for (int y = 0; y < matrix[0].length; y++) {
//            for (int x = 0; x < matrix.length; x++) {
//                res += printValue(matrix[x][y]);
//            }
//            res += "\n";
//        }
//        res += "\n";
        res += "LIDAR RELATIVE\n";
        matrix = getEnvironment().getRelativeLidar();
        for (int y = 0; y < matrix[0].length; y++) {
            for (int x = 0; x < matrix.length; x++) {
                res += printValue(matrix[x][y]);
            }
            res += "\n";
        }
        for (int x = 0; x < matrix.length; x++) {
            if (x != matrix.length / 2) {
                res += "----";
            } else {
                res += "-^^-";
            }
        }
        res += "\n";
        return res;
    }

    String printValue(int v) {
        if (v == Perceptor.NULLREAD) {
            return "XXX ";
        } else {
            return String.format("%03d ", v);
        }
    }

    String printValue(double v) {
        if (v == Perceptor.NULLREAD) {
            return "XXX ";
        } else {
            return String.format("%05.2f ", v);
        }
    }

}
