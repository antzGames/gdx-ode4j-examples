package com.antz.ode4libGDX.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
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
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DFixedJoint;
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

/** First screen of the application. Displayed after the application is created. */
public class CrashTest implements Screen, InputProcessor {

    private Mundus mundus;
    private Scene scene;
    enum GameState {
        LOADING,
        PLAYING
    }
    private GameState gameState = GameState.LOADING;

    public ModelBatch modelBatch;
    public ModelBuilder modelBuilder;
    public Model model;
    public ModelInstance cannonBallModelInstance;
    public ModelInstance cannonBodyModelInstance;
    public ArrayList<ModelInstance> sphereModelInstances = new ArrayList<>();
    public ArrayList<ModelInstance> boxesModelInstances = new ArrayList<>();
    public ArrayList<ModelInstance> wallBoxModelInstances = new ArrayList<>();
    public InputMultiplexer inputMultiplexer;
    private FirstPersonCameraController controller;
    private Color color = new Color();

    // some constants
    private static final float LENGTH = 3.5f;		// chassis length
    private static final float WIDTH = 2.5f;		// chassis width
    private static final float HEIGHT = 1.0f;		// chassis height
    private static final float RADIUS = 0.5f;	// wheel radius
    private static final float STARTY = 1.0f;	// starting height of chassis
    private static final float CMASS = 1;			// chassis mass
    private static final float WMASS = 1;			// wheel mass
    private static final float COMOFFSET = -5;		// center of mass offset
    private static final float WALLMASS = 1	;	// wall box mass
    private static final float BALLMASS = 1;		// ball mass
    private static final float FMAX = 25;			// car engine fmax
    private static final float ROWS = 1	;		// rows of cars
    private static final float COLS = 1	;		// columns of cars
    private static final int ITERS = 20;		// number of iterations
    private static final float WBOXSIZE = 1.0f;		// size of wall boxes
    private static final float WALLWIDTH = 24;		// width of wall
    private static final float WALLHEIGHT = 20;		// height of wall
    private static final float DISABLE_THRESHOLD = 0.008f;	// maximum velocity (squared) a body can have and be disabled
    private static final float DISABLE_STEPS = 10;	// number of steps a box has to have been disable-able before it will be disabled
    private static final float CANNON_X = 100;		// x position of cannon
    private static final float CANNON_Z = 90;	// y position of cannon
    private static final float CANNON_BALL_MASS = 10;	// mass of the cannon ball
    private static final float CANNON_BALL_RADIUS = 0.5f;

    //	private static boolean BOX = false;
//	private static boolean CARS = true;
//	private static boolean WALL = true;
//	private static boolean BALLS = false;
//	private static boolean BALLSTACK = false;
//	private static boolean ONEBALL = false;
//	private static boolean CENTIPEDE = false;
//	private static boolean CANNON = true;
    private static boolean BOX = false;
    private static boolean CARS = false;
    private static boolean WALL = true;
    private static boolean BALLS = false;
    private static boolean BALLSTACK = false;
    private static boolean ONEBALL = false;
    private static boolean CENTIPEDE = false;
    private static boolean CANNON = true;

    // dynamics and collision objects (chassis, 3 wheels, environment)
    private static DWorld world;
    private static DSpace space;
    private static DBody[] body=new DBody[10000];
    private static int bodies;
    private static DHinge2Joint[] joint=new DHinge2Joint[100000];
    private static int joints;
    private static DJointGroup contactgroup;
    //private static DGeom ground;
    private static DBox[] box=new DBox[10000];
    private static int boxes;
    private static DSphere[] sphere=new DSphere[10000];
    private static int spheres;
    private static DBox[] wall_boxes=new DBox[10000];
    private static DBody[] wall_bodies=new DBody[10000];
    private static DSphere cannon_ball_geom;
    private static DBody cannon_ball_body;
    private static int[] wb_stepsdis=new int[10000];
    private static int wb;
    private static boolean doFast;
    private static DBody b;
    private static DMass m;

    // things that the user controls
    private static float turn = 0, speed = 0;	// user commands
    private static float cannon_angle=0;
    private static float cannon_elevation=0f;

    @Override
    public void show() {
        inputMultiplexer = new InputMultiplexer();
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        doFast = true;

        // Mundus
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

        System.out.println("Press:\t'+' to increase speed.\n" +
            "\t'-' to decrease speed.\n" +
            "\t',' to steer left.\n" +
            "\t'.' to steer right.\n" +
            "\t' ' to reset speed and steering.\n" +
            "\t'[' to turn the cannon left.\n" +
            "\t']' to turn the cannon right.\n" +
            "\t'1' to raise the cannon.\n" +
            "\t'2' to lower the cannon.\n" +
            "\t'x' to shoot from the cannon.\n" +
            "\t'f' to toggle fast step mode.\n" +
            "\t'r' to reset simulation.\n");
        inputMultiplexer.addProcessor(this);

        bodies = 0;
        joints = 0;
        boxes = 0;
        spheres = 0;
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
            case PLAYING:
                draw();
                break;
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        controller.update();
        scene.sceneGraph.update();
        scene.render();

        modelBatch.begin(scene.cam);
        simLoop(false, modelBatch);
        modelBatch.end();
    }

    // simulation loop
    private void simLoop (boolean pause, ModelBatch modelBatch)  {
        int i, j;

        //dsSetTexture (DS_TEXTURE_NUMBER.DS_WOOD);

        if (!pause) {
            if (BOX) {
                //dBodyAddForce(body[bodies-1],lspeed,0,0);
                body[bodies-1].addForce(speed,0,0);
            }

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
                        color.set(0.5f,0.5f,1,1);
                    }
                    else
                        color.set(1,1,1,1);

                } else
                    color.set(0.4f,0.4f,0.4f,1);

                DVector3 ss = new DVector3();
                wall_boxes[i].getLengths (ss);
                //dsDrawBox(wall_boxes[i].getPosition(), wall_boxes[i].getRotation(), ss);

                wallBoxModelInstances.get(i).model.materials.get(0).set(ColorAttribute.createDiffuse(color));
                wallBoxModelInstances.get(i).transform.setTranslation(new Vector3((float)wall_boxes[i].getPosition().get0(), (float)wall_boxes[i].getPosition().get1(), (float)wall_boxes[i].getPosition().get2()));
                modelBatch.render(wallBoxModelInstances.get(i));
            }
        } else {
            for (i = 0; i < wb; i++) {
                b = wall_boxes[i].getBody();
                if (b.isEnabled())
                    color.set(1,1,1,1);
                else
                    color.set(0.4f,0.4f,0.4f,1);

                wallBoxModelInstances.get(i).model.materials.get(0).set(ColorAttribute.createDiffuse(color));
                DVector3 ss = new DVector3();
                wall_boxes[i].getLengths (ss);
                wallBoxModelInstances.get(i).transform.setTranslation(new Vector3((float)wall_boxes[i].getPosition().get0(), (float)wall_boxes[i].getPosition().get1(), (float)wall_boxes[i].getPosition().get2()));
                modelBatch.render(wallBoxModelInstances.get(i));
                //dsDrawBox(wall_boxes[i].getPosition(), wall_boxes[i].getRotation(), ss);
            }
        }

        color.set(0,1,1,1);
        DVector3 sides = new DVector3(LENGTH,WIDTH,HEIGHT);
        for (i = 0; i < boxes; i++) {
            boxesModelInstances.get(i).transform.setTranslation(new Vector3((float)box[i].getPosition().get0(), (float)box[i].getPosition().get1(), (float)box[i].getPosition().get2()));
            modelBatch.render(boxesModelInstances.get(i));
            //dsDrawBox (box[i].getPosition(),box[i].getRotation(),sides);
        }

        color.set(1,1,1,1);

        for (i=0; i< spheres; i++){
            sphereModelInstances.get(i).transform.setTranslation(new Vector3((float)sphere[i].getPosition().get0(), (float)sphere[i].getPosition().get1(), (float)sphere[i].getPosition().get2()));
            modelBatch.render(sphereModelInstances.get(i));
            //dsDrawSphere (sphere[i].getPosition(), sphere[i].getRotation(),RADIUS);
        }

        // draw the cannon
        color.set(1,1,0,1);
        DMatrix3 R2 = new DMatrix3(), R3 = new DMatrix3(), R4 = new DMatrix3();
        dRFromAxisAndAngle (R2,0,1,0,cannon_angle);
        dRFromAxisAndAngle (R3,0,0,1,cannon_elevation);
        dMultiply0 (R4,R2,R3);
        DVector3 cpos = new DVector3(CANNON_X,1,CANNON_Z);
        DVector3 csides = new DVector3(2,2,2);
        //dsDrawBox (cpos,R2,csides);

        for (i=0; i<3; i++) cpos.add(i,  1.5*R4.get(i, 2));//[i*4+2]);

        cannonBodyModelInstance.transform.setTranslation(CANNON_X,1,CANNON_Z);
        modelBatch.render(cannonBodyModelInstance);
        //dsDrawCylinder (cpos,R4,3f,0.5f);

        // draw the cannon ball
        cannonBallModelInstance.transform.setTranslation(new Vector3((float)cannon_ball_body.getPosition().get0(),(float)cannon_ball_body.getPosition().get1(),(float)cannon_ball_body.getPosition().get2()));
        modelBatch.render(cannonBallModelInstance);
        //dsDrawSphere (cannon_ball_body.getPosition(),cannon_ball_body.getRotation(), CANNON_BALL_RADIUS);
    }

    private void setupSimulation() {
        int i;
        for (i = 0; i < 1000; i++) wb_stepsdis[i] = 0;

        // recreate world
        world = OdeHelper.createWorld();

        //  space = dHashSpaceCreate( null );
        //	space = dSimpleSpaceCreate( null );
        space = OdeHelper.createSapSpace( null, DSapSpace.AXES.XZY );

        m = OdeHelper.createMass();

        contactgroup = OdeHelper.createJointGroup();
        world.setGravity (0,-1.5,0);
        world.setCFM (1e-5);
        world.setERP (0.8);
        world.setQuickStepNumIterations (ITERS);

        //TODO
//	    DThreadingImplementation threading = OdeHelper.allocateMultiThreaded();
//	    DThreadingThreadPool pool = OdeHelper.allocateThreadPool(4, 0, /*dAllocateFlagBasicData,*/ null);
//	    pool.serveMultiThreadedImplementation(threading);
//	    // dWorldSetStepIslandsProcessingMaxThreadCount(world, 1);
//	    world.setStepThreadingImplementation(threading.dThreadingImplementationGetFunctions(), threading);

        OdeHelper.createPlane (space,0,1,0,0);
        bodies = 0;
        joints = 0;
        boxes = 0;
        spheres = 0;
        wb = 0;
        IrContainer ir = new IrContainer();
        if (CARS) {//#ifdef CARS
            for (double x = 0.0; x < COLS*(LENGTH+RADIUS); x += LENGTH+RADIUS)
                for (double z = -((ROWS-1)*(WIDTH/2+RADIUS)); z <= ((ROWS-1)*(WIDTH/2+RADIUS)); z += WIDTH+RADIUS*2)
                    makeCar(x, z, ir);
            bodies = ir.bodyIr;
            joints = ir.jointIr;
            boxes = ir.boxIr;
            spheres = ir.sphereIr;
        }//#endif
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

                    model = modelBuilder.createBox(WBOXSIZE, WBOXSIZE, WBOXSIZE, GL20.GL_LINES,
                        new Material(ColorAttribute.createDiffuse(Color.RED)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    wallBoxModelInstances.add(new ModelInstance(model));
                    wb++;
                }
            }
            System.out.println("wall boxes: " + wb);
        }//#endif
        if (BALLS) {//#ifdef BALLS
            for (double x = -7; x <= -4; x+=1)
                for (double y = -1.5; y <= 1.5; y+=1)
                    for (double z = 1; z <= 4; z+=1) {
                        b = OdeHelper.createBody (world);
                        b.setPosition (x*RADIUS*2,y*RADIUS*2, z*RADIUS*2);
                        m.setSphere (1,RADIUS);
                        m.adjust (BALLMASS);
                        b.setMass (m);
                        sphere[spheres] = OdeHelper.createSphere (space,RADIUS);
                        sphere[spheres++].setBody (b);
                        model = modelBuilder.createSphere(RADIUS*2, RADIUS*2, RADIUS*2, 10, 10,
                            new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                        sphereModelInstances.add(new ModelInstance(model));
                    }
        }//#endif
        if (ONEBALL) {//#ifdef ONEBALL
            b = OdeHelper.createBody (world);
            b.setPosition (100 - 10,0,100-10);
            m.setSphere (1,RADIUS);
            m.adjust (1);
            b.setMass (m);
            sphere[spheres] = OdeHelper.createSphere (space,RADIUS);
            sphere[spheres++].setBody (b);

        }//#endif
        if (BALLSTACK) {//#ifdef BALLSTACK
            for (double z = 1; z <= 6; z+=1)
            {
                b = OdeHelper.createBody (world);
                b.setPosition (0,0,z*RADIUS*2);
                m.setSphere (1,RADIUS);
                m.adjust (0.1);
                b.setMass (m);
                sphere[spheres] = OdeHelper.createSphere (space,RADIUS);
                sphere[spheres++].setBody (b);

                model = modelBuilder.createSphere(RADIUS*2, RADIUS*2, RADIUS*2, 10, 10,
                    new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                sphereModelInstances.add(new ModelInstance(model));

            }
        }//#endif
        if (CENTIPEDE) {//#ifdef CENTIPEDE
            DBody lastb = null;
            for (double y = 0; y < 10*LENGTH; y+=LENGTH+0.1)
            {
                // chassis body

                b = body[bodies] = OdeHelper.createBody (world);
                body[bodies].setPosition (-15,STARTY,y);
                m.setBox (1,WIDTH,LENGTH,HEIGHT);
                m.adjust (CMASS);
                body[bodies].setMass (m);
                box[boxes] = OdeHelper.createBox (space,WIDTH,LENGTH,HEIGHT);
                box[boxes++].setBody (body[bodies++]);

                model = modelBuilder.createBox(WIDTH, HEIGHT, LENGTH,
                    new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                boxesModelInstances.add(new ModelInstance(model));

                for (double x = -17; x > -20; x-=RADIUS*2) {
                    body[bodies] = OdeHelper.createBody (world);
                    body[bodies].setPosition(x, STARTY, y);
                    m.setSphere(1, RADIUS);
                    m.adjust(WMASS);
                    body[bodies].setMass(m);
                    sphere[spheres] = OdeHelper.createSphere (space, RADIUS);
                    sphere[spheres++].setBody (body[bodies]);

                    model = modelBuilder.createSphere(RADIUS*2, RADIUS*2, RADIUS*2, 10, 10,
                        new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    sphereModelInstances.add(new ModelInstance(model));

                    joint[joints] = OdeHelper.createHinge2Joint (world,null);
                    if (x == -17)
                        joint[joints].attach (b,body[bodies]);
                    else
                        joint[joints].attach (body[bodies-2],body[bodies]);
                    DVector3C a = body[bodies++].getPosition ();
                    DHinge2Joint j = joint[joints++];
                    j.setAnchor (a);
                    j.setAxis1 (0,0,1);
                    j.setAxis2 (1,0,0);
                    j.setParamSuspensionERP (1.0);
                    j.setParamSuspensionCFM (1e-5);
                    j.setParamLoStop (0);
                    j.setParamHiStop (0);
                    j.setParamVel2 (-10.0);
                    j.setParamFMax2 (FMAX);

                    body[bodies] = OdeHelper.createBody (world);
                    body[bodies].setPosition(-30 - x, STARTY,y);
                    m.setSphere(1, RADIUS);
                    m.adjust(WMASS);
                    body[bodies].setMass(m);
                    sphere[spheres] = OdeHelper.createSphere (space, RADIUS);
                    sphere[spheres++].setBody (body[bodies]);

                    model = modelBuilder.createSphere(RADIUS*2, RADIUS*2, RADIUS*2, 10, 10,
                        new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    sphereModelInstances.add(new ModelInstance(model));


                    joint[joints] = OdeHelper.createHinge2Joint (world,null);
                    if (x == -17)
                        joint[joints].attach (b,body[bodies]);
                    else
                        joint[joints].attach (body[bodies-2],body[bodies]);
                    DVector3C b = body[bodies++].getPosition ();
                    j = joint[joints++];
                    j.setAnchor (b);
                    j.setAxis1 (0,0,1);
                    j.setAxis2 (1,0,0);
                    j.setParamSuspensionERP (1.0);
                    j.setParamSuspensionCFM (1e-5);
                    j.setParamLoStop (0);
                    j.setParamHiStop (0);
                    j.setParamVel2 (10.0);
                    j.setParamFMax2 (FMAX);
                }
                if (lastb!=null)
                {
                    DFixedJoint j = OdeHelper.createFixedJoint(world,null);
                    j.attach (b, lastb);
                    j.setFixed();
                }
                lastb = b;
            }
        }//#endif
        if (BOX) {//#ifdef BOX
            body[bodies] = OdeHelper.createBody (world);
            body[bodies].setPosition (0,0,HEIGHT/2);
            m.setBox (1,LENGTH,WIDTH,HEIGHT);
            m.adjust (1);
            body[bodies].setMass (m);
            box[boxes] = OdeHelper.createBox (space,LENGTH,WIDTH,HEIGHT);
            box[boxes++].setBody (body[bodies++]);

            model = modelBuilder.createBox(WIDTH, HEIGHT, LENGTH,
                new Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            boxesModelInstances.add(new ModelInstance(model));

        }//#endif
        if (CANNON) {//#ifdef CANNON
            cannon_ball_body = OdeHelper.createBody (world);
            cannon_ball_geom = OdeHelper.createSphere (space,CANNON_BALL_RADIUS);
            m.setSphereTotal (CANNON_BALL_MASS,CANNON_BALL_RADIUS);

            model = modelBuilder.createSphere(CANNON_BALL_RADIUS*2, CANNON_BALL_RADIUS*2, CANNON_BALL_RADIUS*2, 10, 10,
                new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            sphereModelInstances.add(new ModelInstance(model));

            cannon_ball_body.setMass (m);
            cannon_ball_geom.setBody (cannon_ball_body);
            cannon_ball_body.setPosition (CANNON_X, CANNON_BALL_RADIUS, CANNON_Z);

            model = modelBuilder.createSphere(RADIUS, RADIUS, RADIUS, 10, 10,
                new Material(ColorAttribute.createDiffuse(Color.ORANGE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            cannonBallModelInstance = new ModelInstance(model);

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
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char cmd) {
        switch (cmd) {
            case '=':
                speed += 0.3;
                break;
            case '-':
                speed -= 0.3;
                break;
            case ',':
                turn += 0.1;
                if (turn > 0.3)
                    turn = 0.3f;
                break;
            case '.':
                turn -= 0.1;
                if (turn < -0.3)
                    turn = -0.3f;
                break;
            case ' ':
                speed = 0;
                turn = 0;
                break;
            case 'f': case 'F':
                doFast = !doFast;
                break;
            case 'r': case 'R':
                shutdownSimulation();
                setupSimulation();
                break;
            case '[':
                cannonBodyModelInstance.transform.rotateRad(Vector3.Z, -0.1f);
                cannon_angle += 0.1;
                break;
            case ']':
                cannonBodyModelInstance.transform.rotateRad(Vector3.Z, 0.1f);
                cannon_angle -= 0.1;
                break;
            case '1':
                cannon_elevation += 0.1;
                cannonBodyModelInstance.transform.rotateRad(Vector3.X, -0.1f);
                break;
            case '2':
                cannonBodyModelInstance.transform.rotateRad(Vector3.X, 0.1f);
                cannon_elevation -= 0.1;
                break;
            case 'x': case 'X': {
                DMatrix3 R2 = new DMatrix3(), R3 = new DMatrix3(), R4 = new DMatrix3();
                dRFromAxisAndAngle (R2,0,0,1,cannon_angle);
                dRFromAxisAndAngle (R3,0,1,0,cannon_elevation);
                dMultiply0 (R4,R2,R3);
                double[] cpos = {CANNON_X,1, CANNON_Z};
                for (int i=0; i<3; i++) cpos[i] += 3*R4.get(i, 2);//[i*4+2];
                cannon_ball_body.setPosition (cpos[0],cpos[1],cpos[2]);
                double force = 10;
                cannon_ball_body.setLinearVel (force*R4.get(0, 2),force*R4.get(1,2),force*R4.get(2,2));
                cannon_ball_body.setAngularVel (0,0,0);
                break;
            }
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
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
            scene.cam.position.set(50, 10, 50);
            scene.cam.lookAt(100,0,100);
            // setup input
            controller = new FirstPersonCameraController(scene.cam);
            controller.setVelocity(20f);
            inputMultiplexer.addProcessor(controller);
            Gdx.input.setInputProcessor(inputMultiplexer);
            // Update our game state
            gameState = GameState.PLAYING;
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

    private static class IrContainer {
        int bodyIr, jointIr, boxIr, sphereIr;
    }

    //private void makeCar(double x, double y, int &bodyI, int &jointI, int &boxI, int &sphereI)
    private void makeCar(double x, double z, IrContainer ir) {

        x = 80; z = 80;

        final int bodyI = ir.bodyIr;
        final int jointI = ir.jointIr;
        final int boxI = ir.boxIr;
        final int sphereI = ir.sphereIr;
        int i;
        DMass m = OdeHelper.createMass();

        // chassis body
        body[bodyI] = OdeHelper.createBody(world);
        body[bodyI].setPosition (x ,STARTY, z);
        m.setBox (1,LENGTH, WIDTH, HEIGHT);
        m.adjust (CMASS/2.0);
        body[bodyI].setMass (m);
        box[boxI] = OdeHelper.createBox (space,LENGTH,WIDTH, HEIGHT);
        box[boxI].setBody (body[bodyI]);

        model = modelBuilder.createBox(LENGTH, HEIGHT, WIDTH,
            new Material(ColorAttribute.createDiffuse(Color.GREEN)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        boxesModelInstances.add(new ModelInstance(model));

        // wheel bodies
        for (i=1; i<=4; i++) {
            body[bodyI+i] = OdeHelper.createBody(world);
            DQuaternion q = new DQuaternion();
            dQFromAxisAndAngle (q,1,0,0,M_PI*0.5);
            body[bodyI+i].setQuaternion (q);
            m.setSphere (1,RADIUS);
            m.adjust (WMASS);
            body[bodyI+i].setMass (m);
            sphere[sphereI+i-1] = OdeHelper.createSphere (space,RADIUS);
            sphere[sphereI+i-1].setBody (body[bodyI+i]);

            model = modelBuilder.createSphere(RADIUS*2, RADIUS*2, RADIUS*2, 10, 10,
                new Material(ColorAttribute.createDiffuse(Color.PURPLE)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            sphereModelInstances.add(new ModelInstance(model));
        }
        body[bodyI+1].setPosition (x+0.4*LENGTH-0.5*RADIUS,STARTY-HEIGHT*0.5,z+WIDTH*0.5);
        body[bodyI+2].setPosition (x+0.4*LENGTH-0.5*RADIUS,STARTY-HEIGHT*0.5,z-WIDTH*0.5);
        body[bodyI+3].setPosition (x-0.4*LENGTH+0.5*RADIUS,STARTY-HEIGHT*0.5,z+WIDTH*0.5);
        body[bodyI+4].setPosition (x-0.4*LENGTH+0.5*RADIUS,STARTY-HEIGHT*0.5,z-WIDTH*0.5);

        // front and back wheel hinges
        for (i=0; i<4; i++) {
            joint[jointI+i] = OdeHelper.createHinge2Joint (world,null);
            DHinge2Joint j = joint[jointI+i];
            j.attach (body[bodyI],body[bodyI+i+1]);
            DVector3C a = body[bodyI+i+1].getPosition ();
            j.setAnchor(a);
            j.setAxis1 (0,0,(i<2 ? 1 : -1));
            j.setAxis2 (0,1,0);
            j.setParamSuspensionERP (0.8);
            j.setParamSuspensionCFM (1e-5);
            j.setParamVel2 (0);
            j.setParamFMax2 (FMAX);
        }

        //center of mass offset body. (hang another copy of the body COMOFFSET units below it by a fixed joint)
        DBody b = OdeHelper.createBody (world);
        b.setPosition (x,STARTY+COMOFFSET, z);
        m.setBox (1,LENGTH,WIDTH, HEIGHT);
        m.adjust (CMASS/2.0);
        b.setMass (m);
        DFixedJoint j = OdeHelper.createFixedJoint(world, null);
        j.attach(body[bodyI], b);
        j.setFixed();
        //box[boxI+1] = dCreateBox(space,LENGTH,WIDTH,HEIGHT);
        //dGeomSetBody (box[boxI+1],b);

        ir.bodyIr += 5;
        ir.jointIr += 4;
        ir.boxIr += 1;
        ir.sphereIr += 4;
    }

    private void shutdownSimulation() {
        // destroy world if it exists
        if (bodies!=0)  {
            //TODO
//		    threading.shutdownProcessing();//dThreadingImplementationShutdownProcessing(threading);
//		    pool.freeThreadPool();
//		    world.setStepThreadingImplementation(null, null);
//		    threading.free();

            contactgroup.destroy();
            space.destroy();
            world.destroy();

            bodies = 0;
        }
    }

}
