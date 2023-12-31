package com.antz.ode4libGDX.screens.demo;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.controllers.camera.ThirdPersonCameraController;
import com.antz.ode4libGDX.controllers.character.DynamicCharacterController;
import com.antz.ode4libGDX.util.OdeEntity;
import com.antz.ode4libGDX.util.OdePhysicsSystem;
import com.antz.ode4libGDX.util.Utils3D;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
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
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.github.antzGames.gdx.ode4j.math.DMatrix3;
import com.github.antzGames.gdx.ode4j.math.DQuaternion;
import com.github.antzGames.gdx.ode4j.math.DVector3;
import com.github.antzGames.gdx.ode4j.ode.DAABBC;
import com.github.antzGames.gdx.ode4j.ode.DMass;
import com.github.antzGames.gdx.ode4j.ode.DRay;
import com.github.antzGames.gdx.ode4j.ode.DTriMesh;
import com.github.antzGames.gdx.ode4j.ode.DTriMeshData;
import com.github.antzGames.gdx.ode4j.ode.OdeHelper;

import static com.github.antzGames.gdx.ode4j.ode.internal.Common.M_PI;
import static com.github.antzGames.gdx.ode4j.ode.internal.Rotation.dRFromAxisAndAngle;


/**
 * Original code from: https://github.com/JamesTKhan/libgdx-bullet-tutorials
 * @author JamesTKhan
 * @version October 04, 2022
 *
 * modified to work on odej4 by:
 * Antz
 * April 27, 2023
 */
public class DemoDynamicCharacterScreen implements Screen, InputProcessor {

    protected PerspectiveCamera camera;
    protected ThirdPersonCameraController cameraController;
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

    private Array<Color> colors;
    protected BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));
    protected String info;
    private DynamicCharacterController controller;
    protected boolean showAABB;

    @Override
    public void show() {
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 500;
        camera.position.set(0,10, 50f);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add((shadowLight = new DirectionalShadowLight(2048, 2048, 30f, 30f, 1f, 100f)).set(0.8f, 0.8f, 0.8f, -.4f, -.4f, -.4f));
        environment.shadowMap = shadowLight;

        model = new Model();
        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        shadowBatch = new ModelBatch(new DepthShaderProvider());
        batch2D = new SpriteBatch();
        renderInstances = new Array<>();
        showAABB = false;

        colors = new Array<>();
        colors.add(Color.PURPLE);
        colors.add(Color.BLUE);
        colors.add(Color.TEAL);
        colors.add(Color.BROWN);
        colors.add(Color.FIREBRICK);

        odePhysicsSystem = new OdePhysicsSystem();

        // create scene floor
        createScene();

        // Create Player
        OdeEntity player = createPlayer();

        // Create Ray
        createRay();

        // Create objects
        createObjects();

        // player controller
        controller = new DynamicCharacterController();

        // camera stuff
        cameraController = new ThirdPersonCameraController(camera, player.getModelInstance());
        camera.position.set(new Vector3(0, 10, -10));
        camera.lookAt(Vector3.Zero);

        info =  "JTK's jBullet tutorial migrated to ode4j\n\n" +
            "WASD to move player, mouse wheel camera zoom.\n" +
            "SPACE to jump.\n" +
            "R to reset simulation.\n" +
            "B to toggle AABB rendering.\n" +
            "Gravity set to Moon: 1.62 m/s\n" +
            "F1 to run Demo Crash.\n";
        System.out.println(info);

        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        cameraController.update(delta);
        odePhysicsSystem.update(delta);
        controller.update(delta);

        ScreenUtils.clear(Color.BLACK, true);

        shadowLight.begin(Vector3.Zero, camera.direction);
        shadowBatch.begin(shadowLight.getCamera());
        shadowBatch.render(renderInstances);
        shadowBatch.end();
        shadowLight.end();

        modelBatch.begin(camera);
        modelBatch.render(renderInstances, environment);
        modelBatch.end();

        //render AABB boxes
        if (showAABB) renderAABB();

        // 2D stuff for info text
        batch2D.begin();
        font.draw(batch2D, info + "FPS:" + Gdx.graphics.getFramesPerSecond(), 10, 160);
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

    protected Color getRandomColor(){
        return colors.get(MathUtils.random(0, colors.size-1));
    }

    private OdeEntity createScene(){
        OdeEntity scene = new OdeEntity();
        DTriMesh sceneTriMesh;
        DTriMeshData sceneTriMeshData;

        // Load a walkable area, set tri mesh for ODE
        Model sceneModel = Utils3D.loadOBJ(Gdx.files.internal("models/scene.obj"));

        scene.modelInstance = new ModelInstance(sceneModel);
        scene.modelInstance.materials.get(0).set(ColorAttribute.createDiffuse(Color.FOREST));
        scene.modelInstance.materials.get(1).set(ColorAttribute.createDiffuse(Color.TEAL));
        scene.modelInstance.materials.get(2).set(ColorAttribute.createDiffuse(Color.DARK_GRAY));
        scene.modelInstance.materials.get(3).set(ColorAttribute.createDiffuse(Color.TAN));

        // Get vertices and indices for scene
        Array<Float> vertOut = new Array<>();
        Array<Integer> indexOut =  new Array<>();

        scene.modelInstance.transform.set(new Quaternion().setFromAxis(Vector3.Y, 90));
        Utils3D.getVerticesIndicesFromModel(scene.modelInstance, vertOut, indexOut,0); // Thanks JTK for this method

        float[] vertices = new float[vertOut.size];
        int[] indices = new int[indexOut.size];
        for (int x = 0; x < vertOut.size ; x++) vertices[x] = vertOut.get(x).floatValue();
        for (int x = 0; x < indexOut.size ; x++) indices[x] = indexOut.get(x).intValue();

        sceneTriMeshData = OdeHelper.createTriMeshData();
        ((DTriMeshData) sceneTriMeshData).build(vertices, indices);
        sceneTriMeshData.preprocess();

        // create the geom
        sceneTriMesh = OdeHelper.createTriMesh(odePhysicsSystem.space, sceneTriMeshData, null,null,null);
        sceneTriMesh.setPosition(0,0,0);
        DMatrix3 Rotation1 = new DMatrix3();
        dRFromAxisAndAngle(Rotation1, 0, 1, 0, M_PI / 2);
        //sceneTriMesh.setRotation(Rotation1);

        scene.id = "scene";
        scene.body = OdeHelper.createBody(odePhysicsSystem.world);
        scene.body.setRotation(Rotation1);
        scene.geom[0] = sceneTriMesh;

        // add to view and physics system
        odePhysicsSystem.obj.add(scene);
        renderInstances.add(scene.modelInstance);
        return scene;
    }

    private OdeEntity createPlayer() {
        OdeEntity entity = new OdeEntity();
        ModelInstance playerModelInstance = new ModelInstance(Utils3D.buildCapsuleCharacter());

        // Calculate dimension
        BoundingBox boundingBox = new BoundingBox();
        playerModelInstance.calculateBoundingBox(boundingBox);
        Vector3 dimensions = new Vector3();
        boundingBox.getDimensions(dimensions);

        // Scale for half extents
        dimensions.scl(0.5f);

        DMass m = OdeHelper.createMass();
        m.setCapsule(odePhysicsSystem.DENSITY,3,dimensions.len()/2.5f, dimensions.y);

        entity.id = "player";
        entity.body = OdeHelper.createBody(odePhysicsSystem.world);
        entity.geom[0] = OdeHelper.createCapsule(odePhysicsSystem.space,dimensions.len()/2.5f, dimensions.y);
        DMatrix3 R = new DMatrix3();
        dRFromAxisAndAngle(R, 1, 0, 0, M_PI / 2);
        entity.geom[0].setRotation(R);
        entity.body.setRotation(R);
        entity.body.setData(1);

        // Move him up above the ground
        Vector3 v = new Vector3();
        playerModelInstance.transform.setToTranslation(0,8,0);
        playerModelInstance.transform.getTranslation(v);
        //playerModelInstance.transform.rotate(Ode2GdxMathUtils.getGdxQuaternion(entity.body.getQuaternion()));
        entity.body.setPosition(v.x, v.y, v.z);
        entity.body.setMass(m);
        entity.body.setMaxAngularSpeed(0);

        for (int k = 0; k < odePhysicsSystem.GPB; k++) {
            if (entity.geom[k] != null) {
                entity.geom[k].setBody(entity.body);
            }
        }

        entity.modelInstance = playerModelInstance;
        renderInstances.add(playerModelInstance);
        odePhysicsSystem.obj.add(entity);
        return entity;
    }

    private OdeEntity createRay() {
        DRay ray = OdeHelper.createRay(OdePhysicsSystem.space, 1.4);
//        ray.setBody(OdePhysicsSystem.obj.get(1).body);
        OdeEntity rayEntity = new OdeEntity();
        rayEntity.geom[0] = ray;
        rayEntity.id = "ray";
        odePhysicsSystem.obj.add(rayEntity);
        return rayEntity;
    }

    protected void createObjects() {
        // Create some random shapes
        for (int i = -6; i < 6; i+=2) {
            for (int j = -6; j < 6; j+=2) {
                ModelBuilder modelBuilder = new ModelBuilder();
                modelBuilder.begin();
                Material material = new Material();
                material.set(ColorAttribute.createDiffuse(getRandomColor()));
                MeshPartBuilder builder = modelBuilder.part("box", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, material);

                OdeEntity entity = new OdeEntity();
                DMass m = OdeHelper.createMass();

                int random = MathUtils.random(1, 3);
                switch (random) {
                    case 1:
                        BoxShapeBuilder.build(builder, 0, 0, 0, 1f, 1f, 1f);
                        entity.body = OdeHelper.createBody(OdePhysicsSystem.world);
                        entity.geom[0] = OdeHelper.createBox(OdePhysicsSystem.space, 1,1,1);
                        m.setBox(odePhysicsSystem.DENSITY/5, 1,1,1);
                        //shape = new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f));
                        break;
                    case 2:
                        SphereShapeBuilder.build(builder, 1, 1, 1, 12, 12);
                        entity.body = OdeHelper.createBody(OdePhysicsSystem.world);
                        entity.geom[0] = OdeHelper.createSphere(OdePhysicsSystem.space, 0.5);
                        m.setSphere(odePhysicsSystem.DENSITY/5, 0.5);
                        //shape = new btSphereShape(0.5f);
                        break;
                    case 3:
                    default:
                        CylinderShapeBuilder.build(builder, 1, 1, 1, 12);
                        entity.body = OdeHelper.createBody(OdePhysicsSystem.world);
                        entity.geom[0] = OdeHelper.createCylinder(OdePhysicsSystem.space, 0.5, 1.0);
                        m.setCylinder(odePhysicsSystem.DENSITY/5, 3, 0.5,1);
                        //shape = new btCylinderShape(new Vector3(0.5f, 0.5f, 0.5f));
                        break;
                }
                entity.modelInstance = new ModelInstance(modelBuilder.end());
                entity.id = "objects";
                entity.body.setMass(m);

                // random positions and rotations
                Vector3 pos = new Vector3(i, MathUtils.random(10, 20), j);
                Quaternion q = new Quaternion(Vector3.X, MathUtils.random(0f, 270f));

                DQuaternion qq = new DQuaternion();
                qq.set(q.w, q.x, q.y, q.z);
                entity.geom[0].setQuaternion(qq);
                entity.geom[0].setPosition(pos.x, pos.y, pos.z);
                entity.body.setQuaternion(qq);
                entity.body.setPosition(pos.x, pos.y, pos.z);

                for (int k=0; k < odePhysicsSystem.GPB; k++) {
                    if (entity.geom[k] != null) {
                        entity.geom[k].setBody(entity.body);
                    }
                }

                // add physics and render objects
                renderInstances.add(entity.modelInstance);
                odePhysicsSystem.obj.add(entity);
                //bulletPhysicsSystem.addBody(body);
            }
        }
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

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.F1) {
            odePhysicsSystem.dispose();
            Ode4libGDX.game.setScreen(new DemoCrashScreen());
        } else if (keycode == Input.Keys.R) {
            Ode4libGDX.game.setScreen(new DemoDynamicCharacterScreen());
        } else if (keycode == Input.Keys.B) {
            showAABB = !showAABB;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
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
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
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
        cameraController.cameraDistance += amountY;
        return false;
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
}
