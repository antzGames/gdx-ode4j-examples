package com.antz.ode4libGDX.screens.demo;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.util.Ode2GdxMathUtils;
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
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.mbrlabs.mundus.commons.Scene;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.runtime.Mundus;
import net.mgsx.gltf.scene3d.attributes.FogAttribute;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DHeightfield;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.DHeightfieldData;
import static org.ode4j.ode.OdeHelper.areConnectedExcluding;
import static org.ode4j.ode.OdeMath.*;
import org.ode4j.ode.internal.DxTrimeshHeightfield;

public class DemoTriMeshHeightFieldScreen implements Screen, InputProcessor {

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
    private InputMultiplexer inputMultiplexer;
    private FirstPersonCameraController controller;
    private ModelInstance heightMI;
    private Color color;

    // ODE stuff
    private static final float DEGTORAD = 0.01745329251994329577f	; //!< PI / 180.0, convert degrees to radians

    // Our heightfield geom
    private DGeom gheight;

    // Heightfield dimensions
    private static final int HFIELD_WSTEP =			15;			// Vertex count along edge >= 2
    private static final int HFIELD_DSTEP =			31;

    private static final float HFIELD_WIDTH =		4.0f;
    private static final float HFIELD_DEPTH =		8.0f;

    private static final float HFIELD_WSAMP =		( HFIELD_WIDTH / ( HFIELD_WSTEP-1 ) );
    private static final float HFIELD_DSAMP =		( HFIELD_DEPTH / ( HFIELD_DSTEP-1 ) );

    // some constants
    private static final int NUM = 5;			    // max number of objects
    private static final float DENSITY = 5.0f;	    // density of all objects
    private static final int  GPB = 3;			    // maximum number of geometries per body
    private static final int  MAX_CONTACTS = 64;	// maximum number of contact points per body

    // dynamics and collision objects
    private static class MyObject {
        DBody body;			                // the body
        DGeom[] geom = new DGeom[GPB];		// geometries representing this body
        ModelInstance modelInstance;        // libGDX model instance
    };

    private static int num = 0;		    // number of objects in simulation
    private static int nextobj=0;		// next object to recycle if num==NUM
    private static DWorld world;
    private static DSpace space;
    private MyObject[] obj = new MyObject[NUM];
    private static DJointGroup contactgroup;
    private static int selected = -1;	        // selected object
    private static boolean random_pos = true;	// drop objects from random position?

    private DHeightfield.DHeightfieldGetHeight heightfield_callback = new DHeightfield.DHeightfieldGetHeight(){
        @Override
        public double call(Object pUserData, int x, int z) {
            return heightfield_callback(pUserData, x, z);
        }
    };

    private double heightfield_callback( Object pUserData, int x, int z ) {
        double fx = ( ((double)x) - ( HFIELD_WSTEP-1 )/2 ) / ( HFIELD_WSTEP-1 );
        double fz = ( ((double)z) - ( HFIELD_DSTEP-1 )/2 ) / ( HFIELD_DSTEP-1 );

        // Create an interesting 'hump' shape
        double h = ( 1.0 ) + ( ( -16.0 ) * ( fx*fx*fx + fz*fz*fz ) );

        return h;
    }

    private DGeom.DNearCallback nearCallback = new DGeom.DNearCallback() {
        @Override
        public void call(Object data, DGeom o1, DGeom o2) {
            nearCallback( data, o1, o2);
        }
    };

    // this is called by dSpaceCollide when two objects in space are
    // potentially colliding.
    private void nearCallback (Object data, DGeom o1, DGeom o2)	{
        // if (o1->body && o2->body) return;

        // exit without doing anything if the two bodies are connected by a joint
        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && areConnectedExcluding (b1,b2,DContactJoint.class)) return;

        DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);   // up to MAX_CONTACTS contacts per box-box
        for (int i=0; i<MAX_CONTACTS; i++) {
            DContact contact = contacts.get(i);
            contact.surface.mode = dContactBounce | dContactSoftCFM;
            contact.surface.mu = dInfinity;
            contact.surface.mu2 = 0;
            contact.surface.bounce = 0.1;
            contact.surface.bounce_vel = 0.1;
            contact.surface.soft_cfm = 0.01;
        }
        int numc = OdeHelper.collide (o1,o2,MAX_CONTACTS,contacts.getGeomBuffer());
        if (numc != 0) {
            DMatrix3 RI = new DMatrix3();
            RI.setIdentity();
            final DVector3 ss = new DVector3(0.02,0.02,0.02);
            for (int i=0; i<numc; i++) {
                DJoint c = OdeHelper.createContactJoint (world,contactgroup,contacts.get(i));
                c.attach (b1,b2);
            }
        }
    }

    @Override
    public void show() {
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(this);
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        batch = new SpriteBatch();
        font.setColor(Color.PINK);
        color = new Color(Color.WHITE);

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

        info = "DemoTriMesh HeightField\n\n" +
            "WASD to move camera, click-drag mouse to rotate camera.\n" +
            "To drop another object, press:\n" +
            "   1 for sphere.\n" +
            "   2 for box.\n" +
            "F1 to run Mundus Terrain Integration\n";
        System.out.println(info);

        initODE();
  }


  private void initODE(){
      num = 0;		// number of objects in simulation
      nextobj=0;		// next object to recycle if num==NUM
      selected = -1;

      // create world
      OdeHelper.initODE2(0);
      world = OdeHelper.createWorld ();
      space = OdeHelper.createHashSpace (null);
      contactgroup = OdeHelper.createJointGroup ();
      world.setGravity (0,0,-0.05);
      world.setCFM (1e-5);
      world.setAutoDisableFlag (true);
      world.setContactMaxCorrectingVel (0.1);
      world.setContactSurfaceLayer (0.001);
      for (int i = 0; i < obj.length; i++) {
          obj[i] = new MyObject();
      }

      world.setAutoDisableAverageSamplesCount( 1 );

      // base plane to catch overspill
      //OdeHelper.createPlane( space, 0, 0, 1, 0 );

      // our heightfield floor
      DHeightfieldData height = OdeHelper.createHeightfieldData();

      // Create an finite heightfield.
      height.buildCallback( null, heightfield_callback,
          HFIELD_WIDTH, HFIELD_DEPTH, HFIELD_WSTEP, HFIELD_DSTEP,
          1.0, 0.0, 0.0, false );
      height.setBounds( ( -4.0 ), ( +6.0 ) );
      gheight = new DxTrimeshHeightfield(space, height, true);// OdeHelper.createHeightfield( space, height, true );
      DVector3 pos = new DVector3();

      // Rotate so Z is up, not Y (which is the default orientation)
      DMatrix3 R = new DMatrix3();
      R.setIdentity();
      dRFromAxisAndAngle( R, 1, 0, 0, DEGTORAD * 90 );

      // Place it.
      gheight.setRotation( R );
      gheight.setPosition( pos );
  }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
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
//        scene.sceneGraph.update(); // update Mundus
//        scene.render(); // render Mundus scene

        // 3D models drawing
        modelBatch.begin(scene.cam);
        doStep(false); // so the original demo did rendering in the simulation loop, I did the same thing, but I think its not a good idea
        modelBatch.end();

        // 2D stuff for info text
        batch.begin();
        font.draw(batch, info + "FPS:" + Gdx.graphics.getFramesPerSecond(), 10, 140);
        batch.end();
    }


    public void doStep (boolean pause) {
        space.collide (null,nearCallback);
        if (!pause) {
            world.quickStep (0.05);
        }
        // remove all contact joints
        contactgroup.empty();

        // Draw Heightfield
        drawGeom(gheight, -1,null, null);

        color.set(0.5f,1,0.5f,0.5f);
        for (int i=0; i<num; i++) {
            for (int j=0; j < GPB; j++) {
                if (i==selected) {
                    color.set(0,0.7f,1,1);
                    //dsSetColor (0f,0.7f,1f);
                } else if (!obj[i].body.isEnabled ()) {
                    color.set(1,0.8f,0,1);
                    //dsSetColor (1,0,0);
                } else {
                    color.set(Color.YELLOW);
                    //dsSetColor (1,1,0);
                }
                drawGeom (obj[i].geom[j], i,null, null);
            }
        }
    }

    // draw a geom
    private void drawGeom (DGeom g, int objInstance,DVector3C pos,  DMatrix3C R) {
        if (g==null) return;
        if (pos==null) pos = g.getPosition();
        if (R==null) R = g.getRotation();

        if (g instanceof DBox) {
            DVector3C sides = ((DBox)g).getLengths();
            if (obj[objInstance].modelInstance == null) {
                model = modelBuilder.createBox((float) sides.get0(), (float) sides.get1(), (float) sides.get2(), GL20.GL_LINES,
                    new Material(ColorAttribute.createDiffuse(color)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                obj[objInstance].modelInstance = new ModelInstance(model);
            }
            obj[objInstance].modelInstance.transform.set(Ode2GdxMathUtils.getGdxQuaternion(g.getQuaternion()));
            obj[objInstance].modelInstance.transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            modelBatch.render(obj[objInstance].modelInstance);
            //dsDrawBox (pos,R,sides);
        } else if (g instanceof DSphere) {
            if (obj[objInstance].modelInstance == null) {
                model = modelBuilder.createSphere((float) ((DSphere) g).getRadius() * 2, (float) ((DSphere) g).getRadius() * 2, (float) ((DSphere) g).getRadius() * 2,
                    15, 15, GL20.GL_LINES,
                    new Material(ColorAttribute.createDiffuse(color)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                obj[objInstance].modelInstance = new ModelInstance(model);
            }
            obj[objInstance].modelInstance.transform.set(Ode2GdxMathUtils.getGdxQuaternion(g.getQuaternion()));
            obj[objInstance].modelInstance.transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            modelBatch.render(obj[objInstance].modelInstance);
            //dsDrawSphere( pos,R, ((DSphere)g).getRadius() );
        } else if (g instanceof DHeightfield) {
            // Set ox and oz to zero for DHEIGHTFIELD_CORNER_ORIGIN mode.
            int ox = (int) (-HFIELD_WIDTH / 2);
            int oz = (int) (-HFIELD_DEPTH / 2);
            color.set(0.5f, 1, 0.5f, 0.5f);
            if (heightMI == null) {
                modelBuilder.begin();
                MeshPartBuilder meshPartBuilder = modelBuilder.part("line", 1, 3, new Material());
                meshPartBuilder.setColor(Color.CYAN);

                for (int i = 0; i < HFIELD_WSTEP - 1; ++i) {
                    for (int j = 0; j < HFIELD_DSTEP - 1; ++j) {
                        float[] a = new float[3], b = new float[3];
                        float[] c = new float[3], d = new float[3];

                        a[0] = ox + (i) * HFIELD_WSAMP;
                        a[1] = (float) heightfield_callback(null, i, j);
                        a[2] = oz + (j) * HFIELD_DSAMP;

                        b[0] = ox + (i + 1) * HFIELD_WSAMP;
                        b[1] = (float) heightfield_callback(null, i + 1, j);
                        b[2] = oz + (j) * HFIELD_DSAMP;

                        c[0] = ox + (i) * HFIELD_WSAMP;
                        c[1] = (float) heightfield_callback(null, i, j + 1);
                        c[2] = oz + (j + 1) * HFIELD_DSAMP;

                        d[0] = ox + (i + 1) * HFIELD_WSAMP;
                        d[1] = (float) heightfield_callback(null, i + 1, j + 1);
                        d[2] = oz + (j + 1) * HFIELD_DSAMP;

                        meshPartBuilder.line(a[0], a[1], a[2], b[0], b[1], b[2]);
                        meshPartBuilder.line(b[0], b[1], b[2], c[0], c[1], c[2]);
                        meshPartBuilder.line(c[0], c[1], c[2], a[0], a[1], a[2]);
                        meshPartBuilder.line(b[0], b[1], b[2], c[0], c[1], c[2]);
                        meshPartBuilder.line(c[0], c[1], c[2], d[0], d[1], d[2]);
                        meshPartBuilder.line(d[0], d[1], d[2], b[0], b[1], b[2]);

//                    dsDrawTriangle( pos, R, a, c, b, true );
//                    dsDrawTriangle( pos, R, b, c, d, true );
                    }
                }
                model = modelBuilder.end();
                heightMI = new ModelInstance(model);
                heightMI.transform.set(Ode2GdxMathUtils.getGdxQuaternion(g.getQuaternion()));
                heightMI.transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            }
            modelBatch.render(heightMI);
        }
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
        model.dispose();
        modelBatch.dispose();
        batch.dispose();
        font.dispose();

        odeDispose();
    }

    private void odeDispose(){
        // ode cleanup
        contactgroup.destroy ();
        space.destroy ();
        world.destroy ();
        gheight.destroy();
        OdeHelper.closeODE();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F1) {
            odeDispose();
            Ode4libGDX.game.setScreen(new DemoMundusHeightFieldScreen());
        }
        return false;
    }


    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char cmd) {
        int i,j,k;
        double[] sides= new double[3];
        DMass m = OdeHelper.createMass();
        boolean setBody = false;

        if (cmd == '1' || cmd == '2') {
            if (num < NUM) {
                i = num;
                num++;
            }  else {
                i = nextobj;
                nextobj++;
                nextobj %= num;

                // destroy the body and geoms for slot i
                obj[i].body.destroy();
                obj[i].body = null;

                for (k=0; k < GPB; k++)	{
                    if (obj[i].geom[k]!=null) {
                        obj[i].geom[k].destroy();
                        obj[i].geom[k] = null;
                    }
                }
                obj[i] = new MyObject();
            }

            obj[i].body = OdeHelper.createBody (world);
            for (k=0; k<3; k++) sides[k] = dRandReal()*0.5+0.1;

            DMatrix3 R = new DMatrix3();
            if (random_pos) {
                obj[i].body.setPosition(
                    (dRandReal()-0.5)*HFIELD_WIDTH*0.75,
                    (dRandReal()-0.5)*HFIELD_DEPTH*0.75,
                    dRandReal() + 2 );
                dRFromAxisAndAngle (R,dRandReal()*2.0-1.0,dRandReal()*2.0-1.0,
                    dRandReal()*2.0-1.0,dRandReal()*10.0-5.0);
            } else {
                double maxheight = 0;
                for (k=0; k<num; k++) {
                    DVector3C pos = obj[k].body.getPosition ();
                    if (pos.get2() > maxheight) maxheight = pos.get2();

                }
                obj[i].body.setPosition(0,maxheight+1,0);
                dRFromAxisAndAngle (R,0,0,1,dRandReal()*10.0-5.0);
            }
            obj[i].body.setRotation (R);
            obj[i].body.setData (i);

            if (cmd == '2') {
                m.setBox (DENSITY,sides[0],sides[1],sides[2]);
                obj[i].geom[0] = OdeHelper.createBox (space,sides[0],sides[1],sides[2]);
            } else if (cmd == '1') {
                sides[0] *= 0.5;
                m.setSphere(DENSITY, sides[0]);
                obj[i].geom[0] = OdeHelper.createSphere(space, sides[0]);
            }

            if (!setBody) {
                for (k=0; k < GPB; k++) {
                    if (obj[i].geom[k]!=null) {
                        obj[i].geom[k].setBody (obj[i].body);
                    }
                }
                obj[i].body.setMass (m);
            }
        }
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
//        if (Gdx.app.getType().equals(Application.ApplicationType.Android))
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
            scene.cam.position.set(0, 6, 6);
            scene.cam.lookAt(0,0,0);
            scene.cam.up.set(Vector3.Z);
            scene.cam.update();

            // Remove fog
            FogAttribute fogAttribute = (FogAttribute) scene.environment.get(FogAttribute.FogEquation);
            fogAttribute.value.x = 2000f; // Near plane
            fogAttribute.value.y = 2500f; // Far plane

            // setup input
            controller = new FirstPersonCameraController(scene.cam);
            controller.setVelocity(10f);
            inputMultiplexer.addProcessor(controller);
            Gdx.input.setInputProcessor(inputMultiplexer);
            // Update our game state
            gameState = GameState.RENDER;
        }
    }
}
