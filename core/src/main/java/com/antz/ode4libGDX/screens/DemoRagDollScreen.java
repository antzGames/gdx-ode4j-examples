package com.antz.ode4libGDX.screens;

import static org.ode4j.ode.OdeHelper.*;
import static org.ode4j.ode.OdeMath.*;
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
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.mbrlabs.mundus.commons.Scene;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.runtime.Mundus;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DCapsule;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.OdeMath;
import org.ode4j.ode.internal.Rotation;
import org.ode4j.ode.internal.ragdoll.DxRagdoll;

public class DemoRagDollScreen implements Screen, InputProcessor {

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
    private ModelInstance[] m = new ModelInstance[32];

    // **** ode4j Ragdoll Stuff
    private static final int  MAX_CONTACTS = 64;		// maximum number of contact points per body
    private DWorld world;
    private DSpace space;
    private DxRagdoll ragdoll;
    private DJointGroup contactgroup;
    private boolean show_contacts = false;	// show contact points?


    @Override
    public void show() {
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(this);
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        batch = new SpriteBatch();
        font.setColor(Color.BLUE);

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

        info = "DemoRagDoll\n\n" +
            "WASD to move camera, click-drag mouse to rotate camera.\n" +
            "SPACE to apply some force to the ragdoll.\n" +
            "F1 for Trimesh Heightfield Demo\n";

        System.out.println(info);

        OdeHelper.initODE2(0);
        world = OdeHelper.createWorld();
        world.setGravity(0,-9.8,0);
        world.setDamping(1e-4, 1e-5);
        //    dWorldSetERP(world, 1);
        space = OdeHelper.createSimpleSpace();
        contactgroup = OdeHelper.createJointGroup ();
        OdeHelper.createPlane( space, 0, 1, 0, 0 );

        ragdoll = new DxRagdoll(world, space, new DxDefaultHumanRagdollConfig());
        ragdoll.setAngularDamping(0.1);
        DQuaternion q = new DQuaternion(1, 0, 0, 0);
        Rotation.dQFromAxisAndAngle(q, new DVector3(1, 0, 0), -0.5 * Math.PI);
        for (int i = 0; i < ragdoll.getBones().size(); i++) {
            DxRagdoll.DxRagdollBody bone = ragdoll.getBones().get(i);
            DGeom g = OdeHelper.createCapsule(space, bone.getRadius(), bone.getLength());
            DBody body = bone.getBody();
            DQuaternion qq = new DQuaternion();
            OdeMath.dQMultiply1(qq, q, body.getQuaternion());
            body.setQuaternion(qq);
            DMatrix3 R = new DMatrix3();
            OdeMath.dRfromQ(R, q);
            DVector3 v = new DVector3();
            OdeMath.dMultiply0_133(v, body.getPosition(), R);
            body.setPosition(v.get0(), v.get1(), v.get2());
            g.setBody(body);
        }
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
        controller.update();        // camera controller
        scene.sceneGraph.update();  // update Mundus
        scene.render();             // render Mundus scene

        // 3D models drawing
        modelBatch.begin(scene.cam);
        doStep(false, modelBatch); // so the original demo did rendering in the simulation loop, I did the same thing, but I think its not a good idea
        modelBatch.end();

        // 2D stuff for info text
        batch.begin();
        font.draw(batch, info + "FPS:" + Gdx.graphics.getFramesPerSecond(), 10, 105);
        batch.end();
    }

    private void doStep(boolean pause, ModelBatch modelBatch){
        space.collide (null,nearCallback);
        if (!pause) {
            final double step = 0.005;
            final int nsteps = 4;
            for (int i=0; i<nsteps; ++i) {
                world.quickStep(step);
            }
        }
        contactgroup.empty();
        // now we draw everything
        int i=0;
        for (DGeom g : space.getGeoms()) {
            drawGeom(g, modelBatch, i++);
        }
    }

    private void drawGeom(DGeom g, ModelBatch modelBatch, int i) {
        if (g instanceof DCapsule) {
            DVector3C pos = g.getPosition();
            DMatrix3C rot = g.getRotation();
            DCapsule cap = (DCapsule) g;

            // create libGDX modelInstances for rendering
            double x = cap.getLength();
            if (cap.getLength() < 2 * cap.getRadius()) x = cap.getRadius() * 2.01d;

            if (m[i] == null) {
                model = modelBuilder.createCapsule((float) cap.getRadius(), (float) x, 5, GL20.GL_LINES,
                    new Material(ColorAttribute.createDiffuse(Color.BLACK)),
                    VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
                m[i] = new ModelInstance(model);
            }
            Quaternion q = Ode2GdxMathUtils.getGdxQuaternion(cap.getQuaternion());  // Using new convert util class
            m[i].transform.set(q);

            m[i].transform.setTranslation((float) pos.get0(), (float) pos.get1(), (float) pos.get2());
            modelBatch.render(m[i]);
            //dsDrawCapsule (pos, rot, cap.getLength(), cap.getRadius()); // original draw call
        }
    }

    private DGeom.DNearCallback nearCallback = new DGeom.DNearCallback() {
        @Override
        public void call(Object data, DGeom o1, DGeom o2) {
            nearCallback(data, o1, o2);
        }
    };

    private void nearCallback (Object data, DGeom o1, DGeom o2) {
        int i;
        // if (o1->body && o2->body) return;

        // exit without doing anything if the two bodies are connected by a joint
        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && areConnectedExcluding (b1,b2, DContactJoint.class)) {
            return;
        }

        DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);   // up to MAX_CONTACTS contacts per box-box
        for (i=0; i<MAX_CONTACTS; i++) {
            DContact contact = contacts.get(i);
            contact.surface.mode = dContactBounce | dContactSoftCFM;
            contact.surface.mu = 100;
            contact.surface.mu2 = 0;
            contact.surface.bounce = 0.01;
            contact.surface.bounce_vel = 0.01;
            contact.surface.soft_cfm = 0.0001;
        }
        int numc = OdeHelper.collide (o1,o2,MAX_CONTACTS,contacts.getGeomBuffer());
        if (numc!=0) {
            DMatrix3 RI = new DMatrix3();
            RI.setIdentity();
            final Vector3 ss = new Vector3(0.02f,0.02f,0.02f);
            for (i=0; i<numc; i++) {
                DJoint c = OdeHelper.createContactJoint (world,contactgroup,contacts.get(i));
                c.attach (b1,b2);
                if (show_contacts) {
                    model = modelBuilder.createBox(ss.x, ss.y, ss.z, GL20.GL_LINES,
                        new Material(ColorAttribute.createDiffuse(Color.RED)),
                        VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

                    ModelInstance m = new ModelInstance(model);
                    m.transform.setToTranslation((float)contacts.get(i).geom.pos.get0(),
                        (float)contacts.get(i).geom.pos.get1(),
                        (float)contacts.get(i).geom.pos.get2());
                    modelBatch.render(new ModelInstance(model));

                    //dsDrawBox (contacts.get(i).geom.pos,RI,ss);  // original draw
                }
            }
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

    private void odeDispose() {
        // ode cleanup
        ragdoll.destroy();
        contactgroup.destroy();
        space.destroy();
        world.destroy();
        OdeHelper.closeODE();
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.SPACE:
                ragdoll.getBones().get(DxDefaultHumanRagdollConfig.PELVIS).getBody().setLinearVel(10, 80, 10);
                break;
            case Input.Keys.F1:
                odeDispose();
                Ode4libGDX.game.setScreen(new DemoTriMeshHeightFieldScreen());
                break;
        }
        return false;
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
        if (Gdx.app.getType().equals(Application.ApplicationType.Android))
            ragdoll.getBones().get(DxDefaultHumanRagdollConfig.PELVIS).getBody().setLinearVel(10, 80, 10);
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
            scene.cam.position.set(-5, 5, -5);
            scene.cam.lookAt(0,5,0);
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
