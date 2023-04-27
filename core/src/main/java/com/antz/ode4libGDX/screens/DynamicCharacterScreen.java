package com.antz.ode4libGDX.screens;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.controllers.camera.ThirdPersonCameraController;
import com.antz.ode4libGDX.controllers.character.DynamicCharacterController;
import com.antz.ode4libGDX.util.Ode2GdxMathUtils;
import com.antz.ode4libGDX.util.OdeEntity;
import com.antz.ode4libGDX.util.OdePhysicsSystem;
import com.antz.ode4libGDX.util.Utils3D;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DQuaternion;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DRay;
import org.ode4j.ode.DTriMeshData;
import org.ode4j.ode.OdeHelper;
import static org.ode4j.ode.internal.Common.M_PI;
import static org.ode4j.ode.internal.Rotation.dRFromAxisAndAngle;

/**
 * Original code from: https://github.com/JamesTKhan/libgdx-bullet-tutorials
 * @author JamesTKhan
 * @version October 04, 2022
 *
 * modified to work on odej4 by:
 * Antz
 * April 27, 2023
 */
public class DynamicCharacterScreen extends BaseScreen {

    private final DynamicCharacterController controller;

    public DynamicCharacterScreen() {
        super();

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
        setCameraController(new ThirdPersonCameraController(camera, player.getModelInstance()));
        camera.position.set(new Vector3(0, 10, -10));
        camera.lookAt(Vector3.Zero);

        info =  "JTK's jBullet tutorial migrated to ode4j\n\n" +
                "WASD to move player, mouse wheel camera zoom.\n" +
                "SPACE to jump.\n" +
                "F1 to run Demo Crash.\n";
        System.out.println(info);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.F1)) Ode4libGDX.game.setScreen(new DemoCrashScreen());

        controller.update(delta);
        super.render(delta);
    }

    private OdeEntity createScene(){
        OdeEntity scene = new OdeEntity();
        DGeom sceneTriMesh;
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
        sceneTriMeshData.build(vertices, indices);
        sceneTriMeshData.preprocess();

        // create the geom
        sceneTriMesh = OdeHelper.createTriMesh(odePhysicsSystem.space, sceneTriMeshData, null,null,null);
        //sceneTriMesh.setData(sceneTriMeshData);
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
        m.setCapsule(odePhysicsSystem.DENSITY,2,dimensions.len()/2.5f, dimensions.y);

        entity.id = "player";
        entity.body = OdeHelper.createBody(odePhysicsSystem.world);
        entity.geom[0] = OdeHelper.createCapsule(odePhysicsSystem.space,dimensions.len()/2.5f, dimensions.y);
        DMatrix3 R = new DMatrix3();
        dRFromAxisAndAngle(R, 0, 1, 0, M_PI / 2);
        entity.body.setRotation(R);
        entity.body.setData(1);

        // Move him up above the ground
        Vector3 v = new Vector3();
        playerModelInstance.transform.setToTranslation(0,4,0);
        playerModelInstance.transform.getTranslation(v);
        playerModelInstance.transform.set(Ode2GdxMathUtils.getGdxQuaternion(entity.body.getQuaternion()));
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
                entity.id = "objects";

                int random = MathUtils.random(1, 3);
                switch (random) {
                    case 1:
                        BoxShapeBuilder.build(builder, 0, 0, 0, 1f, 1f, 1f);
                        entity.body = OdeHelper.createBody(OdePhysicsSystem.world);
                        entity.geom[0] = OdeHelper.createBox(OdePhysicsSystem.space, 1,1,1);
                        //shape = new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f));
                        break;
                    case 2:
                        SphereShapeBuilder.build(builder, 1, 1, 1, 12, 12);
                        entity.body = OdeHelper.createBody(OdePhysicsSystem.world);
                        entity.geom[0] = OdeHelper.createSphere(OdePhysicsSystem.space, 0.5);
                        //shape = new btSphereShape(0.5f);
                        break;
                    case 3:
                    default:
                        CylinderShapeBuilder.build(builder, 1, 1, 1, 12);
                        entity.body = OdeHelper.createBody(OdePhysicsSystem.world);
                        entity.geom[0] = OdeHelper.createCylinder(OdePhysicsSystem.space, 0.5, 1.0);
                        //shape = new btCylinderShape(new Vector3(0.5f, 0.5f, 0.5f));
                        break;
                }

                m.setBox(odePhysicsSystem.DENSITY/10, 1,1,1);
                entity.body.setMass(m);
                entity.modelInstance = new ModelInstance(modelBuilder.end());
                entity.modelInstance.transform.setToTranslation(i, MathUtils.random(10, 20), j);
                entity.modelInstance.transform.rotate(new Quaternion(Vector3.X, MathUtils.random(0f, 270f)));

                Vector3 pos = new Vector3();
                Quaternion q = new Quaternion();
                entity.modelInstance.transform.getTranslation(pos);
                entity.modelInstance.transform.getRotation(q);

                DQuaternion qq = new DQuaternion();
                qq.set(q.w, q.x, q.y, q.z);
                entity.geom[0].setQuaternion(qq);
                entity.geom[0].setPosition(pos.x, pos.y, pos.z);
                entity.body.setPosition(pos.x, pos.y, pos.z);
                entity.body.setQuaternion(qq);

                for (int k=0; k < odePhysicsSystem.GPB; k++) {
                    if (entity.geom[k] != null) {
                        entity.geom[k].setBody(entity.body);
                    }
                }

                renderInstances.add(entity.modelInstance);
                odePhysicsSystem.obj.add(entity);
                //bulletPhysicsSystem.addBody(body);
            }
        }
    }
}
