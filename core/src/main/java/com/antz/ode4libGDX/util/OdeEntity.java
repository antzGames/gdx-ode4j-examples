package com.antz.ode4libGDX.util;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

import org.ode4j.ode.DBody;
import org.ode4j.ode.DGeom;

/**
 * Original code from: https://github.com/JamesTKhan/libgdx-bullet-tutorials
 * @author JamesTKhan
 * @version October 04, 2022
 *
 * modified to work on odej4 by:
 * Antz
 * April 27, 2023
 */
public class OdeEntity {
    public String id;
    public DBody body;
    public DGeom[] geom;
    public ModelInstance modelInstance;

    public OdeEntity() {
        this.geom = new DGeom[OdePhysicsSystem.GPB];
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public DBody getBody() {
        return body;
    }

    public DGeom[] getGeom() {
        return geom;
    }
}
