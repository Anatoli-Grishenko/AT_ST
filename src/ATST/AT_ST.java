package ATST;

import agents.LARVAFirstAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import tools.TimeHandler;
import tools.emojis;
import world.Perceptor;

/*
This is a fully operable agent that conencts to a world, execute actions
and read perceptions, nut does not have autonomy of decisions, that is, we tell it
what to do with a meu, but it sets the skeleton for future autonomos versions from the 
basic structure of HwelloWorld.AgentLARVAFull.java 
 */
public class AT_ST extends LARVAFirstAgent {

    // The same statuses but there is a new one: JOINSESSION
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
    protected int indexplan = 0, myEnergy = 0, indexSensor = 0;
    protected boolean showPerceptions = false, useAlias = false;

    @Override
    public void setup() {
        this.enableDeepLARVAMonitoring();
        super.setup();
        showPerceptions = false;
        logger.onTabular();
        myStatus = Status.START;
        // Thse brand-new agents have their own, powerful Environment, capable of
        // storing much information about the real environment of the agent coming from 
        // the perceptions. See reference for the list of powerful methods
        this.setupEnvironment();
        openRemote();
        actions = new String[]{
            "LEFT",
            "RIGHT",
            "MOVE",
            "UP",
            "DOWN",
            "EXIT"};
        problems = new String[]{
            "SandboxTesting",
            "FlatNorth",
            "FlatNorthWest",
            "FlatSouth",
            "Bumpy0",
            "Bumpy1",
            "Bumpy2",
            "Bumpy3",
            "Bumpy4",
            "Halfmoon1",
            "Halfmoon3",
            "Dagobah.Apr1",
            "Dagobah.Apr2",
            "Dagobah.Not1",
            "Dagobah.Not2",
            "Endor.Sob1",
            "Endor.Sob2",
            "Endor.Hon1",
            "Endor.Hon2",
            "Wobani.Apr1",
            "Wobani.Not1",
            "Wobani.Sob1",
            "Wobani.Hon1",
            "SandboxTesting.Alaska",
            "Zeffo"
        };
        //        this.openRemote();
        this.activateSequenceDiagrams();
//        this.deactivateSequenceDiagrams();
        this.setFrameDelay(200);
    }

    // 99% Recycled from AgentLARVAFull
    @Override
    public void Execute() {
        Info("\n\n\nStatus: " + myStatus.name());
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
            case JOINSESSION: // This is new wrt HelloWorld
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

    // 100% Reciclado de HelloWorld
    @Override
    public void takeDown() {
        Info("Taking down...");
        super.takeDown();
    }

    // 100% Reciclado de HelloWorld
    public Status MyCheckin() {
        Info("Loading passport and checking-in to LARVA");
        //this.loadMyPassport("config/ANATOLI_GRISHENKO.passport");
        if (!doLARVACheckin()) {
            Error("Unable to checkin");
            return Status.EXIT;
        }
        return Status.OPENPROBLEM;
    }

    // 100% Reciclado de HelloWorld
    public Status MyCheckout() {
        this.doLARVACheckout();
        return Status.EXIT;
    }

    // 99% Reciclado de HelloWorld
    public Status MyOpenProblem() {

        if (this.LARVADFGetAllProvidersOf(service).isEmpty()) {
            Error("Service PMANAGER is down");
            return Status.CHECKOUT;
        }
        problemManager = this.LARVADFGetAllProvidersOf(service).get(0);
        Info("Found problem manager " + problemManager);
        problem = "SandboxTesting";
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
            return Status.JOINSESSION; // This is the only change wrt HelloWorld
        } else {
            Error(content);
            return Status.CHECKOUT;
        }
    }

    // This is 100% A NEW METHOD
    // Just register in the DF as a terrestrial agent AT_ST
    public Status MyJoinSession() {
        // Register in the DF
        this.LARVADFAddMyServices(new String[]{"TYPE AT_ST"});
        outbox = session.createReply();
        outbox.setContent("Request join session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().startsWith("Confirm")) {
            Error("Could not join session " + sessionKey + " due to " + session.getContent());
            return Status.CLOSEPROBLEM;
        }
        // Immediately afterwards, read the first perceptions
        this.MyReadPerceptions();
//        Info(this.easyPrintPerceptions());
        // Mark this GPS positoin as our destination
        return this.myAssistedNavigation(37, 13);
    }

    // No autonomy. Just ask the user what to do next
    public Status MySolveProblem() {
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
        this.MyReadPerceptions();
        return Status.SOLVEPROBLEM;
    }

    // Just mark an X Y position as our next target. No more
    protected Status myAssistedNavigation(int goalx, int goaly) {
        Info("Requesting course to " + goalx + " " + goaly);
        outbox = session.createReply();
        outbox.setContent("Request course to " + goalx + " " + goaly + " Session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        getEnvironment().setExternalPerceptions(session.getContent());
        return Status.CHECKIN.SOLVEPROBLEM;
    }

    // 100% New method to execute an action
    protected boolean MyExecuteAction(String action) {
        Info("Executing action " + action);
        outbox = session.createReply();
        // Remember to include sessionID in all communications
        outbox.setContent("Request execute " + action + " session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().startsWith("Inform")) {
            Error("Unable to execute action " + action + " due to " + session.getContent());
            return false;
        }
        return true;
    }

    // Read perceptions and send them directly to the Environment instance,
    // so we can query any items of sensors and added-value information
    protected boolean MyReadPerceptions() {
        Info("Reading perceptions");
        outbox = session.createReply();
        outbox.setContent("Query sensors session " + sessionKey);
        this.LARVAsend(outbox);
        this.myEnergy++;
        session = this.LARVAblockingReceive();
        if (session.getContent().startsWith("Failure")) {
            Error("Unable to read perceptions due to " + session.getContent());
            return false;
        }
        getEnvironment().setExternalPerceptions(session.getContent());
        Info(this.easyPrintPerceptions());
        return true;
    }

    // 99% recycled from HelloWorld
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

    // A new method just to show the information of sensors in console
    public String easyPrintPerceptions() {
        String res;
        int matrix[][];
        if (!logger.isEcho()) {
            return "";
        }
        if (getEnvironment() == null) {
            Error("Environment is unacessible, please setupEnvironment() first");
            return "";
        }
        if (!showPerceptions) {
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
        res += String.format("%10s: %05d W %05d W %05d W\n", "ENERGY",
                getEnvironment().getEnergy(), getEnvironment().getEnergyburnt(), myEnergy);
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

    protected String printValue(int v) {
        if (v == Perceptor.NULLREAD) {
            return "XXX ";
        } else {
            return String.format("%03d ", v);
        }
    }

    protected String printValue(double v) {
        if (v == Perceptor.NULLREAD) {
            return "XXX ";
        } else {
            return String.format("%05.2f ", v);
        }
    }

}
