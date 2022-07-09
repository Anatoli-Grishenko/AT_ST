/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

import Environment.Environment;
import ai.Choice;
import ai.DecisionSet;
import ai.Plan;

public class STF_DELIBERATIVE extends STF_FULL {

    Plan behaviour = null;
    Environment Ei, Ef;
    Choice a;

    @Override
    public void setup() {
        super.setup();
        logger.onEcho();
        logger.onTabular();
    }

    protected Plan AgPlan(Environment E, DecisionSet A) {
        Plan result;
        Ei = E;
        Plan p = new Plan();
        for (int i = 0; i < Ei.getRange() / 2 - 2; i++) {
            Info("\nPlanning STEP:" + i + " :\n " + this.easyPrintPerceptions());
            if (!Ve(Ei)) {
                return null;
            } else if (G(Ei)) {
                return p;
            } else {
                a = super.Ag(Ei, A);
                if (a != null) {
                    p.add(a);
                    Ef = S(Ei, a);
                    Ei = Ef;
                } else {
                    return null;
                }
            }
        }
        return p;
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
        behaviour = AgPlan(E, A);
        if (behaviour.isEmpty()) {
            Alert("Found no plan to execute");
            return Status.CLOSEPROBLEM;
        } else {// Execute
            Info("Found plan: "+behaviour.toString());
            while (!behaviour.isEmpty()) {
                a = behaviour.get(0);
                behaviour.remove(0);
                Info("Excuting " + a);
                this.MyExecuteAction(a.getName());
                Info(this.easyPrintPerceptions());
                if (!Ve(E)) {
                    this.Error("The agent is not alive: " + E.getStatus());
                    return Status.CLOSEPROBLEM;
                }
            }
            return Status.SOLVEPROBLEM;
        }
    }

    @Override
    public String easyPrintPerceptions() {
        return super.easyPrintPerceptions()
                + "\nPlan :" + (behaviour == null ? "---\n" : behaviour.toString()) + "\n";
    }

}
