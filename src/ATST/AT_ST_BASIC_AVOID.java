package ATST;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import Environment.Environment;
import ai.Choice;

public class AT_ST_BASIC_AVOID extends AT_ST_DIRECTDRIVE {

    // New refacotiring of the Utility function. In this case we split it into two 
    // different cases:moving and avoiding
    @Override
    protected double U(Environment E, Choice a) {
        // If the position just in front of the agent, no matters which orientation
        // it has (polar/relative sensors) is freee, then move, otherwise,
        // turn to avoid
        if (!E.isFreeFront()) {
            return goAvoid(E, a);
        } else {
            return goAhead(E, a);
        }
    }

    // This is the old Utility function, now restricted to be used
    // Only when the position right in front of the agent is a valid movement
    protected double goAhead(Environment E, Choice a) {
        if (a.getName().equals("MOVE")) {
            return U(S(E, a));
        } else if (a.getName().equals("LEFT") || a.getName().equals("RIGHT")) {
            return U(S(E, a), new Choice("MOVE"));
        }
        return Choice.MAX_UTILITY;
    }

    
    // Split to avoid an obstacle
    public double goAvoid(Environment E, Choice a) {
        // By default, we avoid something in front of us by turing always right
        if (a.getName().equals("RIGHT")) {
            return Choice.ANY_VALUE; // I give the choice "RIGHT" a positive value, any
        }
        return Choice.MAX_UTILITY; // and the others just a penalty with the max value, so that the decisoin is clear, isn't it?
    }

}
