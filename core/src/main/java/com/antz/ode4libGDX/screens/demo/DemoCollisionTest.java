package com.antz.ode4libGDX.screens.demo;

import com.antz.ode4libGDX.Ode4libGDX;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import org.ode4j.Ode2GdxMathUtils;
import org.ode4j.math.DQuaternion;
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
import java.nio.Buffer;
import java.nio.FloatBuffer;

public class DemoCollisionTest implements Screen {

    private Texture texture;
    private PerspectiveCamera camera;
    private Frustum camFrustum;
    private Environment environment;
    private Mesh mesh;
    private ModelBatch batch;
    private SpriteBatch batch2D;
    private BitmapFont font;
    private Renderable renderable;
    private FloatBuffer offsets;
    private Quaternion q;
    private Matrix4 mat4;
    private Vector3 vec3Temp;
    private float logicTimer = 0, size;
    private long physicsTime, startTime, updateTime, renderTime, nearCallBackTotal, nearCallbackStart;
    private int logicIndex, targetIndex, instanceUpdated;
    private String version;

    // some constants
    private static final int   ITERS = 3;		                    // number of iterations
    private static final float WALLMASS = 1;	                    // wall box mass
    private static final float WBOXSIZE = 1;		                // size of wall boxes
    private static final int   WALLWIDTH = 5;		                // width of wall
    private static final int   WALLHEIGHT = 40;		                // height of wall
    private static final int   WALLDEPTH = 5;		                // depth of wall
    private static int INSTANCE_COUNT = WALLWIDTH * WALLHEIGHT * WALLDEPTH;

    private static final float DISABLE_THRESHOLD = 0.008f;	// maximum velocity (squared) a body can have and be disabled
    private static final float DISABLE_STEPS = 3;	        // number of steps a box has to have been disable-able before it will be disabled

    // dynamics and collision objects
    private static DWorld world;
    private static DSpace space;
    private static int bodies;
    private static DJointGroup contactGroup;
    private DMass m;
    private static final int MAX_CONTACTS = 1;
    private static final DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);

    private static DBox[] wall_boxes;
    private static DBody[] wall_bodies;
    private static int[] wb_stepsdis;
    private static int wb;

    @Override
    public void show() {
        init();
        initPhysics();
        setupInstancedMesh();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.WHITE,true);

        logicTimer += Gdx.graphics.getDeltaTime();

        // GWT issue.  You need to draw before updating the instanced data
        if (logicTimer < 0.5f) {
            draw();
        } else if (logicTimer > 15) { // switch to next demo after 15s
            ShaderProgram.prependVertexCode = "";
            ShaderProgram.prependFragmentCode = "";
            Ode4libGDX.game.setScreen(new DemoDynamicCharacterScreen());
        } else { // main loop
            checkInput();
            simLoop();
            draw();
        }
    }

    private void checkInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)){
            ShaderProgram.prependVertexCode = "";
            ShaderProgram.prependFragmentCode = "";
            Ode4libGDX.game.setScreen(new DemoDynamicCharacterScreen());
        }
    }

    // simulation loop
    private void simLoop()  {
        instanceUpdated = 0;

        startTime = TimeUtils.nanoTime();
        space.collide(null, nearCallback);
        world.quickStep(0.02);
        contactGroup.empty();
        boolean disable;

        for (int i = 0; i < wb; i++) {
            DBody b = wall_boxes[i].getBody();
            if (b.isEnabled()) {
                disable = true;

                if (b.getLinearVel().lengthSquared() > DISABLE_THRESHOLD) disable = false;
                if (b.getAngularVel().lengthSquared() > DISABLE_THRESHOLD) disable = false;

                if (disable)
                    wb_stepsdis[i]++;
                else
                    wb_stepsdis[i] = 0;

                if (wb_stepsdis[i] > DISABLE_STEPS)
                    b.disable();
            }
            vec3Temp.set(
                (float)wall_boxes[i].getPosition().get0(),
                (float)wall_boxes[i].getPosition().get1(),
                (float)wall_boxes[i].getPosition().get2());

            if (!camFrustum.sphereInFrustum(vec3Temp, 2f)) continue;

            targetIndex = i * 16; // each instance uses 16 floats for matrix4
            q.set(Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion()));
            mat4.set(vec3Temp,q);

            // update float buffer and update the mesh instance data
            offsets.position(targetIndex);
            offsets.put(mat4.getValues());

            renderable.meshPart.mesh.updateInstanceData(targetIndex, mat4.getValues()); // use this if you use culling
        }
        //renderable.meshPart.mesh.updateInstanceData(0, offsets); // if no culling done then you can use this, but comment out above
        physicsTime = TimeUtils.timeSinceNanos(startTime);
    }

    private void draw() {
        camera.update();
        startTime = TimeUtils.nanoTime();
        texture.bind();
        batch.begin(camera);
        batch.render(renderable);
        batch.end();
        renderTime = TimeUtils.timeSinceNanos(startTime);

        // 2D stuff for info text
        batch2D.begin();
        font.draw(batch2D,"FPS: " + Gdx.graphics.getFramesPerSecond(), 10, 20);
        font.draw(batch2D,"ODE4J Version: " + version, 10,40);
        font.draw(batch2D,"Boxes: " + WALLWIDTH + "x" + WALLHEIGHT + "x" + WALLDEPTH + " = " + INSTANCE_COUNT, 10, 60);
        font.draw(batch2D,"NearCallback Time: " + TimeUtils.nanosToMillis(nearCallBackTotal) + "ms", 10, 80);
        font.draw(batch2D,"Total Physics Time: " + TimeUtils.nanosToMillis(physicsTime) + "ms", 10, 100);
        font.draw(batch2D,"Render Time: " + TimeUtils.nanosToMillis(renderTime) + "ms", 10, 120);
        font.draw(batch2D,"Press F1 for Dynamic Character Screen Demo.", 10, 140);
        font.draw(batch2D,"Standard Collision Test", 10, 180);
        batch2D.end();
        nearCallBackTotal = 0;
    }

    private void initPhysics() {
        OdeHelper.initODE2(0);
        version = OdeHelper.getVersion();

        // recreate world
        world = OdeHelper.createWorld();
        space = OdeHelper.createSapSpace2(DSapSpace.AXES.XZY,0);
        m = OdeHelper.createMass();

        contactGroup = OdeHelper.createJointGroup();
        world.setGravity(0,-1.5,0);
        world.setCFM(1e-5);
        world.setERP(0.8);
        world.setQuickStepNumIterations(ITERS);

        OdeHelper.createPlane(space,0,1,0,0);
        bodies = 0;
        wb = 0;
    }

    private void setupInstancedMesh() {
        size = WBOXSIZE/2f;

        // Create a 3D cube mesh
        mesh = new Mesh(true, 24, 36,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoords0")
        );

        // 24 vertices - one of the texture coordinates is flipped, but no big deal
        float[] vertices = new float[] {
            -size, size, -size, 0.0f, 1.0f,
            size, size, -size, 1.0f, 1.0f,
            size, -size, -size, 1.0f, 0.0f,
            -size, -size, -size, 0.0f, 0.0f,
            size, size, size, 1.0f, 1.0f,
            -size, size, size, 0.0f, 1.0f,
            -size, -size, size, 0.0f, 0.0f,
            size, -size, size, 1.0f, 0.0f,
            -size, size, size, 1.0f, 1.0f,
            -size, size, -size, 0.0f, 1.0f,
            -size, -size, -size, 0.0f, 0.0f,
            -size, -size, size, 1.0f, 0.0f,
            size, size, -size, 1.0f, 1.0f,
            size, size, size, 0.0f, 1.0f,
            size, -size, size, 0.0f, 0.0f,
            size, -size, -size, 1.0f, 0.0f,
            -size, size, size, 1.0f, 1.0f,
            size, size, size, 0.0f, 1.0f,
            size, size, -size, 0.0f, 0.0f,
            -size, size, -size, 1.0f, 0.0f,
            -size, -size, -size, 1.0f, 1.0f,
            size, -size, -size, 0.0f, 1.0f,
            size, -size, size, 0.0f, 0.0f,
            -size, -size, size, 1.0f, 0.0f
        };

        // 36 indices
        short[] indices = new short[]
            {0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4, 8, 9, 10, 10, 11, 8, 12, 13,
                14, 14, 15, 12, 16, 17, 18, 18, 19, 16, 20, 21, 22, 22, 23, 20 };

        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        // Thanks JamesTKhan for saving me hours: how to pass a Matrix4 to the shader (using 4 x Vec4 = 16 floats)
        mesh.enableInstancedRendering(true, INSTANCE_COUNT,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3));

        // Create offset FloatBuffer that will hold matrix4 for each instance to pass to shader
        offsets = BufferUtils.newFloatBuffer(INSTANCE_COUNT * 16); // 16 floats for mat4

        setupSimulation();

        ((Buffer)offsets).position(0);
        mesh.setInstanceData(offsets);

        renderable = new Renderable();
        renderable.meshPart.set("Cube", mesh, 0, 36, GL20.GL_TRIANGLES); // 36 indices
        renderable.environment = environment;
        renderable.worldTransform.idt();
        renderable.shader = createShader(); // see method for more details
        renderable.shader.init();
    }

    private void setupSimulation() {
        wall_boxes = new DBox[INSTANCE_COUNT];
        wall_bodies = new DBody[INSTANCE_COUNT];
        wb_stepsdis = new int[INSTANCE_COUNT];

        for (int i = 0; i < WALLWIDTH*WALLHEIGHT; i++) wb_stepsdis[i] = 0;

        // Wall boxes
        for (float y = WBOXSIZE/2f; y < WALLHEIGHT; y += WBOXSIZE) {
            for (float x = -WALLWIDTH/2f; x < WALLWIDTH/2f; x += WBOXSIZE) {
                for (float z = -WALLWIDTH/2f; z < WALLWIDTH/2f; z += WBOXSIZE) {
                    createBox(new Vector3(100.5f + x *2, y*2 + 15 , 100 + z *2));
                }
            }
        }
    }

    /** See assets/shaders/instanced.vert + assets/shaders/instanced.frag files to see how:

     a_position
     a_texCoords0
     i_worldTrans

     vertex attributes are used to update each instance.

     u_projViewTrans uniform needs to be set with camera.combined
     so shader can calculate the updated position and rotation
     */
    private BaseShader createShader() {
        return new BaseShader() {

            @Override
            public void begin(Camera camera, RenderContext context) {
                program.bind();
                program.setUniformMatrix("u_projViewTrans", camera.combined);
                program.setUniformi("u_texture", 0);
                context.setDepthTest(GL30.GL_LEQUAL);
            }

            @Override
            public void init () {
                ShaderProgram.prependVertexCode = "#version 300 es\n";
                ShaderProgram.prependFragmentCode = "#version 300 es\n";
                program = new ShaderProgram(Gdx.files.internal("shaders/instanced.vert"),
                    Gdx.files.internal("shaders/instanced.frag"));
                if (!program.isCompiled()) {
                    throw new GdxRuntimeException("Shader compile error: " + program.getLog());
                }
                init(program, renderable);
            }

            @Override
            public int compareTo (Shader other) {
                return 0;
            }

            @Override
            public boolean canRender (Renderable instance) {
                return true;
            }
        };
    }

    private void createBox(Vector3 position){
        wall_bodies[wb] = OdeHelper.createBody(world);
        wall_bodies[wb].setPosition(position.x, position.y, position.z);

        m.setBox(1,WBOXSIZE,WBOXSIZE,WBOXSIZE);
        m.adjust(WALLMASS);
        wall_bodies[wb].setMass(m);
        wall_bodies[wb].enable();
        wall_boxes[wb] = OdeHelper.createBox(space,WBOXSIZE,WBOXSIZE,WBOXSIZE);

        wall_boxes[wb].setBody(wall_bodies[wb]);
        wall_boxes[wb].setQuaternion(DQuaternion.fromEuler(
            MathUtils.random(180)-270,
            MathUtils.random(180)-270f,
            MathUtils.random(180)-270f
            ));

        q.set(Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[wb].getQuaternion()));

        // create matrix transform
        mat4.set(position, q);

        // put the 16 floats for mat4 in the float buffer
        offsets.put(mat4.getValues());
        wb++;
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
        shutdownSimulation();

        font.dispose();
        batch.dispose();
        batch2D.dispose();
        mesh.dispose();
        offsets.clear();

        contactGroup.destroy();
        space.destroy();
        world.destroy();
        OdeHelper.closeODE();
    }

    private final DGeom.DNearCallback nearCallback = (data, o1, o2) -> nearCallback(o1, o2);

    private void nearCallback (DGeom o1, DGeom o2) {
        nearCallbackStart = TimeUtils.nanoTime();

        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && OdeHelper.areConnected(b1, b2)) {
            nearCallBackTotal += TimeUtils.timeSinceNanos(nearCallbackStart);
            return;
        }

        int i,n;
        //contacts = new DContactBuffer(N);
        contacts.nullify(); // less GC

        n = OdeHelper.collide(o1,o2, MAX_CONTACTS,contacts.getGeomBuffer());
        if (n > 0) {
            for (i=0; i<n; i++) {
                DContact contact = contacts.get(i);
                contact.surface.mu = 0.5;

                DJoint c = OdeHelper.createContactJoint(world, contactGroup, contact);
                c.attach(o1.getBody(), o2.getBody());
            }
        }
        nearCallBackTotal += TimeUtils.timeSinceNanos(nearCallbackStart);
    }

    private void shutdownSimulation() {
        // destroy world if it exists
        if (bodies!=0)  {
            contactGroup.destroy();
            space.destroy();
            world.destroy();
            bodies = 0;
        }
        wb = 0;
        offsets.clear();
    }

    private void init() {
        texture = new Texture(Gdx.files.internal("graphics/zebra.png")); // our mascot!

        // Loading complete, load a scene.
        camera = new PerspectiveCamera(45, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 200;
        camera.position.set(100 - WALLWIDTH*4,17,100 - WALLWIDTH*4);
        camera.lookAt(100,1,100);
        camera.up.set(Vector3.Y);
        camera.update();
        camFrustum = camera.frustum;

        // reusable variables
        mat4 = new Matrix4();
        q = new Quaternion();
        vec3Temp = new Vector3();
        logicTimer = 0;
        logicIndex = 0;

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.9f, 0.9f, 0.9f, 1f));

        // batches
        batch = new ModelBatch();
        batch2D = new SpriteBatch();

        // until they fix the default font, load the fixed version locally
        font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
        font.setColor(Color.RED);
    }
}
