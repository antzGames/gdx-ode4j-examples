package com.antz.ode4libGDX.util;

import com.antz.ode4libGDX.screens.DemoMundusHeightFieldScreen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import org.ode4j.math.DMatrix3;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import static org.ode4j.ode.OdeMath.*;
import static org.ode4j.ode.OdeConstants.dContactSoftCFM;
import static org.ode4j.ode.OdeConstants.dInfinity;
import static org.ode4j.ode.OdeHelper.areConnectedExcluding;

/**
 * @author JamesTKhan
 * @version September 29, 2022
 */
public class OdePhysicsSystem implements Disposable {

    // Ode objects
    public static DWorld world;
    public static DSpace space;
    public static DJointGroup contactgroup;
    public static Array<OdeEntity> obj = new Array<>(); // use this variable so ode code migration is easier;

    // some constants
    public static final float DENSITY = 1.0f	;	// density of all objects
    public static final int GPB = 3;			    // maximum number of geometries per body
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
        //world.step(0.05);

        // remove all contact joints
        contactgroup.empty();

        for (OdeEntity o: obj) {
            if (o.id.equals("player")) {
                //o.modelInstance.transform.set(Ode2GdxMathUtils.getGdxQuaternion(o.geom[0].getQuaternion()));
                o.modelInstance.transform.setTranslation(
                (float) o.geom[0].getPosition().get0(),
                (float) o.geom[0].getPosition().get1(),
                (float) o.geom[0].getPosition().get2());
            } else if (o.id.equals("scene")){

            }
        }
    }

    /**
     * Debug draw the physics world
     * @param camera camera to render to
     */
    public void render(Camera camera) {
//        debugDrawer.begin(camera);
//        debugDrawer.drawLine(lastRayFrom, lastRayTo, rayColor);
//        dynamicsWorld.debugDrawWorld();
//        debugDrawer.end();
    }


    @Override
    public void dispose() {
//        collisionConfig.dispose();
//        dispatcher.dispose();
//        broadphase.dispose();
//        constraintSolver.dispose();
//        dynamicsWorld.dispose();
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
            contact.surface.mode = dContactBounce;
            contact.surface.mu = dInfinity;
            contact.surface.mu2 = 0;
            contact.surface.bounce = 0.1;
            contact.surface.bounce_vel = 0.1;
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

    private void initODE() {
        // create world
        OdeHelper.initODE2(0);
        world = OdeHelper.createWorld();
        world.setGravity(0, -9.8, 0);
        world.setCFM(1e-5);
        //world.setERP(0.8);
        world.setAutoDisableFlag(true);
        world.setContactMaxCorrectingVel(0.1);
        world.setContactSurfaceLayer(0.001);
        world.setAutoDisableAverageSamplesCount(0);
        //world.setQuickStepNumIterations(20);

        space = OdeHelper.createHashSpace(null);
        contactgroup = OdeHelper.createJointGroup();
    }
}
