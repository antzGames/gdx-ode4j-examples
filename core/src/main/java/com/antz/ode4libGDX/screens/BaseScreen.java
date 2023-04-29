package com.antz.ode4libGDX.screens;

import com.antz.ode4libGDX.controllers.camera.CameraController;
import com.antz.ode4libGDX.util.OdeEntity;
import com.antz.ode4libGDX.util.OdePhysicsSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DAABBC;

/**
 * Original code from: https://github.com/JamesTKhan/libgdx-bullet-tutorials
 * @author JamesTKhan
 * @version October 04, 2022
 *
 * modified to work on odej4 by:
 * Antz
 * April 27, 2023
 */
public class BaseScreen extends ScreenAdapter {
    protected PerspectiveCamera camera;
    protected CameraController cameraController;
    protected ModelBatch modelBatch;
    protected SpriteBatch batch2D;
    protected ModelBatch shadowBatch;

    protected Model model;
    protected ModelInstance modelInstance;
    protected ModelBuilder modelBuilder;

    public static Array<ModelInstance> renderInstances;
    protected Environment environment;
    protected DirectionalShadowLight shadowLight;
    protected OdePhysicsSystem odePhysicsSystem;

    private final Array<Color> colors;
    protected BitmapFont font = new BitmapFont();
    protected String info;


    public BaseScreen() {
        Gdx.input.setCatchKey(Input.Keys.SPACE, true);
        Gdx.input.setCatchKey(Input.Keys.F1, true);
        odePhysicsSystem = new OdePhysicsSystem();
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 500;
        camera.position.set(0,10, 50f);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add((shadowLight = new DirectionalShadowLight(2048, 2048, 30f, 30f, 1f, 100f)).set(0.8f, 0.8f, 0.8f, -.4f, -.4f, -.4f));
        environment.shadowMap = shadowLight;

        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        shadowBatch = new ModelBatch(new DepthShaderProvider());
        batch2D = new SpriteBatch();
        renderInstances = new Array<>();

        colors = new Array<>();
        colors.add(Color.PURPLE);
        colors.add(Color.BLUE);
        colors.add(Color.TEAL);
        colors.add(Color.BROWN);
        colors.add(Color.FIREBRICK);
    }

    @Override
    public void render(float delta) {
        odePhysicsSystem.update(delta);
        cameraController.update(delta);

        ScreenUtils.clear(Color.BLACK, true);

        shadowLight.begin(Vector3.Zero, camera.direction);
        shadowBatch.begin(shadowLight.getCamera());
        shadowBatch.render(renderInstances);
        shadowBatch.end();
        shadowLight.end();

        modelBatch.begin(camera);
        modelBatch.render(renderInstances, environment);
        //modelBatch.render(odePhysicsSystem.obj.get(2).modelInstance, environment);
        modelBatch.end();

        //render AABB boxes
        //renderAABB();

        // 2D stuff for info text
        batch2D.begin();
        font.draw(batch2D, info + "FPS:" + Gdx.graphics.getFramesPerSecond(), 10, 115);
        batch2D.end();
    }

    public void renderAABB() {
        modelBatch.begin(camera);
        for (OdeEntity o: odePhysicsSystem.obj){
            if (o.geom[0] == null) continue;
            DAABBC aabb = o.geom[0].getAABB();
            DVector3 bbpos = aabb.getCenter();
            DVector3 bbsides = aabb.getLengths();

            model = modelBuilder.createBox((float)bbsides.get0(), (float)bbsides.get1(), (float)bbsides.get2(), GL20.GL_LINES,
                new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
            modelInstance = new ModelInstance(model);
            modelInstance.transform.set(new Matrix3().idt());
            modelInstance.transform.setTranslation((float)bbpos.get0(), (float)bbpos.get1(), (float)bbpos.get2());
            modelBatch.render(modelInstance);
        }
        modelBatch.end();
    }

    public void setCameraController(CameraController cameraController) {
        this.cameraController = cameraController;
        Gdx.input.setInputProcessor(cameraController);
    }

    protected Color getRandomColor(){
        return colors.get(MathUtils.random(0, colors.size-1));
    }

    @Override
    public void dispose() {
        // Destroy screen's assets here.
        model.dispose();
        modelBatch.dispose();
        shadowBatch.dispose();
        batch2D.dispose();
        font.dispose();

        // ode cleanup
        odePhysicsSystem.dispose();
    }
}
