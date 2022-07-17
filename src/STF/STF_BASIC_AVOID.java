/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package STF;

import Environment.Environment;
import ai.Choice;

public class STF_BASIC_AVOID extends STF_DIRECT_DRIVE {

    public double goAvoid(Environment E, Choice a) {
        if (a.getName().equals("RIGHT")) {
            a.setAnnotation(this.myMethod());
            return Choice.ANY_VALUE;
        }
        return Choice.MAX_UTILITY;
    }

    @Override
    protected double U(Environment E, Choice a) {
        if (E.getDistance() > 0
                && E.getGPS().getZ() < E.getMaxlevel()) {
//                && E.getGPS().getZ() < Math.min(E.getVisualFront() + 15, E.getMaxlevel())) {
            return goTakeOff(E, a);
        } else if (E.getDistance() == 0 && E.getGround() > 0) {
            return goLanding(E, a);
        } else if (!E.isFreeFront()) {
            return goAvoid(E, a);
        } else {
            return goAhead(E, a);
        }
    }

}
