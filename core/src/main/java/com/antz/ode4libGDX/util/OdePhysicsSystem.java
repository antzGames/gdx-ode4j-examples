package com.antz.ode4libGDX.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.github.antzGames.gdx.ode4j.Ode2GdxMathUtils;
import com.github.antzGames.gdx.ode4j.math.DMatrix3;
import com.github.antzGames.gdx.ode4j.ode.DBody;
import com.github.antzGames.gdx.ode4j.ode.DContact;
import com.github.antzGames.gdx.ode4j.ode.DContactBuffer;
import com.github.antzGames.gdx.ode4j.ode.DContactJoint;
import com.github.antzGames.gdx.ode4j.ode.DGeom;
import com.github.antzGames.gdx.ode4j.ode.DJoint;
import com.github.antzGames.gdx.ode4j.ode.DJointGroup;
import com.github.antzGames.gdx.ode4j.ode.DSpace;
import com.github.antzGames.gdx.ode4j.ode.DWorld;
import com.github.antzGames.gdx.ode4j.ode.OdeHelper;

import static com.github.antzGames.gdx.ode4j.ode.OdeConstants.dContactApprox1;
import static com.github.antzGames.gdx.ode4j.ode.OdeConstants.dContactBounce;
import static com.github.antzGames.gdx.ode4j.ode.OdeConstants.dContactSlip1;
import static com.github.antzGames.gdx.ode4j.ode.OdeConstants.dContactSlip2;
import static com.github.antzGames.gdx.ode4j.ode.OdeConstants.dContactSoftCFM;
import static com.github.antzGames.gdx.ode4j.ode.OdeConstants.dContactSoftERP;
import static com.github.antzGames.gdx.ode4j.ode.OdeHelper.areConnectedExcluding;

/**
 * Original code from: https://github.com/JamesTKhan/libgdx-bullet-tutorials
 * @author JamesTKhan
 * @version October 04, 2022
 *
 * modified to work on odej4 by:
 * Antz
 * April 27, 2023
 */
public class OdePhysicsSystem implements Disposable {

    // Ode4j objects
    public static DWorld world;
    public static DSpace space;
    public static DJointGroup contactgroup;
    public static Array<OdeEntity> obj; // used this variable so ode4j code migration is easier;

    // some constants
    public static final float DENSITY = 1.0f	;	// density of all objects
    public static final int GPB = 1;			    // maximum number of geometries per body
    public static final int MAX_CONTACTS = 32;	    // maximum number of contact points per body

    public OdePhysicsSystem() {
        initODE();
    }

    /**
     * Update physics world, should be called every frame
     * @param delta deltaTime since last frame
     */
    public void update(float delta) {
        // performs collision detection and physics simulation
        space.collide(null, nearCallback);
        world.quickStep(1/30f);

        // remove all contact joints
        contactgroup.empty();

        for (OdeEntity o: obj) {
            if (o.id.equals("player")) {
                //o.modelInstance.transform.set(Ode2GdxMathUtils.getGdxQuaternion(o.geom[0].getQuaternion()));
                o.modelInstance.transform.setTranslation(
                    (float) o.geom[0].getPosition().get0(),
                    (float) o.geom[0].getPosition().get1(),
                    (float) o.geom[0].getPosition().get2());
            } else if (o.id.equals("objects")){
                o.modelInstance.transform.set(Ode2GdxMathUtils.getGdxQuaternion(o.geom[0].getQuaternion()));
                o.modelInstance.transform.rotate(Vector3.X, 90); // for libGDX cylinders
                o.modelInstance.transform.setTranslation(
                    (float) o.geom[0].getPosition().get0(),
                    (float) o.geom[0].getPosition().get1(),
                    (float) o.geom[0].getPosition().get2());
            }
        }
    }

    @Override
    public void dispose() {
        // ode cleanup
        contactgroup.destroy();
        space.destroy();
        world.destroy();
        OdeHelper.closeODE();
    }


    private static DGeom.DNearCallback nearCallback = new DGeom.DNearCallback() {
        @Override
        public void call(Object data, DGeom o1, DGeom o2) {
            nearCallback( data, o1, o2);
        }
    };

    // this is called by dSpaceCollide when two objects in space are potentially colliding.
    private static void nearCallback (Object data, DGeom o1, DGeom o2)	{
        // exit without doing anything if the two bodies are connected by a joint
        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && areConnectedExcluding(b1,b2, DContactJoint.class)) return;

        DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);   // up to MAX_CONTACTS contacts per box-box
        for (int i=0; i<MAX_CONTACTS; i++) {
            DContact contact = contacts.get(i);
            contact.surface.mode = dContactBounce | dContactSlip1 | dContactSlip2 | dContactSoftERP | dContactSoftCFM | dContactApprox1;
            contact.surface.bounce = 0.5;
            contact.surface.bounce_vel = 0.1;
            contact.surface.mu = 0.4;
            //contact.surface.mu2 = 0.1;
            contact.surface.slip1 = 0.1;
            contact.surface.slip2 = 0.1;
            contact.surface.soft_erp = 0.4;
            contact.surface.soft_cfm = 0.01;
        }

        int numc = OdeHelper.collide(o1,o2,MAX_CONTACTS,contacts.getGeomBuffer());
        if (numc != 0) {
            DMatrix3 RI = new DMatrix3();
            RI.setIdentity();
            for (int i=0; i<numc; i++) {
                DJoint c = OdeHelper.createContactJoint(world,contactgroup,contacts.get(i));
                c.attach (b1,b2);
            }
        }
    }

    public void initODE() {
        // create world
        obj = new Array<>();
        OdeHelper.initODE2(0);
        world = OdeHelper.createWorld();
        world.setGravity(0, -1.62, 0); // moon gravity
        world.setCFM(1e-5);
        world.setERP(0.8);
        world.setAutoDisableFlag(true);
        world.setContactMaxCorrectingVel(0.1);
        world.setContactSurfaceLayer(0.001);
        world.setAutoDisableAverageSamplesCount(0);
        world.setQuickStepNumIterations(20);

        space = OdeHelper.createHashSpace(null);
        contactgroup = OdeHelper.createJointGroup();
    }
}
