/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package casosprácticos;

import Environment.Environment;
import ai.Choice;

public class AT_ST_BASIC_AVOID extends AT_ST_DIRECTDRIVE {

 
    @Override
    protected double U(Environment E, Choice a) {
        if (E.isFreeFront()) {
            if (a.getName().equals("MOVE")) {
                return U(S(E, a));
            } else {
                return U(S(E, a), new Choice("MOVE"));
            }
        } else {
            if (a.getName().equals("RIGHT")) {
                return Choice.ANY_VALUE;
            }

        }
        return Choice.MAX_UTILITY;
    }


}
