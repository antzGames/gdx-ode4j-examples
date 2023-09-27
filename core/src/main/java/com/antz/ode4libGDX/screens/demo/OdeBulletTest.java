package com.antz.ode4libGDX.screens.demo;

import com.antz.ode4libGDX.Ode4libGDX;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.ode4j.Ode2GdxMathUtils;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSapSpace;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

import static org.ode4j.ode.OdeConstants.*;


public class OdeBulletTest implements Screen {

    // some constants
    private static final int   ITERS = 5;		            // number of iterations
    private static final float DISABLE_THRESHOLD = 0.008f;	// maximum velocity (squared) a body can have and be disabled
    private static final float DISABLE_STEPS = 5;	        // number of steps a box has to have been disable-able before it will be disabled

    // dynamics and collision objects
    DWorld world;
    DSpace space;
    DJointGroup contactGroup;
    int wb;

    int totalBoxes = 500;

    ModelInstance[] boxes = new ModelInstance[totalBoxes];
    DBox[] wall_boxes = new DBox[totalBoxes];
    DBody[] wall_bodies = new DBody[totalBoxes];
    int[] wb_stepsdis = new int[totalBoxes];
    Quaternion q = new Quaternion();

    DMass m;

    PerspectiveCamera camera;

    ScreenViewport viewport;
    ScreenViewport guiViewport;

    ModelInstance ground;

    ModelBatch modelBatch;

    SpriteBatch batch;

    Environment environment;
    BitmapFont font;
    Model boxModel;

    long timeNow;
    long time;
    boolean freeze = false;

    CameraInputController cameraController;
    ModelBuilder builder = new ModelBuilder();

    @Override
    public void show() {
        wb = 0;
        OdeHelper.initODE2(0);

        camera = new PerspectiveCamera();
        viewport = new ScreenViewport(camera);
        guiViewport = new ScreenViewport();

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1.f));
        DirectionalLight set = new DirectionalLight().set(1.0f, 1.0f, 1.0f, -1f, -1f, -0.4f);
        environment.add(set);

        batch = new SpriteBatch();

        camera.position.z = 43;
        camera.position.y = 2;

        camera.lookAt(0, 0, 0);

        final Texture texture = new Texture(Gdx.files.internal("graphics/badlogic.jpg"));
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        final Material material = new Material(TextureAttribute.createDiffuse(texture),
            FloatAttribute.createShininess(4f));

        boxModel = builder.createBox(1, 1, 1, material, Usage.Position | Usage.Normal | Usage.TextureCoordinates);

        resetSimulation();

        font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
        font.setColor(1, 0, 0, 1);
        time = System.currentTimeMillis();

        cameraController = new CameraInputController(camera);
        cameraController.autoUpdate = false;
        cameraController.forwardTarget = false;
        cameraController.translateTarget = false;

        Gdx.input.setInputProcessor(cameraController);
    }

    public ModelInstance createBox(String userData, boolean add, float mass, float x, float y, float z, float axiX, float axiY, float axiZ, Model model, float x1, float y1, float z1, float colorR, float colorG, float colorB) {
        ModelInstance modelInstance = new ModelInstance(model);

        ColorAttribute attr = ColorAttribute.createDiffuse(colorR, colorG, colorB, 1);
        modelInstance.materials.get(0).set(attr);

        modelInstance.transform.translate(x, y, z);

        modelInstance.transform.rotate(Vector3.X, axiX);
        modelInstance.transform.rotate(Vector3.Y, axiY);
        modelInstance.transform.rotate(Vector3.Z, axiZ);

        if (add){
            wall_bodies[wb] = OdeHelper.createBody(world);
            m.setBox(1, x1, y1, z1);
            m.adjust(mass);
            wall_bodies[wb].setMass(m);
            wall_boxes[wb] = OdeHelper.createBox(space, x1, y1, z1);
            wall_boxes[wb].setBody(wall_bodies[wb]);
            wall_boxes[wb].setPosition(x,y,z);
            wall_boxes[wb].setQuaternion(new DQuaternion(q.w, q.x, q.y, q.z));
        }

        return modelInstance;
    }

    public void resetSimulation() {
        // recreate world
        world = OdeHelper.createWorld();
        space = OdeHelper.createSapSpace2(DSapSpace.AXES.XZY,0 );
        m = OdeHelper.createMass();

        contactGroup = OdeHelper.createJointGroup();
        world.setGravity(0,-9.8,0);
        world.setCFM(1e-5);
        world.setERP(0.8);
        world.setQuickStepNumIterations(ITERS);

        OdeHelper.createPlane(space,0,1,0,0);

        float r = 1;
        float g = 1;
        float b = 1;
        float a = 1;

        float groundWidth = 600f;
        float groundHeight = 1f;
        float groundDepth = 600f;
        Model groundBox = builder.createBox(groundWidth, groundHeight, groundDepth, new Material(ColorAttribute.createDiffuse(r, g, b, a), ColorAttribute.createSpecular(r, g, b, a), FloatAttribute.createShininess(16f)), Usage.Position | Usage.Normal);
        ground = createBox("ground", false, 0, 0, -0.5f, 0, 0, 0, 0, groundBox, groundWidth, groundHeight, groundDepth, 0, 0, 1);

        int count = 0;
        wb = 0;

        int offsetY = 15;

        for(int i = 0; i < totalBoxes; i++) {
            ModelInstance createBox = null;
            float x = MathUtils.random(-5.0f, 5.0f);
            float y = MathUtils.random(offsetY + 4f, offsetY + 9f);
            float z = MathUtils.random(-5.0f, 5.0f);
            float axisX = MathUtils.random(0, 360);
            float axisY = MathUtils.random(0, 360);
            float axisZ = MathUtils.random(0, 360);

            boxes[wb] = createBox("ID: " + count, true, 0.4f, x, y, z, axisX, axisY, axisZ, boxModel, 1, 1, 1, r, g, b);
            count++;
            wb++;
        }
    }

    @Override
    public void render(float delta) {
        doInput();
        camera.update();
        Gdx.gl.glClearColor(0.9f, 0.9f, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if(freeze == false) {
            timeNow = System.currentTimeMillis();

            if(timeNow - time > 8000) {
                shutdownSimulation();
                resetSimulation();
                time = System.currentTimeMillis();
            }
            simLoop();
        }

        modelBatch.begin(camera);
        for (int x = 0; x < boxes.length; x++) {
            modelBatch.render(boxes[x], environment);
        }
        modelBatch.render(ground, environment);
        modelBatch.end();

        batch.begin();
        font.draw(batch, "Bullet test on ODE4J" +
            "\n\nFPS: " + Gdx.graphics.getFramesPerSecond() +
            "\nTotal Boxes: " + totalBoxes +
            "\nInputs: F2 for next demo" +
            "\nHold Left/Right mouse to manipulate camera", 30, 120);
        batch.end();
    }


    // simulation loop
    private void simLoop ()  {

        space.collide(null,nearCallback);
        world.quickStep(0.05);
        contactGroup.empty();

        for (int i = 0; i < wb; i++) {
            DBody b = wall_boxes[i].getBody();
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

            q = Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion());
            boxes[i].transform.set(new Vector3((float)wall_boxes[i].getPosition().get0(), (float)wall_boxes[i].getPosition().get1(), (float)wall_boxes[i].getPosition().get2()), q);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, false);
        guiViewport.update(width, height, true);
        Camera guiCam = guiViewport.getCamera();
        guiCam.update();
        batch.setProjectionMatrix(guiCam.combined);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        shutdownSimulation();

        contactGroup.destroy();
        space.destroy();
        world.destroy();
        OdeHelper.closeODE();
    }

    private void shutdownSimulation() {
        // destroy world if it exists
        if (wb != 0) {
            contactGroup.destroy();
            space.destroy();
            world.destroy();
            wb = 0;
        }
    }

    public void doInput() {
        if(Gdx.input.isKeyJustPressed(Keys.F2)) {
            shutdownSimulation();
            Ode4libGDX.game.setScreen(new DemoCollisionTest());
        }
    }

    private final DGeom.DNearCallback nearCallback = (data, o1, o2) -> nearCallback(o1, o2);

    private void nearCallback (DGeom o1, DGeom o2) {
        int i,n;

        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && OdeHelper.areConnected(b1, b2))
            return;

        final int N = 4;
        DContactBuffer contacts = new DContactBuffer(N);
        n = OdeHelper.collide(o1,o2,N,contacts.getGeomBuffer());
        if (n > 0) {
            for (i=0; i<n; i++) {
                DContact contact = contacts.get(i);
                contact.surface.mode = dContactSlip1 | dContactBounce |dContactSlip2 | dContactSoftERP | dContactSoftCFM | dContactApprox1;
                contact.surface.mu = 0.9;
                contact.surface.slip1 = 0.1;
                contact.surface.bounce = 1;
                contact.surface.bounce_vel = 0.01;
                contact.surface.slip2 = 0.1;
                contact.surface.soft_erp = 0.8;
                contact.surface.soft_cfm = 0.1;
                DJoint c = OdeHelper.createContactJoint(world,contactGroup,contact);
                c.attach (o1.getBody(), o2.getBody());
            }
        }
    }

}
