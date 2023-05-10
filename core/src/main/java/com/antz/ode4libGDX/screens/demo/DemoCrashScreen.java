package com.antz.ode4libGDX.screens.demo;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.util.Ode2GdxMathUtils;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.mbrlabs.mundus.commons.Scene;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.runtime.Mundus;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DHinge2Joint;
import org.ode4j.ode.DSapSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.DGeom.DNearCallback;
import java.util.ArrayList;
import static org.ode4j.ode.OdeMath.*;

public class DemoCrashScreen implements Screen, InputProcessor {

    // My stuff
    private Mundus mundus;
    private Scene scene;
    enum GameState {
        LOADING,
        RENDER
    }
    private GameState gameState = GameState.LOADING;
    private SpriteBatch batch;
    private BitmapFont font = new BitmapFont();
    private String info;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private Model model;
    private ModelInstance cannonBallModelInstance;                              // the cannonball model
    private ModelInstance cannonBodyModelInstance;                              // cannon body model
    private ArrayList<ModelInstance> sphereModelInstances = new ArrayList<>();  // The models for sphere[] array (not used)
    private ArrayList<ModelInstance> boxesModelInstances = new ArrayList<>();   // The models for boxes[] array (not used)
    private ArrayList<ModelInstance> wallBoxModelInstances = new ArrayList<>(); // The models for wall_boxes[] array
    private InputMultiplexer inputMultiplexer;
    private FirstPersonCameraController controller;

    // **** ode4j DemoCrash original stuff below

    // some constants
    private static final float RADIUS = 0.5f;	            // canon ball radius
    private static final float WALLMASS = 1	;	            // wall box mass
    private static final float FMAX = 25;			        // max force
    private static final int   ITERS = 20;		            // number of iterations
    private static final float WBOXSIZE = 1.0f;		        // size of wall boxes
    private static final float WALLWIDTH = 12;		        // width of wall
    private static final float WALLHEIGHT = 10;		        // height of wall
    private static final float DISABLE_THRESHOLD = 0.008f;	// maximum velocity (squared) a body can have and be disabled
    private static final float DISABLE_STEPS = 10;	        // number of steps a box has to have been disable-able before it will be disabled
    private static final float CANNON_X = 100;		        // x position of cannon
    private static final float CANNON_Z = 90;	            // y position of cannon
    private static final float CANNON_BALL_MASS = 10;	    // mass of the cannon ball
    private static final float CANNON_BALL_RADIUS = 0.5f;   // cannon ball radius

    private static boolean WALL = true;
    private static boolean CANNON = true;

    /**
     libGDX user note: All the ode4j demos create very large arrays for joints, boxes, bodies etc.
     And then they have a integer keeping track of how many actual elements in the array are used.
     The original version had 100,000 elements per array.  I reduced it to 1000.
     I think this is a relic from the C language malloc to reserve memory?
     Either way this is a todo to switch to an Array
     There is also some weird code format and variable names.  I think its due to relics of ported C/C++ code.
    **/

    // dynamics and collision objects
    private static DWorld world;
    private static DSpace space;
    private static int bodies;
    private static DHinge2Joint[] joint = new DHinge2Joint[1000];
    private static int joints;
    private static DJointGroup contactgroup;

    //private static DGeom ground;
    private static DBox[] wall_boxes = new DBox[1000];
    private static DBody[] wall_bodies = new DBody[1000];
    private static int[] wb_stepsdis = new int[1000];

    private static DSphere cannon_ball_geom;
    private static DBody cannon_ball_body;

    private static int wb;
    private static boolean doFast;
    private static DBody b;
    private static DMass m;

    // things that the user controls
    private static float turn = 0, speed = 0;
    private static float cannon_angle = 0;
    private static float cannon_elevation = 0f;

    @Override
    public void show() {
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(this);
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        batch = new SpriteBatch();
        font.setColor(Color.BLUE);
        doFast = true;

        // From Mundus Example Project
        Mundus.Config config = new Mundus.Config();
        config.autoLoad = false; // Do not autoload, we want to queue custom assets
        config.asyncLoad = true; // Do asynchronous loading

        // Start asynchronous loading
        mundus = new Mundus(Gdx.files.internal("mundus"), config);
        try {
            mundus.getAssetManager().queueAssetsForLoading(true);
        } catch (MetaFileParseException e) {
            e.printStackTrace();
        }

        info = "DemoCrash\n\n" +
            "WASD to move camera, click-drag mouse to rotate camera.\n" +
            "LEFT-CURSOR to turn the cannon left.\n" +
            "RIGHT-CURSOR to turn the cannon right.\n" +
            "SPACE to shoot from the cannon.\n" +
            "R to reset simulation.\n" +
            "F1 to go to RagDoll Demo\n";

        System.out.println(info);

        bodies = 0;
        joints = 0;
        OdeHelper.initODE2(0);
        setupSimulation();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        switch (gameState) {
            case LOADING:
                continueLoading();
                break;
            case RENDER:
                draw();
                break;
        }
    }

    private void draw() {
        controller.update(); // camera controller
        scene.sceneGraph.update(); // update Mundus
        scene.render(); // render Mundus scene

        // 3D models drawing
        modelBatch.begin(scene.cam);
        simLoop(false, modelBatch); // so the original demo did rendering in the simulation loop, I did the same thing, but I think its not a good idea
        modelBatch.end();

        // 2D stuff for info text
        batch.begin();
        font.draw(batch, info + "Number of Boxes:" + wb + "\nFPS:" + Gdx.graphics.getFramesPerSecond(), 10, 180);
        batch.end();
    }

    // simulation loop
    private void simLoop (boolean pause, ModelBatch modelBatch)  {
        int i, j;
        if (!pause) {
            for (j = 0; j < joints; j++)  {
                DHinge2Joint j2 = joint[j];
                double curturn = j2.getAngle1 ();
                //dMessage (0,"curturn %e, turn %e, vel %e", curturn, turn, (turn-curturn)*1.0);
                j2.setParamVel((turn-curturn)*1.0);
                j2.setParamFMax(dInfinity);
                j2.setParamVel2(speed);
                j2.setParamFMax2(FMAX);
                j2.getBody(0).enable();
                j2.getBody(1).enable();
            }
            if (doFast) {
                space.collide (null,nearCallback);
                world.quickStep (0.05);
                contactgroup.empty ();
            } else {
                space.collide (null,nearCallback);
                world.step (0.05);
                contactgroup.empty ();
            }

            for (i = 0; i < wb; i++) {
                b = wall_boxes[i].getBody();
                if (b.isEnabled()) {
                    boolean disable = true;
                    DVector3C lvel = b.getLinearVel();
                    double lspeed = lvel.lengthSquared();
                    if (lspeed > DISABLE_THRESHOLD)
                        disable = false;
                    DVector3C avel = b.getAngularVel();
                    double aspeed = avel.lengthSquared();
                    if (aspeed > DISABLE_THRESHOLD)
                        disable = false;

                    if (disable)
                        wb_stepsdis[i]++;
                    else
                        wb_stepsdis[i] = 0;

                    if (wb_stepsdis[i] > DISABLE_STEPS) {
                        b.disable();
                    }
                }

                DVector3 ss = new DVector3();
                wall_boxes[i].getLengths (ss);

                wallBoxModelInstances.get(i).transform.set(Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion()));
                wallBoxModelInstances.get(i).transform.setTranslation((float)wall_boxes[i].getPosition().get0(), (float)wall_boxes[i].getPosition().get1(), (float)wall_boxes[i].getPosition().get2());
                modelBatch.render(wallBoxModelInstances.get(i));
                //dsDrawBox(wall_boxes[i].getPosition(), wall_boxes[i].getRotation(), ss);  //original draw call
            }
        } else {
            for (i = 0; i < wb; i++) {
                b = wall_boxes[i].getBody();

                DVector3 ss = new DVector3();
                wall_boxes[i].getLengths (ss);
                wallBoxModelInstances.get(i).transform.setTranslation((float)wall_boxes[i].getPosition().get0(), (float)wall_boxes[i].getPosition().get1(), (float)wall_boxes[i].getPosition().get2());
                modelBatch.render(wallBoxModelInstances.get(i));
                //dsDrawBox(wall_boxes[i].getPosition(), wall_boxes[i].getRotation(), ss);  //original draw call
            }
        }

        // draw the cannon
        DMatrix3 R2 = new DMatrix3(), R3 = new DMatrix3(), R4 = new DMatrix3();
        dRFromAxisAndAngle (R2,0,1,0,cannon_angle);
        dRFromAxisAndAngle (R3,1,0,0,cannon_elevation);
        dMultiply0 (R4,R2,R3);

        cannonBodyModelInstance.transform.setTranslation(CANNON_X,1,CANNON_Z);
        modelBatch.render(cannonBodyModelInstance);
        //dsDrawCylinder (cpos,R4,3f,0.5f); //original draw call

        // draw the cannon ball
        cannonBallModelInstance.transform.setTranslation((float)cannon_ball_body.getPosition().get0(),(float)cannon_ball_body.getPosition().get1(),(float)cannon_ball_body.getPosition().get2());
        modelBatch.render(cannonBallModelInstance);
        //dsDrawSphere (cannon_ball_body.getPosition(),cannon_ball_body.getRotation(), CANNON_BALL_RADIUS); //original draw call
    }

    private void setupSimulation() {
        cannon_angle = 0;
        cannon_elevation = 0;

        int i;
        for (i = 0; i < 1000; i++) wb_stepsdis[i] = 0;

        // recreate world
        world = OdeHelper.createWorld();
        space = OdeHelper.createSapSpace( null, DSapSpace.AXES.XZY );
        m = OdeHelper.createMass();

        contactgroup = OdeHelper.createJointGroup();
        world.setGravity (0,-1.5,0);
        world.setCFM (1e-5);
        world.setERP (0.8);
        world.setQuickStepNumIterations (ITERS);

        OdeHelper.createPlane (space,0,1,0,0);
        bodies = 0;
        joints = 0;
        wb = 0;

        if (WALL) {//#ifdef WALL
            boolean offset = false;
            for (double y = WBOXSIZE/2.0; y <= WALLHEIGHT; y+=WBOXSIZE) {
                offset = !offset;
                for (double x = (-WALLWIDTH+y)/2; x <= (WALLWIDTH-y)/2; x+=WBOXSIZE) {
                    wall_bodies[wb] = OdeHelper.createBody(world);
                    wall_bodies[wb].setPosition (100 + x,y,100 );
                    m.setBox (1,WBOXSIZE,WBOXSIZE,WBOXSIZE);
                    m.adjust (WALLMASS);
                    wall_bodies[wb].setMass(m);
                    wall_boxes[wb] = OdeHelper.createBox (space,WBOXSIZE,WBOXSIZE,WBOXSIZE);
                    wall_boxes[wb].setBody (wall_bodies[wb]);
                    //dBodyDisable(wall_bodies[wb++]);

                    // libGDX model code
                    model = modelBuilder.createBox(WBOXSIZE, WBOXSIZE, WBOXSIZE, GL20.GL_LINES,
                        new Material(ColorAttribute.createDiffuse(Color.RED)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    wallBoxModelInstances.add(new ModelInstance(model));
                    wb++;
                }
            }
            System.out.println("wall boxes: " + wb);
        }//#endif
        if (CANNON) {//#ifdef CANNON
            cannon_ball_body = OdeHelper.createBody (world);
            cannon_ball_geom = OdeHelper.createSphere (space,CANNON_BALL_RADIUS);
            m.setSphereTotal (CANNON_BALL_MASS,CANNON_BALL_RADIUS);

            // libGDX model code
            model = modelBuilder.createSphere(CANNON_BALL_RADIUS*2, CANNON_BALL_RADIUS*2, CANNON_BALL_RADIUS*2, 10, 10,
                new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            sphereModelInstances.add(new ModelInstance(model));

            cannon_ball_body.setMass (m);
            cannon_ball_geom.setBody (cannon_ball_body);
            cannon_ball_body.setPosition (CANNON_X, CANNON_BALL_RADIUS, CANNON_Z);

            // libGDX model code
            model = modelBuilder.createSphere(RADIUS, RADIUS, RADIUS, 10, 10,
                new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            cannonBallModelInstance = new ModelInstance(model);

            // libGDX model code
            model = modelBuilder.createCylinder(0.5f,3f,0.5f, 10,
                new Material(ColorAttribute.createDiffuse(Color.PINK)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            cannonBodyModelInstance = new ModelInstance(model);
            cannonBodyModelInstance.transform.translate(CANNON_X,1,CANNON_Z);
            cannonBodyModelInstance.transform.setToRotation(Vector3.X, 90);
        }//#endif
    }

    @Override
    public void resize(int width, int height) {
        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        mundus.dispose();
        scene.dispose();
        model.dispose();
        modelBatch.dispose();
        batch.dispose();
        font.dispose();

        contactgroup.destroy();
        space.destroy();
        world.destroy();
        OdeHelper.closeODE();
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.R: // reset simulation
                shutdownSimulation();
                setupSimulation();
                break;
            case Input.Keys.LEFT:
                cannonBodyModelInstance.transform.rotateRad(Vector3.Z, -0.1f);
                cannon_angle += 0.1;
                break;
            case Input.Keys.RIGHT:
                cannonBodyModelInstance.transform.rotateRad(Vector3.Z, 0.1f);
                cannon_angle -= 0.1;
                break;
            case Input.Keys.SPACE:
                fireCannon();
                break;
            case Input.Keys.F1:
                shutdownSimulation();
                Ode4libGDX.game.setScreen(new DemoRagDollScreen());
                break;
        }
        return false;
    }

    private void fireCannon() {
        DMatrix3 R2 = new DMatrix3(), R3 = new DMatrix3(), R4 = new DMatrix3();
        dRFromAxisAndAngle (R2,0,1,0,cannon_angle);
        dRFromAxisAndAngle (R3,1,0,0,cannon_elevation);
        dMultiply0 (R4,R2,R3);
        double[] cpos = {CANNON_X,1, CANNON_Z};
        for (int i=0; i<3; i++) cpos[i] += 3*R4.get(i, 2);//[i*4+2];
        cannon_ball_body.setPosition (cpos[0],cpos[1],cpos[2]);
        double force = 10;
        cannon_ball_body.setLinearVel (force*R4.get(0, 2),force*R4.get(1,2),force*R4.get(2,2));
        cannon_ball_body.setAngularVel (0,0,0);
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char cmd) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (Gdx.app.getType().equals(Application.ApplicationType.Android)) fireCannon();
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }


    private void continueLoading() {
        if (mundus.continueLoading()) {
            // Loading complete, load a scene.
            scene = mundus.loadScene("Main Scene.mundus");
            scene.cam.position.set(90, 10, 90);
            scene.cam.lookAt(100,0,100);
            scene.cam.up.set(Vector3.Y);
            scene.cam.update();;
            // setup input
            controller = new FirstPersonCameraController(scene.cam);
            controller.setVelocity(20f);
            inputMultiplexer.addProcessor(controller);
            Gdx.input.setInputProcessor(inputMultiplexer);
            // Update our game state
            gameState = GameState.RENDER;
        }
    }

    private DNearCallback nearCallback = new DNearCallback() {
        @Override
        public void call(Object data, DGeom o1, DGeom o2) {
            nearCallback(data, o1, o2);
        }
    };

    private void nearCallback (Object data, DGeom o1, DGeom o2) {
        int i,n;

        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && OdeHelper.areConnected(b1, b2))
            return;

        final int N = 4;
        DContactBuffer contacts = new DContactBuffer(N);
        n = OdeHelper.collide (o1,o2,N,contacts.getGeomBuffer());//[0].geom,sizeof(dContact));
        if (n > 0) {
            for (i=0; i<n; i++) {
                DContact contact = contacts.get(i);
                contact.surface.mode = dContactSlip1 | dContactSlip2 | dContactSoftERP | dContactSoftCFM | dContactApprox1;
                if ( o1 instanceof DSphere || o2 instanceof DSphere )
                    contact.surface.mu = 20;
                else
                    contact.surface.mu = 0.5;

                contact.surface.slip1 = 0.0;
                contact.surface.slip2 = 0.0;
                contact.surface.soft_erp = 0.8;
                contact.surface.soft_cfm = 0.01;
                DJoint c = OdeHelper.createContactJoint(world,contactgroup,contact);
                c.attach (o1.getBody(), o2.getBody());
            }
        }
    }

    private void shutdownSimulation() {
        // destroy world if it exists
        if (bodies!=0)  {
            contactgroup.destroy();
            space.destroy();
            world.destroy();
            bodies = 0;
        }
    }
}
