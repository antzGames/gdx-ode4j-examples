package com.antz.ode4libGDX.controllers.character;

import com.antz.ode4libGDX.screens.DynamicCharacterScreen;
import com.antz.ode4libGDX.util.OdeEntity;
import com.antz.ode4libGDX.util.OdePhysicsSystem;
import com.antz.ode4libGDX.util.Utils3D;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.DRay;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.DxRay;

/**
 * @author JamesTKhan
 * @version October 10, 2022
 */
public class DynamicCharacterController {
    private final float MOVE_SPEED = 45f;
    private final float JUMP_FACTOR = 45f;

    private final Vector3 position = new Vector3();
    private final Vector3 normal = new Vector3();
    private final Vector3 tmpPosition = new Vector3();
    private final Vector3 currentDirection = new Vector3();
    private final Vector3 linearVelocity = new Vector3();
    private final Vector3 angularVelocity = new Vector3();

    public DynamicCharacterController() {

    }

    public void update(float delta) {

        Utils3D.getDirection(OdePhysicsSystem.obj.get(1).getModelInstance().transform, currentDirection);
        resetVelocity();
        boolean isOnGround = isGrounded();

        // A slightly hacky work around to allow climbing up and preventing sliding down slopes
        if (isOnGround) {
            OdePhysicsSystem.obj.get(1).body.setGravityMode(false);
            //callback.getHitNormalWorld(normal);

            // dot product returns 1 if same direction, -1 if opposite direction, zero if perpendicular
            // so we get the dot product of the normal and the Up (Y) vector.
            float dot = normal.dot(Vector3.Y);

            // If the dot product is NOT 1, meaning the ground is not flat, then we disable gravity
          if (dot != 1.0) {
              OdePhysicsSystem.obj.get(1).body.setGravityMode(false);
                //body.setGravity(Vector3.Zero);
            }
        } else {
            OdePhysicsSystem.obj.get(1).body.setGravityMode(true);
            //body.setGravity(BulletPhysicsSystem.DEFAULT_GRAVITY);
        }

        // Forward movement
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            linearVelocity.set(currentDirection).scl(delta * MOVE_SPEED);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            linearVelocity.set(currentDirection).scl(-delta * MOVE_SPEED);
        }

        // Turning
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            angularVelocity.set(0, 1f, 0);
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            angularVelocity.set(0, -1f, 0);
        }

        // Jump
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
        //if (isOnGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            linearVelocity.y += JUMP_FACTOR;
            System.out.println("Jump");
        }


        if (!linearVelocity.isZero()) {
            OdePhysicsSystem.obj.get(1).body.addForce(linearVelocity.x, linearVelocity.y, linearVelocity.z);
            //body.applyCentralImpulse(linearVelocity);
        }

        if (!angularVelocity.isZero()) {
            OdePhysicsSystem.obj.get(1).body.setAngularVel(angularVelocity.x, angularVelocity.y, angularVelocity.z);
            //body.setAngularVelocity(angularVelocity);
        }

        System.out.println("dir: " + currentDirection + "  G: " +OdePhysicsSystem.obj.get(1).body.getGravityMode() + "   isGround: " + isOnGround + "  lv: " + linearVelocity + "  av: " + angularVelocity);

    }

    /**
     * Check if we are standing on something by casting a ray straight down.
     * @return
     */
    private boolean isGrounded() {
        Utils3D.getPosition(OdePhysicsSystem.obj.get(1).getModelInstance().transform, position);
        // The position we are casting a ray to, slightly below the players current position.
        tmpPosition.set(position).sub(0, 1.4f, 0);
        Vector3 direction = new Vector3(0,-1,0);

        DRay ray = OdeHelper.createRay(OdePhysicsSystem.space, 2.4);
        ray.setBody(OdePhysicsSystem.obj.get(1).body);
        ray.set(position.x, position.y, position.z, direction.x, direction.y, direction.z);
        ray.setFirstContact(true);

        Model model;
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder meshPartBuilder = modelBuilder.part("line", 1, 3, new Material());
        meshPartBuilder.setColor(Color.CYAN);
        meshPartBuilder.line(position.x, position.y, position.z, tmpPosition.x, tmpPosition.y, tmpPosition.z);
        model = modelBuilder.end();
        OdePhysicsSystem.obj.get(2).modelInstance = new ModelInstance(model);
        OdePhysicsSystem.obj.get(2).geom[0] = ray;

        //System.out.println(tmpPosition + " " + position + " " + direction + " " + ((DxRay) DynamicCharacterScreen.rayObject.geom[0]).getLength());
        //DContactGeomBuffer contacts = new DContactGeomBuffer(OdePhysicsSystem.MAX_CONTACTS);
        DContactGeomBuffer contacts = new DContactGeomBuffer(OdePhysicsSystem.MAX_CONTACTS);
        if (OdeHelper.collide(OdePhysicsSystem.obj.get(2).geom[0], OdePhysicsSystem.obj.get(0).geom[0], 1, contacts) != 0) {
            System.out.println("RAY hit");
            //System.out.println(contacts.get(0).normal);
            normal.set((float)contacts.get(0).normal.get0(),(float)contacts.get(0).normal.get1(), (float)contacts.get(0).normal.get2());
            return true;
        }
        return false;

        //dsDrawLine(Origin, End);
        //return callback.hasHit();
    }


    private void resetVelocity() {
        angularVelocity.set(0,0,0);
        linearVelocity.set(0,0,0);
    }
}
