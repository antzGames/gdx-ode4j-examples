package com.antz.ode4libGDX.util;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

import org.ode4j.ode.DBody;
import org.ode4j.ode.DGeom;

/**
 * @author JamesTKhan
 * @version October 01, 2022
 */
public class OdeEntity {
    public String id;
    public DBody body;
    public DGeom[] geom;
    public ModelInstance modelInstance;
    public static final int GPB = 3; // max number of geometries

    public OdeEntity() {
        this.geom = new DGeom[GPB];
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
