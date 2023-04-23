package com.antz.ode4libGDX.util;

import com.badlogic.gdx.math.Quaternion;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;

public class Ode2GdxMathUtils {

    // ODE      z, x, y ,x
    // libGDX   x, y, z, w
    public static Quaternion getGdxQuaternion(DQuaternionC odeQ){
        Quaternion dgxQ = new Quaternion();
        return dgxQ.set((float)odeQ.get1(), (float)odeQ.get2(), (float)odeQ.get3(), (float) odeQ.get0());
    }


}
