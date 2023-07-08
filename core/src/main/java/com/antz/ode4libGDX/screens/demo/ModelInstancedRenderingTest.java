package com.antz.ode4libGDX.screens.demo;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.graphics.g3d.utils.RenderContext;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.profiling.GLProfiler;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;

public class ModelInstancedRenderingTest implements Screen {

    private Mesh mesh;
    private ModelBatch batch;
    private GLProfiler profiler;
    private SpriteBatch batch2D;
    private BitmapFont font;
    private PerspectiveCamera camera;
    private FirstPersonCameraController controller;
    private Frustum camFrustum;
    private Renderable renderable;
    private FloatBuffer offsets;
    private Quaternion q;
    private Matrix4 mat4;
    private Vector3 vec3Temp;
    private float[] floatTemp;

    private int targetIndex, instanceUpdated;
    private long startTime, updateTime, renderTime;
    private float size;
    private boolean rotateOn = false, showStats = true;

    private static int INSTANCE_COUNT_SIDE;
    private static int INSTANCE_COUNT;
    private static float CULLING_FACTOR;  // distance from camera

    @Override
    public void show() {
        // Check if for GL30 profile
        if (Gdx.gl30 == null) {
            throw new GdxRuntimeException("GLES 3.0 profile required for this test");
        }

        init();
        setupInstancedMesh();
    }

    @Override
    public void render(float delta) {
        profiler.reset();
        controller.update();
        ScreenUtils.clear(Color.BLACK, true);

        // rotate all instances that are close and in view
        startTime = TimeUtils.nanoTime();
        update(delta);
        updateTime = TimeUtils.timeSinceNanos(startTime);

        // draw all instances
        startTime = TimeUtils.nanoTime();
        batch.begin(camera);
        batch.render(renderable);
        batch.end();
        renderTime = TimeUtils.timeSinceNanos(startTime);

        // 2D stuff for info text
        if (showStats) {
            batch2D.begin();
            drawStats();
            batch2D.end();
        }
    }

    private void drawStats() {
        font.draw(batch2D,"FPS: " + Gdx.graphics.getFramesPerSecond() +
            "  Draw Calls: " + profiler.getDrawCalls() +
                "  Vert Count: " + profiler.getVertexCount().latest +
                "  Shader Switches: " + profiler.getShaderSwitches() +
                "  Texture Bindings: " + profiler.getTextureBindings(),
            10, 40);
        font.draw(batch2D,"3D Cubes: " + INSTANCE_COUNT + "  Updated: " + instanceUpdated + "   Skipped: " + (INSTANCE_COUNT - instanceUpdated), 10, 80);
        font.draw(batch2D,"Update Time: " + TimeUtils.nanosToMillis(updateTime) + "ms", 10, 120);
        font.draw(batch2D,"Render Time: " + TimeUtils.nanosToMillis(renderTime) + "ms", 10, 160);
        font.draw(batch2D,"F1: Toggle Stats, SPACE: Toggle rotation. Rotation=" + rotateOn, 10, 200);
    }

    private void update(float delta) {
        instanceUpdated = 0;

        // toggle rotation if space key pressed
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            rotateOn = !rotateOn;

        // toggle show stats if F1 key pressed
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1))
            showStats = !showStats;

        if (!rotateOn) return;

        // Everything you do in this loop will impact performance with high INSTANCE_COUNT
        for (int x = 0; x < INSTANCE_COUNT; x++) {
            targetIndex = x * 20; // each instance uses 20 floats

            // get position of instance (x, y, z)
            vec3Temp.set(offsets.get(targetIndex + 12), offsets.get(targetIndex + 13), offsets.get(targetIndex + 14));

            // attempt culling if not within camera's frustum, or too far away to be noticed rotating
            if (!(camFrustum.pointInFrustum(vec3Temp)) || vec3Temp.dst(camera.position) > CULLING_FACTOR) continue;

            instanceUpdated++;

            // TODO: maybe we can use the other get() methods?
            // Get the maxtrix4
            floatTemp[0] = offsets.get(targetIndex);
            floatTemp[1] = offsets.get(targetIndex + 1);
            floatTemp[2] = offsets.get(targetIndex + 2);
            floatTemp[3] = offsets.get(targetIndex + 3);
            floatTemp[4] = offsets.get(targetIndex + 4);
            floatTemp[5] = offsets.get(targetIndex + 5);
            floatTemp[6] = offsets.get(targetIndex + 6);
            floatTemp[7] = offsets.get(targetIndex + 7);
            floatTemp[8] = offsets.get(targetIndex + 8);
            floatTemp[9] = offsets.get(targetIndex + 9);
            floatTemp[10] = offsets.get(targetIndex + 10);
            floatTemp[11] = offsets.get(targetIndex + 11);
            floatTemp[12] = vec3Temp.x; // saves time
            floatTemp[13] = vec3Temp.y; // saves time
            floatTemp[14] = vec3Temp.z; // saves time
            floatTemp[15] = offsets.get(targetIndex + 15);

            mat4.set(floatTemp);

            // spin every other cube differently - use just one for slight speed up
            if (x % 2 == 0)
                mat4.rotate(Vector3.X, 45 * delta);
            else
                mat4.rotate(Vector3.Y, 45 * delta);

            // update float buffer and update the mesh instance data
            offsets.position(targetIndex);
            offsets.put(mat4.getValues());
            // This next method only works on desktop
            renderable.meshPart.mesh.updateInstanceData(targetIndex, mat4.getValues());

        }
        // This is the only method that works in GWT and is expensive
        //renderable.meshPart.mesh.updateInstanceData(0, offsets);
    }

    private void setupInstancedMesh() {
        // Create a 3D cube mesh
        mesh = new Mesh(true, 24, 36,
            new VertexAttribute(Usage.Position, 3, "a_position"));
        size = 1f / (float)Math.sqrt(INSTANCE_COUNT) * 0.5f;

        float[] vertices = new float[] {
            -size, -size, -size,    // 0
            size, -size, -size,     // 1
            size, size, -size,      // 2
            -size, size, -size,     // 3
            -size, -size, size,     // 4
            size, -size, size,      // 5
            size, size, size,       // 6
            -size, size, size       // 7
        };

        short[] indices = new short[]
               {0, 1, 2, 2, 3, 0,
                5, 4, 7, 7, 6, 5,
                0, 3, 7, 7, 4, 0,
                5, 6, 2, 2, 1, 5,
                5, 1, 0, 0, 4, 5,
                2, 6, 7, 7, 3, 2};

        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        // Thanks JamesTKhan for saving me hours: how to pass a Matrix4 to the shader (using 4 x Vec4 = 16 floats)
        mesh.enableInstancedRendering(true, INSTANCE_COUNT,
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 0),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 1),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 2),
            new VertexAttribute(VertexAttributes.Usage.Generic, 4, "i_worldTrans", 3),
            new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "i_color"));

        // Create offset FloatBuffer that will hold matrix4 and color for each instance to pass to shader
        offsets = BufferUtils.newFloatBuffer(INSTANCE_COUNT * 20); // 16 mat4 + 4 color = 20 floats
        for (int x = 1; x <= INSTANCE_COUNT_SIDE; x++) {
            for (int y = 1; y <= INSTANCE_COUNT_SIDE; y++) {
                for (int z = 1; z <= INSTANCE_COUNT_SIDE; z++) {

                    // set instance position
                    vec3Temp.set(
                        x / (INSTANCE_COUNT_SIDE * 0.5f) - 1f,
                        y / (INSTANCE_COUNT_SIDE * 0.5f) - 1f,
                        z / (INSTANCE_COUNT_SIDE * 0.5f) - 1f);

                    // set random rotation
                    q.setEulerAngles(MathUtils.random(-90, 90), MathUtils.random(-90, 90), MathUtils.random(-90, 90));

                    // create matrix transform
                    mat4.set(vec3Temp, q);

                    // put the 20 floats in the float buffer
                    offsets.put(mat4.getValues());  // put matrix4 = 16
                    offsets.put(new float[] {       // add color rgba = 4
                        x / (float) INSTANCE_COUNT_SIDE,
                        y / (float) INSTANCE_COUNT_SIDE,
                        z / (float) INSTANCE_COUNT_SIDE,
                        1f});
                }
            }
        }
        ((Buffer)offsets).position(0);
        mesh.setInstanceData(offsets);

        renderable = new Renderable();
        renderable.material = new Material();
        renderable.meshPart.set("Cube", mesh, 0, 36, GL20.GL_TRIANGLES); // 36 indices
        renderable.worldTransform.idt();
        renderable.shader = createShader(); // see method for more details
        renderable.shader.init();
    }

    private BaseShader createShader() {
        return new BaseShader() {

            @Override
            public void begin(Camera camera, RenderContext context) {
                program.bind();
                program.setUniformMatrix("u_projViewTrans", camera.combined);
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

    private void init() {
        // reusable variables
        mat4 = new Matrix4();
        q = new Quaternion();
        vec3Temp = new Vector3();
        floatTemp = new float[16];

        // setup camera, controller, and batches
        camera = new PerspectiveCamera(45, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.001f;
        camera.far = 2f;
        camera.position.set(0,0,0);
        camera.direction.set(Vector3.Z);
        camera.up.set(Vector3.Y);
        camera.update();
        camFrustum = camera.frustum;

        controller = new FirstPersonCameraController(camera);
        controller.setVelocity(0.025f);
        controller.setDegreesPerPixel(0.2f);
        Gdx.input.setInputProcessor(controller);

        batch = new ModelBatch();
        batch2D = new SpriteBatch();
        // until they fix the default font, load the fixed version locally
        font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
        font.setColor(Color.WHITE);
        font.getData().setScale(2);

        // set instance limits for all other platforms
        if (Gdx.app.getType().equals(Application.ApplicationType.Desktop)) {
            INSTANCE_COUNT_SIDE = 100;
            CULLING_FACTOR = camera.far * 0.25f; // cull very small cubes that we cant detect rotating
        } else {
            INSTANCE_COUNT_SIDE = 25;
            CULLING_FACTOR = camera.far; // no culling as all objects can be seen rotating
        }

        // 100 * 100 * 100 = 1 million
        INSTANCE_COUNT = INSTANCE_COUNT_SIDE * INSTANCE_COUNT_SIDE * INSTANCE_COUNT_SIDE;

        // create & enable the profiler
        profiler = new GLProfiler(Gdx.graphics);
        profiler.enable();
    }

    @Override
    public void resize(int width, int height) {

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
    public void dispose () {
        mesh.dispose();
        batch.dispose();
        batch2D.dispose();
        font.dispose();
        renderable.shader.dispose();
        renderable.meshPart.mesh.dispose();
    }
}
