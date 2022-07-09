/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

import Environment.Environment;
import agents.LARVAFirstAgent;
import ai.Choice;
import ai.DecisionSet;
import console.Console;
import geometry.Compass;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import tools.emojis;
import world.Perceptor;

/**
 *
 * @author lcv
 */



public class AT_ST_DIRECTDRIVE extends AT_ST {

    @Override
    public void setup() {
        super.setup();
        A = new DecisionSet();
        A.
                addChoice(new Choice("MOVE")).
                addChoice(new Choice("LEFT")).
                addChoice(new Choice("RIGHT"));
    }

    @Override
    protected double U(Environment E, Choice a) {
        if (a.getName().equals("MOVE")) {
            return U(S(E, a));
        } else {
            return U(S(E, a), new Choice("MOVE"));
        }
    }

    @Override
    public Status MySolveProblem() {
        // Analizar objetivo
        Info(this.easyPrintPerceptions());
        if (G(E)) {
            Info("The problem is over");
            this.Message("The problem " + problem + " has been solved");
            return Status.CLOSEPROBLEM;
        }
        Choice a = Ag(E, A);
        if (a == null) {
            Alert("Found no action to execute");
            return Status.CLOSEPROBLEM;
        } else {// Execute
            Info("Excuting " + a);
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

    @Override
        public String easyPrintPerceptions() {
        this.Prioritize(getEnvironment(), A);
        return super.easyPrintPerceptions()+ 
                "\nDECISION SET:\n" + A.toString() + "\n";
    }

}
