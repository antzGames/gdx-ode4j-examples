package com.antz.ode4libGDX.screens;

import com.antz.ode4libGDX.controllers.camera.ThirdPersonCameraController;
import com.antz.ode4libGDX.controllers.character.DynamicCharacterController;
import com.antz.ode4libGDX.util.Ode2GdxMathUtils;
import com.antz.ode4libGDX.util.OdeEntity;
import com.antz.ode4libGDX.util.OdePhysicsSystem;
import com.antz.ode4libGDX.util.Utils3D;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import org.ode4j.math.DMatrix3;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DRay;
import org.ode4j.ode.DTriMeshData;
import org.ode4j.ode.OdeHelper;

import static org.ode4j.ode.internal.Common.M_PI;
import static org.ode4j.ode.internal.Rotation.dRFromAxisAndAngle;

/**
 * @author JamesTKhan
 * @version October 02, 2022
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
    }

    @Override
    public void render(float delta) {
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

//        for (int k = 0; k < odePhysicsSystem.GPB; k++) {
//            if (scene.geom[k] != null) {
//                scene.geom[k].setBody(scene.body);
//            }
//        }

        // add to view and physics system
        //renderInstances.add(Utils3D.getModelFromVerticesIndices(vertices, indices));
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
}
