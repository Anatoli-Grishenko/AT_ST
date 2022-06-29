/*
* Create new project
* Copy from AgentLARVAFull
* Introduce new State JOINSESSION, create myJoinSession(), add change of state after OpenProblem
* Introduce new probles: Array problems[] and inputSelect()
* Join Session
* Explain HUD y MAP.
* SolveProblem: step a step
* Manual
* semiauto
* assisted
 */
package at_st;

import agents.LARVAFirstAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import tools.emojis;
import world.Perceptor;

public class AT_ST extends LARVAFirstAgent {

    enum Status {
        START, CHECKIN, CHECKOUT, OPENPROBLEM, CLOSEPROBLEM, JOINSESSION, SOLVEPROBLEM, EXIT
    }
    Status myStatus;
    String service = "PMANAGER", problem = "",
            problemManager = "", content, sessionKey, sessionManager;
    String problems[];
    ACLMessage open, session;
    String[] contentTokens;

    @Override
    public void setup() {
        //this.enableDeepLARVAMonitoring(); // ESTO
        super.setup();
        logger.onTabular();
        myStatus = Status.START;
        problems = new String[]{ // This
            "SandboxBasic",
        "SandboxFlat-1-1"};
        this.setupEnvironment(); // This
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
//                myStatus = FreeNavigation(new String[]{"RIGHT","RIGHT","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","RIGHT","MOVE","RIGHT","MOVE"}); //FreeNavigation(); // This
//                myStatus = FreeNavigation(new String[]{"LEFT","LEFT","LEFT","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","LEFT","MOVE","MOVE","MOVE","MOVE","MOVE","RIGHT","MOVE","MOVE","MOVE","MOVE","MOVE","LEFT","MOVE"}); //FreeNavigation(); // This
                myStatus = AssistedNavigation(77, 52, new String[]{"RIGHT","RIGHT","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","RIGHT","MOVE","RIGHT","MOVE"}); //FreeNavigation(); // This
                myStatus = AssistedNavigation(90,33,new String[]{"LEFT","LEFT","LEFT","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","MOVE","LEFT","MOVE","MOVE","MOVE","MOVE","MOVE","RIGHT","MOVE","MOVE","MOVE","MOVE","MOVE","LEFT","MOVE"}); //FreeNavigation(); // This
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
        problem = this.inputSelect("Problem Manager " + problemManager + " is on."
                + "\nPlease select the problem to open", problems, "");
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
            return Status.JOINSESSION;   // This
        } else {
            Error(content);
            return Status.CHECKOUT;
        }
    }

    // This
    public Status MyJoinSession() {
        String command, answer;
        int startx = 50, starty = 50;
//        Info("Joining session " + sessionKey + " at " + startx + " " + starty);
        // Declare publicily my type so that everybody could ask DF
        this.DFAddMyServices(new String[]{"TYPE AT_ST"});
        // request join at a certain point in the map
//        command = "Request join session " + sessionKey + " at " + startx + " " + starty;
        command = "Request join session " + sessionKey;
        outbox = session.createReply();
        outbox.setContent(command);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (!session.getContent().startsWith("Confirm")) {
            Error("Could not join session due to: " + session.getContent());
            return Status.CLOSEPROBLEM;
        }
        if (!this.MyReadPerceptions()) {
            return Status.CLOSEPROBLEM;
        }
        return Status.SOLVEPROBLEM;
    }

    // This
    protected Status FreeNavigation(String steps[]) {
        String action = "", preplan = "", plan = "";
        boolean exit = false;
        int i = 0;
        do {
            if (steps.length > 0 && i<steps.length) {                
                action = steps[i++];
                this.LARVAwait(500);
            } else {
                action = this.inputSelect("Execute ", new String[]{"RIGHT", "LEFT", "MOVE", "EXIT"}, action);
            }
            if (action == null || action.equals("EXIT")) {
                exit = true;
            } else {
                if (!MyExecuteAction(action)) {
                    exit = true;
                }
                this.MyReadPerceptions();
                plan += "\"" + action + "\",";
            }
        } while (!exit);
        Info("Plan =\n" + plan);
        return Status.CLOSEPROBLEM;
    }

//    // This
    protected Status AssistedNavigation(int x, int y, String steps[]) {
        int goalx, goaly;
        String plan1[]={};
        
        goalx=x; goaly=y;        
        Info("Requesting course to " + goalx + " " + goaly);
        outbox = session.createReply();
        outbox.setContent("Request course to " + goalx + " " + goaly + " Session " + sessionKey);
        this.LARVAsend(outbox);
        session = this.LARVAblockingReceive();
        if (session.getContent().startsWith("Failure")) {
            Error("Could not find a course to " + goalx + " " + goaly + " due to " + session.getContent());
            return Status.CLOSEPROBLEM;
        }
        if (getEnvironment().setExternalPerceptions(session.getContent()) == null) {
            Error("Unable to find a path due to " + session.getContent());
            return Status.CLOSEPROBLEM;
        }
        return FreeNavigation(steps);
    }

    // This
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
        if (getEnvironment().setExternalPerceptions(session.getContent()) != null) {
            Info(easyPrintPerceptions());
            return true;
        } else {
            return false;
        }
    }

    // This
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

    protected Status MyCloseProblem() {
        outbox = open.createReply();
        outbox.setContent("Cancel session " + sessionKey);
        Info("Closing problem Helloworld, session " + sessionKey);
        this.LARVAsend(outbox);
        inbox = LARVAblockingReceive();
        Info(problemManager + " says: " + inbox.getContent());
        return Status.CHECKOUT;
    }

    // This
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
        res += "VISUAL\n";
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
        res += "\n";
        res += "LIDAR\n";
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

//        for (int y = 0; y < Obstacle[0].length; y++) {
//            for (int x = 0; x < Obstacle.length; x++) {
//                svalue = String.format("%4s",
//                        (Obstacle[x][y] == Perceptor.NULLREAD
//                                ? "XXX" : String.format("%3d", Obstacle[x][y])));
//                if (x == Obstacle.length / 2 && y == Obstacle[0].length / 2) {
//                    console.print(label(svalue));
//                } else {
//                    console.print(value(svalue));
//                }
//            }
//            console.println("");
//        }
//        console.setCursorXY(2, 12);
//        console.setText(Console.white);
//        console.doFrame(1, 1, cw, 3);
//        console.doFrame(1, 4, cw, ch - 3);
        res += "\n";
        return res;
    }
    
    String printValue(int v) {
        if (v == Perceptor.NULLREAD) {
            return "XXX ";
        } else
            return String.format("%03d",v);
    }

    String printValue(double v) {
        if (v == Perceptor.NULLREAD) {
            return "XXX ";
        } else
            return String.format("%05.2f",v);
    }

}
