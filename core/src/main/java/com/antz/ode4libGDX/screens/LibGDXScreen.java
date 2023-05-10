package com.antz.ode4libGDX.screens;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.screens.demo.DemoCrashScreen;
import com.antz.ode4libGDX.screens.demo.DemoDynamicCharacterScreen;
import com.antz.ode4libGDX.util.Ode2GdxMathUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.mbrlabs.mundus.commons.Scene;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.runtime.Mundus;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DHinge2Joint;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSapSpace;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import java.util.ArrayList;
import static org.ode4j.ode.OdeMath.dContactApprox1;
import static org.ode4j.ode.OdeMath.dContactSlip1;
import static org.ode4j.ode.OdeMath.dContactSlip2;
import static org.ode4j.ode.OdeMath.dContactSoftCFM;
import static org.ode4j.ode.OdeMath.dContactSoftERP;
import static org.ode4j.ode.OdeMath.dMultiply0;
import static org.ode4j.ode.OdeMath.dRFromAxisAndAngle;

public class LibGDXScreen implements Screen, InputProcessor {

    // My stuff
    private Mundus mundus;
    private Scene scene;
    enum GameState {
        LOADING,
        RENDER
    }

    private GameState gameState = GameState.LOADING;
    private TextureRegion texture;
    private TextureRegion[] boxTextures;
    private int boxTextureIndex;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private MeshPartBuilder meshBuilder;
    private Model model;
    private ModelInstance cannonBallModelInstance;                              // the cannonball model
    private ModelInstance ground;                                               // ground model
    private ArrayList<ModelInstance> wallBoxModelInstances = new ArrayList<>(); // The models for wall_boxes[] array
    private ArrayList<GameObject> wallBoxGameObjects = new ArrayList<>();
    private InputMultiplexer inputMultiplexer;
    private FirstPersonCameraController controller;

    // some constants
    private static final float RADIUS = 0.5f;	            // canon ball radius
    private static final float WALLMASS = 1	;	            // wall box mass
    private static final int   ITERS = 20;		            // number of iterations
    private static final float WBOXSIZE = 1.0f;		        // size of wall boxes
    private static final int   WALLWIDTH = 10;		        // width of wall
    private static final int   WALLHEIGHT = 10;		        // height of wall
    private static final float DISABLE_THRESHOLD = 0.008f;	// maximum velocity (squared) a body can have and be disabled
    private static final float DISABLE_STEPS = 10;	        // number of steps a box has to have been disable-able before it will be disabled
    private static final float CANNON_X = 100;		        // x position of cannon
    private static final float CANNON_Z = 90;	            // y position of cannon
    private static final float CANNON_BALL_MASS = 10;	    // mass of the cannon ball
    private static final float CANNON_BALL_RADIUS = 0.5f;   // cannon ball radius

    // dynamics and collision objects
    private static DWorld world;
    private static DSpace space;
    private static int bodies;
    private static DHinge2Joint[] joint = new DHinge2Joint[1000];
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

    private static float cannon_angle = 0;
    private static float cannon_elevation = 0f;

    @Override
    public void show() {
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());

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
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        meshBuilder = new MeshBuilder();
        texture = new TextureRegion(new Texture(Gdx.files.internal("graphics/img.png")));

        doFast = true;
        bodies = 0;
        OdeHelper.initODE2(0);
        initBoxTextures();
        setupSimulation();

        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(inputMultiplexer);
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
                simLoop();
                draw();
                break;
        }
    }

    private void draw() {
        controller.update();
        scene.sceneGraph.update(); // update Mundus
        scene.render(); // render Mundus scene

        modelBatch.begin(scene.cam);
        for (int i = 0; i < wb; i++) {
            modelBatch.render(wallBoxModelInstances.get(i));
        }
        modelBatch.render(cannonBallModelInstance);
        //modelBatch.render(ground);
        modelBatch.end();
    }

    // simulation loop
    private void simLoop ()  {
        if (doFast) {
            space.collide (null,nearCallback);
            world.quickStep (0.05);
            contactgroup.empty ();
        } else {
            space.collide (null,nearCallback);
            world.step (0.05);
            contactgroup.empty ();
        }

        for (int i = 0; i < wb; i++) {
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
            wall_boxes[i].getLengths(ss);
            wallBoxModelInstances.get(i).transform.set(Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion()));
            wallBoxModelInstances.get(i).transform.setTranslation((float)wall_boxes[i].getPosition().get0(), (float)wall_boxes[i].getPosition().get1(), (float)wall_boxes[i].getPosition().get2());
        }
        cannonBallModelInstance.transform.setTranslation((float)cannon_ball_body.getPosition().get0(),(float)cannon_ball_body.getPosition().get1(),(float)cannon_ball_body.getPosition().get2());
    }

    private void setupSimulation() {
        cannon_angle = 0;
        cannon_elevation = 0;
        boxTextureIndex = 0;

        for (int i = 0; i < 1000; i++) wb_stepsdis[i] = 0;

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
        wb = 0;

        // Wall boxes
        for (float y = WBOXSIZE/2f; y < WALLHEIGHT; y += WBOXSIZE) {
            for (float x = -WALLWIDTH/2; x < WALLWIDTH/2; x += WBOXSIZE) {
                wall_bodies[wb] = OdeHelper.createBody(world);

                //if (offset) wall_bodies[wb].setPosition (100 + x - WBOXSIZE/2f , y ,100 );
                wall_bodies[wb].setPosition(100.5f + x , y ,100 );
                m.setBox(1,WBOXSIZE,WBOXSIZE,WBOXSIZE);
                m.adjust(WALLMASS);
                wall_bodies[wb].setMass(m);
                wall_boxes[wb] = OdeHelper.createBox (space,WBOXSIZE,WBOXSIZE,WBOXSIZE);
                wall_boxes[wb].setBody (wall_bodies[wb]);

                // libGDX model code
                wallBoxModelInstances.add(createBox());
                wb++;
            }
        }

        // Cannon ball
        cannon_ball_body = OdeHelper.createBody (world);
        cannon_ball_geom = OdeHelper.createSphere (space,CANNON_BALL_RADIUS);
        m.setSphereTotal(CANNON_BALL_MASS,CANNON_BALL_RADIUS);
        cannon_ball_body.setMass(m);
        cannon_ball_geom.setBody(cannon_ball_body);
        cannon_ball_body.setPosition(CANNON_X, CANNON_BALL_RADIUS, CANNON_Z);

        // libGDX model code
        model = modelBuilder.createSphere(RADIUS, RADIUS, RADIUS, 10, 10,
            new Material(ColorAttribute.createDiffuse(Color.BLACK)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        cannonBallModelInstance = new ModelInstance(model);

        // ground
        model = modelBuilder.createBox(200, 1,200,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        ground = new ModelInstance(model);
        ground.transform.setTranslation(0,-0.5f,0);
    }

    private void initBoxTextures() {
        TextureRegion[][] tmp = texture.split(
            texture.getRegionWidth() / WALLWIDTH,
            texture.getRegionHeight() / WALLHEIGHT);

        boxTextures = new TextureRegion[WALLWIDTH * WALLHEIGHT];
        int index = 0;
        for (int i = 0; i < WALLWIDTH; i++) {
            for (int j = 0; j < WALLHEIGHT; j++) {
                boxTextures[index++] = tmp[i][j];
            }
        }
    }

    private ModelInstance createBox(){
        float size = WBOXSIZE/2f;
        modelBuilder.begin();
        meshBuilder = modelBuilder.part("front",GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
            new Material(ColorAttribute.createDiffuse(Color.WHITE), TextureAttribute.createDiffuse(boxTextures[boxTextureIndex])));
        boxTextureIndex++;
        meshBuilder.rect(new Vector3(-size,size,-size), new Vector3(size,size,-size), new Vector3(size,-size,-size), new Vector3(-size,-size,-size), new Vector3(0,0,1));

        meshBuilder = modelBuilder.part("other",GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
            new Material(ColorAttribute.createDiffuse(Color.WHITE)));
        // back
        meshBuilder.rect(new Vector3(-size,-size,size), new Vector3(size,-size,size), new Vector3(size,size,size), new Vector3(-size,size,size), new Vector3(0,0,1));

        //right
        meshBuilder.rect(new Vector3(-size,size,size), new Vector3(-size,size,-size), new Vector3(-size,-size,-size), new Vector3(-size,-size,size), new Vector3(-1,0,0));
        //left
        meshBuilder.rect(new Vector3(size,size,size), new Vector3(size,-size,size), new Vector3(size,-size,-size), new Vector3(size,size,-size), new Vector3(0,0,1));

        // top
        meshBuilder.rect(new Vector3(-size,size,size), new Vector3(size,size,size), new Vector3(size,size,-size), new Vector3(-size,size,-size), new Vector3(0,1,0));
        //bottom
        meshBuilder.rect(new Vector3(-size,-size,-size), new Vector3(size,-size,-size), new Vector3(size,-size,size), new Vector3(-size,-size,size), new Vector3(0,-1,0));

        model = modelBuilder.end();
        return new ModelInstance(model);
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
        model.dispose();
        modelBatch.dispose();

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
                cannon_angle += 0.1;
                break;
            case Input.Keys.RIGHT:
                cannon_angle -= 0.1;
                break;
            case Input.Keys.SPACE:
                fireCannon();
                break;
            case Input.Keys.F1:
                shutdownSimulation();
                Ode4libGDX.game.setScreen(new DemoDynamicCharacterScreen());
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
        for (int i=0; i<3; i++)
            cpos[i] += 3*R4.get(i, 2);//[i*4+2];

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

    private DNearCallback nearCallback = (data, o1, o2) -> nearCallback(data, o1, o2);

    private void nearCallback (Object data, DGeom o1, DGeom o2) {
        int i,n;

        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && OdeHelper.areConnected(b1, b2))
            return;

        final int N = 4;
        DContactBuffer contacts = new DContactBuffer(N);
        n = OdeHelper.collide(o1,o2,N,contacts.getGeomBuffer());//[0].geom,sizeof(dContact));
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

                if (o1 instanceof DSphere && o2 instanceof DBox){
                    o2.getBody().addLinearVel(25,50,0);
                } else if (o2 instanceof DSphere && o1 instanceof DBox){
                    o1.getBody().addLinearVel(-25,50,0);
                }
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

    private void continueLoading() {
        if (mundus.continueLoading()) {
            // Loading complete, load a scene.
            scene = mundus.loadScene("Main Scene.mundus");
            scene.cam.position.set(100, 3, 90);
            scene.cam.lookAt(100,5,100);
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
}
