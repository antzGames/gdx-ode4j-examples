package com.antz.ode4libGDX.screens;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.util.Ode2GdxMathUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
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
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.mbrlabs.mundus.commons.Scene;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.runtime.Mundus;
import net.mgsx.gltf.scene3d.attributes.FogAttribute;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DAABBC;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DCapsule;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DConvex;
import org.ode4j.ode.DCylinder;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DRay;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DTriMesh;
import org.ode4j.ode.DTriMeshData;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import static org.ode4j.ode.OdeHelper.*;
import static org.ode4j.ode.OdeMath.*;

public class DemoTriMeshScreen implements Screen, InputProcessor {

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
    private Mesh mesh;
    private InputMultiplexer inputMultiplexer;
    private FirstPersonCameraController controller;
    private ModelInstance m;
    private Color color;

    // ODE stuff
    // some constants
    private static final int NUM = 10;			// max number of objects
    private static final double DENSITY = (5.0);		// density of all objects
    private static final int GPB = 3;			// maximum number of geometries per body
    private static final int MAX_CONTACTS = 40;		// maximum number of contact points per body

    // dynamics and collision objects
    private static class MyObject {
        DBody body;			// the body
        DGeom[] geom = new DGeom[GPB];		// geometries representing this body
    };

    private static int num = 0;		// number of objects in simulation
    private static int nextobj=0;		// next object to recycle if num==NUM
    private static DWorld world;
    private static DSpace space;
    private static MyObject[] obj;
    private static DJointGroup contactgroup;
    private static int selected = -1;	// selected object
    private static boolean show_aabb = false;	// show geom AABBs?
    private static boolean show_contacts = false;	// show contact points?
    private static boolean random_pos = true;	// drop objects from random position?

    private static final int VertexCount = 5;
    private static final int IndexCount = 12;

    private static float[] Size;
    private static float[] Vertices = new float[VertexCount*3];
    private static int[] Indices = new int[IndexCount];

    private static DTriMesh TriMesh;
    private static DRay Ray;


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
        int numc = OdeHelper.collide (o1,o2,MAX_CONTACTS,contacts.getGeomBuffer() );
        if (numc != 0) {
            DMatrix3 RI = new DMatrix3();
            RI.setIdentity();
            DVector3 ss = new DVector3(0.02,0.02,0.02);
            for (int i=0; i<numc; i++) {
                DContact contact = contacts.get(i);
                if (o1 instanceof DRay || o2 instanceof DRay){
                    DMatrix3 Rotation = new DMatrix3();
                    Rotation.setIdentity();

                    model = modelBuilder.createSphere(0.01f, 0.01f, 0.01f, 10, 10,
                        new Material(ColorAttribute.createDiffuse(color)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    m = new ModelInstance(model);
                    m.transform.set(new Matrix3(Rotation.toFloatArray()));
                    m.transform.setTranslation((float)contact.geom.pos.get0(), (float)contact.geom.pos.get1(), (float)contact.geom.pos.get2());
                    modelBatch.render(m);
                    //dsDrawSphere(contact.geom.pos, Rotation, (0.01));

                    DVector3 End = new DVector3();
                    End.eqSum(contact.geom.pos, contact.geom.normal, contact.geom.depth );

                    modelBuilder.begin();
                    MeshPartBuilder meshPartBuilder = modelBuilder.part("line", 1, 3, new Material());
                    meshPartBuilder.setColor(color);
                    meshPartBuilder.line((float)contact.geom.pos.get0(), (float)contact.geom.pos.get1() , (float)contact.geom.pos.get2(),
                        (float)End.get0(), (float)End.get1() , (float)End.get2());
                    model = modelBuilder.end();
                    m = new ModelInstance(model);
                    m.transform.set(new Matrix3(Rotation.toFloatArray()));
                    m.transform.setTranslation((float)contact.geom.pos.get0(), (float)contact.geom.pos.get1(), (float)contact.geom.pos.get2());
                    modelBatch.render(m);
                    //dsDrawLine(contact.geom.pos, End);
                    continue;
                }

                DJoint c = OdeHelper.createContactJoint(world,contactgroup,contact );
                c.attach (b1,b2);
                if (show_contacts) {
                    model = modelBuilder.createBox( (float) ss.get0(), (float)ss.get1(), (float)ss.get2(),
                        new Material(ColorAttribute.createDiffuse(color)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                    m = new ModelInstance(model);
                    m.transform.set(new Matrix3(RI.toFloatArray()));
                    m.transform.setTranslation((float) contact.geom.pos.get0(), (float) contact.geom.pos.get1(), (float) contact.geom.pos.get2());
                    modelBatch.render(m);
                    //dsDrawBox (contact.geom.pos,RI,ss);
                }

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

        info = "To drop another object, press:\n" +
        "   1 for box.\n" +
        "   2 for sphere.\n" +
        "   3 for capsule.\n" +
        "   4 for cylinder.\n" +
        "   5 for a composite object.\n" +
//        "To select an object, press SPACE.\n" +
//        "To disable the selected object, press -.\n" +
//        "To enable the selected object, press +.\n" +
//        "To toggle showing the geom AABBs, press B.\n" +
//        "To toggle showing the contact points, press C.\n" +
        "To toggle dropping from random position/orientation, press R.\n" +
        "F1 to run Demo Crash.\n";
        System.out.println(info);

        initODE();
  }


  private void initODE(){
      // create world

      num = 0;		// number of objects in simulation
      nextobj=0;		// next object to recycle if num==NUM
      selected = -1;

      OdeHelper.initODE2(0);
      world = OdeHelper.createWorld();

      space = OdeHelper.createSimpleSpace();
      contactgroup = OdeHelper.createJointGroup();
      world.setGravity (0,0,-0.5);
      world.setCFM (1e-5);
      //dCreatePlane (space,0,0,1,0);
      //memset (obj,0,sizeof(obj));TZ TODO ?

      obj = new MyObject[NUM];
      for (int i = 0; i < obj.length; i++) obj[i] = new MyObject();

      Size = new float[]{ 5.0f, 5.0f, 2.5f };

      Vertices = new float[]{
          -Size[0], -Size[1], Size[2],
          Size[0], -Size[1], Size[2],
          Size[0],  Size[1], Size[2],
          -Size[0],  Size[1], Size[2],
          0f, 0f, 0f};

      Indices = new int[] {
          0, 1, 4,
          1, 2, 4,
          2, 3, 4,
          3, 0, 4
      };

      DTriMeshData Data = OdeHelper.createTriMeshData();

      //dGeomTriMeshDataBuildSimple(Data, (dReal*)Vertices, VertexCount, Indices, IndexCount);
      //dGeomTriMeshDataBuildSingle(Data, Vertices[0], 3 * sizeof(float), VertexCount, &Indices[0], IndexCount, 3 * sizeof(dTriIndex));
      //		Data.build(Data, Vertices[0], 3,// * sizeof(float),
      //				VertexCount, Indices[0], IndexCount, 3);// * sizeof(dTriIndex));
      Data.build(Vertices, Indices);// * sizeof(dTriIndex));
      Data.preprocess();
      TriMesh = OdeHelper.createTriMesh(space, Data, null, null, null);

      //TriMesh.setPosition(0, 0, 1.0);

      Ray = OdeHelper.createRay(space, 0.9);
      DVector3 Origin, Direction;
      Origin = new DVector3( 0, 0, 0.5 );

      Direction = new DVector3( 0, 1.1, -1 );
      Direction.normalize();

      Ray.set(Origin, Direction);
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
        doStep(false, modelBatch); // so the original demo did rendering in the simulation loop, I did the same thing, but I think its not a good idea
        modelBatch.end();

        // 2D stuff for info text
        batch.begin();
        font.draw(batch, info + "FPS:" + Gdx.graphics.getFramesPerSecond(), 10, 160);
        batch.end();
    }

    public void doStep (boolean pause, ModelBatch modelBatch) {
        color.set(Color.BLUE);
        //dsSetColor (0,0,2);
        space.collide (0,nearCallback);
        if (!pause) world.step (0.05);
        //if (!pause) dWorldStepFast (world,0.05, 1);

        // remove all contact joints
        contactgroup.empty ();

        color.set(Color.YELLOW);
        //dsSetColor (1,1,0);
        //dsSetTexture (DS_TEXTURE_NUMBER.DS_WOOD);
        for (int i=0; i<num; i++) {
            for (int j=0; j < GPB; j++) {
                if (i==selected) {
                    color.set(0,0.7f,1,1);
                    //dsSetColor (0f,0.7f,1f);
                } else if (!obj[i].body.isEnabled ()) {
                    color.set(Color.RED);
                    //dsSetColor (1,0,0);
                } else {
                    color.set(Color.YELLOW);
                    //dsSetColor (1,1,0);
                }
                drawGeom (obj[i].geom[j], null, null, show_aabb);
            }
        }

        if (TriMesh!=null) {
            DVector3C Pos = TriMesh.getPosition();
            DMatrix3C Rot = TriMesh.getRotation();
            for (int i = 0; i < IndexCount; i+=3){
                int p0 = Indices[i + 0] * 3;
                int p1 = Indices[i + 1] * 3;
                int p2 = Indices[i + 2] * 3;

                modelBuilder.begin();
                MeshPartBuilder meshPartBuilder = modelBuilder.part("line", 1, 3, new Material());
                meshPartBuilder.setColor(Color.CYAN);
                meshPartBuilder.line(Vertices[p0], Vertices[p0+1],Vertices[p0+2],
                    Vertices[p1], Vertices[p1+1],Vertices[p1+2]);
                meshPartBuilder.line(Vertices[p1], Vertices[p1+1],Vertices[p1+2],
                    Vertices[p2], Vertices[p2+1],Vertices[p2+2]);
                meshPartBuilder.line(Vertices[p2], Vertices[p2+1],Vertices[p2+2],
                    Vertices[p0], Vertices[p0+1],Vertices[p0+2]);

                model = modelBuilder.end();
                m = new ModelInstance(model);
                m.transform.set(Ode2GdxMathUtils.getGdxQuaternion(TriMesh.getQuaternion()));
                m.transform.setTranslation((float)Pos.get0(), (float)Pos.get1(), (float)Pos.get2());
                modelBatch.render(m);
                //dsDrawTriangle(Pos, Rot, Vertices, p0, p1, p2, false);
            }
        }

        if (Ray!=null) {
            DVector3C Origin = Ray.getPosition();
            DVector3C Direction = Ray.getDirection();
            double Length = Ray.getLength();
            DVector3 End = new DVector3();
            End.eqSum( Origin, Direction, Length );

            modelBuilder.begin();
            MeshPartBuilder meshPartBuilder = modelBuilder.part("line", 1, 3, new Material());
            meshPartBuilder.setColor(color);
            meshPartBuilder.line((float)Origin.get0(), (float)Origin.get1() , (float)Origin.get2(),
                (float)End.get0(), (float)End.get1() , (float)End.get2());
            model = modelBuilder.end();
            m = new ModelInstance(model);
            modelBatch.render(m);
            //dsDrawLine(Origin, End);
        }
    }

    // draw a geom
    private void drawGeom (DGeom g, DVector3C pos, DMatrix3C R, boolean show_aabb)
    {
        if (g==null) return;
        if (pos==null) pos = g.getPosition ();
        if (R==null) R = g.getRotation ();

        DQuaternionC qOde = g.getQuaternion();

        if (g instanceof DBox) {
            DVector3C sides = ((DBox)g).getLengths();
            model = modelBuilder.createBox( (float) sides.get0(), (float)sides.get1(), (float)sides.get2(), GL20.GL_LINES,
                new Material(ColorAttribute.createDiffuse(color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            m = new ModelInstance(model);
            m.transform.set(Ode2GdxMathUtils.getGdxQuaternion(qOde));
            m.transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            modelBatch.render(m);
            //dsDrawBox (pos,R,sides);
        } else if (g instanceof DSphere) {
            model = modelBuilder.createSphere( (float) ((DSphere)g).getRadius()*2, (float)((DSphere)g).getRadius()*2, (float)((DSphere)g).getRadius()*2,
                15,15, GL20.GL_LINES,
                new Material(ColorAttribute.createDiffuse(color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            m = new ModelInstance(model);
            m.transform.set(Ode2GdxMathUtils.getGdxQuaternion(qOde));
            m.transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            modelBatch.render(m);
            //dsDrawSphere( pos,R, ((DSphere)g).getRadius() );
        } else if (g instanceof DCapsule) {
            DCapsule c = (DCapsule) g;
            double x = c.getLength();
            if (c.getLength() < 2 * c.getRadius()) x = c.getRadius() * 2.01d;

            model = modelBuilder.createCapsule((float)c.getRadius(), (float) x, 5,
                GL20.GL_LINES,
                new Material(ColorAttribute.createDiffuse(color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            m = new ModelInstance(model);
            m.transform.set(Ode2GdxMathUtils.getGdxQuaternion(qOde));
            m.transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            modelBatch.render(m);
            //dsDrawCapsule( pos, R, c.getLength(), c.getRadius() );
        } else if (g instanceof DCylinder) {
            DCylinder c = (DCylinder) g;
            model = modelBuilder.createCylinder((float)c.getLength(), (float)c.getRadius(), (float)c.getLength(), 15,
                GL20.GL_LINES,
                new Material(ColorAttribute.createDiffuse(color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            m = new ModelInstance(model);
            m.transform.set(Ode2GdxMathUtils.getGdxQuaternion(qOde));
            m.transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            modelBatch.render(m);
            //dsDrawCylinder (pos, R, c.getLength(), c.getRadius());
        } else if (g instanceof DConvex) {
            //dVector3 sides={0.50,0.50,0.50};
//
//
//            dsDrawConvex(pos,R,ConvexCubeGeom.planes,
//                ConvexCubeGeom.planecount,
//                ConvexCubeGeom.points,
//                ConvexCubeGeom.pointcount,
//                ConvexCubeGeom.polygons);
        }

        if (show_aabb) {
            // draw the bounding box for this geom
            DAABBC aabb = g.getAABB();
            DVector3 bbpos = aabb.getCenter();
            DVector3 bbsides = aabb.getLengths();
            DMatrix3 RI = new DMatrix3();
            RI.setIdentity ();
            color.set(1,0,0,0.5f);
            model = modelBuilder.createBox( (float) bbsides.get0(), (float)bbsides.get1(), (float)bbsides.get2(), GL20.GL_LINES,
                new Material(ColorAttribute.createDiffuse(color)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            m = new ModelInstance(model);
            m.transform.set(Ode2GdxMathUtils.getGdxQuaternion(qOde));
            m.transform.setTranslation((float) bbpos.get0(), (float) bbpos.get1(), (float) bbpos.get2());
            modelBatch.render(m);

//            dsSetColorAlpha (1f,0f,0f,0.5f);
//            dsDrawBox (bbpos,RI,bbsides);
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

        // ode cleanup
        contactgroup.destroy ();
        space.destroy ();
        world.destroy ();
        OdeHelper.closeODE();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F1) Ode4libGDX.game.setScreen(new DemoCrashScreen());
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

//        info = 	"To drop another object, press:\n" +
//            "   1 for box.\n" +
//            "   2 for sphere.\n" +
//            "   3 for capsule.\n" +
//            "   4 for cylinder.\n" +
//            "   5 for a convex object.\n" +
//            "   6 for a composite object.\n" +

        if (cmd == '1' || cmd == '2' || cmd == '3' || cmd == '4' || cmd == '5' || cmd == '6') {
            if (num < NUM) {
                i = num;
                num++;
            }  else {
                i = nextobj;
                nextobj++;
                if (nextobj >= num) nextobj = 0;

                // destroy the body and geoms for slot i
                obj[i].body.destroy ();
                for (k=0; k < GPB; k++) {
                    if (obj[i].geom[k]!=null) obj[i].geom[k].destroy ();
                }
                obj[i] = new MyObject();
            }

            obj[i].body = OdeHelper.createBody (world);
            for (k=0; k<3; k++) sides[k] = dRandReal()*0.5+0.1;

            DMatrix3 R = new DMatrix3();
            if (random_pos) {
                obj[i].body.setPosition (
                    dRandReal()*2-1,dRandReal()*2-1,dRandReal()+1);
                dRFromAxisAndAngle(R,dRandReal()*2.0-1.0,dRandReal()*2.0-1.0,
                    dRandReal()*2.0-1.0,dRandReal()*10.0-5.0);
            } else {
                double maxheight = 0;
                for (k=0; k<num; k++) {
                    DVector3C pos = obj[k].body.getPosition ();
                    if (pos.get2() > maxheight) maxheight = pos.get2();

                }
                obj[i].body.setPosition (0,0,maxheight+1);
                //
                dRFromAxisAndAngle (R,0,0,1,dRandReal()*10.0-5.0);
            }
            obj[i].body.setRotation (R);
            obj[i].body.setData (i);

            if (cmd == '1') {
                m.setBox (DENSITY,sides[0],sides[1],sides[2]);
                obj[i].geom[0] = OdeHelper.createBox (space,sides[0],sides[1],sides[2]);
            } else if (cmd == '3') {
                sides[0] *= 0.5;
                m.setCapsule (DENSITY,3,sides[0],sides[1]);
                obj[i].geom[0] = OdeHelper.createCapsule (space,sides[0]/2,sides[0]*3);
//            } else if (cmd == '5') {
//                m.setBox (DENSITY,0.25,0.25,0.25);
//                obj[i].geom[0] = OdeHelper.createConvex (space,
//                    ConvexCubeGeom.planes,
//                    ConvexCubeGeom.planecount,
//                    ConvexCubeGeom.points,
//                    ConvexCubeGeom.pointcount,
//                    ConvexCubeGeom.polygons);
            } else if (cmd == '4') {
                sides[1] *= 0.5;
                m.setCylinder(DENSITY,3,sides[0],sides[1]);
                obj[i].geom[0] = OdeHelper.createCylinder (space,sides[0],sides[1]);
            } else if (cmd == '2') {
                sides[0] *= 0.5;
                m.setSphere (DENSITY,sides[0]);
                obj[i].geom[0] = OdeHelper.createSphere (space,sides[0]);
            } else if (cmd == '5') {
                setBody = true;

                // start accumulating masses for the encapsulated geometries
                DMass m2 = OdeHelper.createMass();
                m.setZero ();

                DVector3[] dpos = DVector3.newArray(GPB);	// delta-positions for encapsulated geometries
                DMatrix3[] drot = DMatrix3.newArray(GPB);

                // set random delta positions
                for (j=0; j<GPB; j++) {
                    for (k=0; k<3; k++) {
                        dpos[j].set(k, dRandReal()*0.3-0.15 );
                    }
                }

                for (k=0; k<GPB; k++) {
                    if (k==0) {
                        double radius = dRandReal()*0.25+0.05;
                        obj[i].geom[k] = OdeHelper.createSphere (space,radius);
                        m2.setSphere (DENSITY,radius);
                    }
                    else if (k==1) {
                        obj[i].geom[k] = OdeHelper.createBox (space,sides[0],sides[1],sides[2]);
                        m2.setBox (DENSITY,sides[0],sides[1],sides[2]);
                    }
                    else {
                        double radius = dRandReal()*0.1+0.05;
                        double length = dRandReal()*1.0+0.1;
                        obj[i].geom[k] = OdeHelper.createCapsule (space,radius,length);
                        m2.setCapsule (DENSITY,3,radius,length);
                    }

                    dRFromAxisAndAngle (drot[k],dRandReal()*2.0-1.0,dRandReal()*2.0-1.0,
                        dRandReal()*2.0-1.0,dRandReal()*10.0-5.0);

                    m2.rotate (drot[k]);
                    m2.translate (dpos[k]);

                    // add to the total mass
                    m.add (m2);
                }

                // move all encapsulated objects so that the center of mass is (0,0,0)
                DVector3 negC = new DVector3(m.getC()).scale(-1);
                for (k=0; k<GPB; k++) {
                    obj[i].geom[k].setBody(obj[i].body);
                    obj[i].geom[k].setOffsetPosition(dpos[k].reAdd(negC));
                    obj[i].geom[k].setOffsetRotation(drot[k]);
                }
                m.translate(negC);
                obj[i].body.setMass(m);
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

//            "To select an object, press SPACE.\n" +
//            "To disable the selected object, press -.\n" +
//            "To enable the selected object, press +.\n" +
//            "To toggle showing the geom AABBs, press B.\n" +
//            "To toggle showing the contact points, press C.\n" +
//            "To toggle dropping from random position/orientation, press R.\n" +
//            "F1 to run Demo Crash.\n";

        if (cmd == ' ') {
            selected++;
            if (selected >= num) selected = 0;
            if (selected < 0) selected = 0;
        } else if (cmd == '-' && selected >= 0 && selected < num) {
            obj[selected].body.disable ();
        } else if (cmd == '=' && selected >= 0 && selected < num) {
            obj[selected].body.enable ();
        } else if (cmd == 'b') {
            show_aabb ^= true;
        } else if (cmd == 'c') {
            show_contacts ^= true;
        } else if (cmd == 'r') {
            random_pos ^= true;
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
