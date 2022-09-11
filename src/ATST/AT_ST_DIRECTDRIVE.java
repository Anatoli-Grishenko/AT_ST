package ATST;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import Environment.Environment;
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

public class AT_ST_DIRECTDRIVE extends AT_ST {

    @Override
    public void setup() {
        super.setup();
        // Sequence Diagrams take too long in large trajectories because there are
        // a lot of messages coming and going.
        this.deactivateSequenceDiagrams();
//        this.setContinuousSequenceDiagram(false);
        // If you want to execute step by step, uncomment thisus
//        this.openRemote();
        
        // Thse brand-new agents have their own, powerful Environment, capable of
        // storing much information about the real environment of the agent coming from 
        // the perceptions. See reference for the list of powerful methods
        this.setupEnvironment();
        
        // A Decision set is a set of Choices to make, that is, the aciotns
        // that we would like to automate
        A = new DecisionSet();
        A.
                addChoice(new Choice("MOVE")).
                addChoice(new Choice("LEFT")).
                addChoice(new Choice("RIGHT"));
    }

    public Status MyOpenProblem() {

        if (this.DFGetAllProvidersOf(service).isEmpty()) {
            Error("Service PMANAGER is down");
            return Status.CHECKOUT;
        }
        problemManager = this.DFGetAllProvidersOf(service).get(0);
        Info("Found problem manager " + problemManager);
        // Selector of the problem to solve
        problem = this.inputSelect("Please select problem to solve", problems, problem);
        if (problem == null) {
            return Status.CHECKOUT;
        }
        this.outbox = new ACLMessage();
        outbox.setSender(getAID());
        outbox.addReceiver(new AID(problemManager, AID.ISLOCALNAME));
        outbox.setContent("Request open " + problem);
        this.LARVAsend(outbox);
        Info("opening problem " + problem + " to " + problemManager);
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

    /*
    Refactoring of the Utility Function U(E,A) to cope with disntances
    properly, so that turns to the left or right are evaluated more accurately
    */
    @Override
    protected double U(Environment E, Choice a) {
        if (a.getName().equals("MOVE")) {
            // I am going to move forward, the value of the Utility function is the distance
            // to the goal from the next position after the move
            return U(S(E, a));
        } else if (a.getName().equals("LEFT") || a.getName().equals("RIGHT")) {
            // If rotations are considered, them considering only distance would never
            // distinguish between left or right because it could be the same.
            // In this case, rotations are evaluated as if the were going to move
            // that is, the evaluation of left would be the evaluation of left+move
            return U(S(E, a), new Choice("MOVE"));
        }
        return Choice.MAX_UTILITY;
    }
    // Just register in the DF as a terrestrial agent AT_ST
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
        this.MyReadPerceptions();
        //Info(this.easyPrintPerceptions());
        return MySolveProblem();
    }

    
    /*
    Refactoring of the method MySolveProblem to behave as explained in theory
    It is just an implementation of a greedy algorithm to get to the desired target
    */
    @Override
    public Status MySolveProblem() {
        // Analizar objetivo
        Info(this.easyPrintPerceptions());
        // If the Environment encodes a goal state (we reached all goals and targets
        // then ends
        if (G(E)) {
            Message("The problem " + problem + " has been solved");
            return Status.CLOSEPROBLEM;
        }
        
        // Otherwise, when the Environment does not represent a goal state,
        // the we will have to do something, don't we? :-)
        // Call the default Ag() function which will use our refactored version
        // of the Utility function
        Choice a = Ag(E, A);
        // If no choice is selected is due to a confusion of the Ag( ) function, then stop
        if (a == null) {
            Alert("Found no action to execute");
            return Status.CLOSEPROBLEM;
        } else {
            // Otherwise, we have a choice: so let us execute it!
            Info("Excuting " + a);
            this.MyExecuteAction(a.getName());
            // Afterwards, read the sensors to reubicate the new state
            this.MyReadPerceptions();
            Info(this.easyPrintPerceptions());
            // If we have made a mistake in the selection of the bes choice a
            // to execute, this might compromise the integrity of the agent
            // Check that it has noe died
            if (!Ve(E)) {
                this.Error("The agent is not alive: " + E.getStatus());
                return Status.CLOSEPROBLEM;
            }
            
            // After all this, iterate again
            return Status.SOLVEPROBLEM;
        }
    }

    @Override
    public String easyPrintPerceptions() {
        if (!showPerceptions || !logger.isEcho()) {
            return "";
        }
        this.Prioritize(getEnvironment(), A);
        return super.easyPrintPerceptions()
                + "\nDECISION SET:\n" + A.toString() + "\n";
    }

}
