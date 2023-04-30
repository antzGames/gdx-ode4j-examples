package com.antz.ode4libGDX.controllers.character;

import com.antz.ode4libGDX.util.OdePhysicsSystem;
import com.antz.ode4libGDX.util.Utils3D;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import org.ode4j.math.DMatrix3;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.DRay;
import org.ode4j.ode.OdeHelper;
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
public class DynamicCharacterController {
    private final float MOVE_SPEED = 3f;
    private final float JUMP_FACTOR = 2f;

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
        isGrounded();

        // Forward movement
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            linearVelocity.set(currentDirection).scl(delta * MOVE_SPEED);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            linearVelocity.set(currentDirection).scl(-delta * MOVE_SPEED);
        }

        // Turning
        Quaternion q = new Quaternion();
        DMatrix3 R = new DMatrix3().setIdentity();
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            OdePhysicsSystem.obj.get(1).getModelInstance().transform.rotate(Vector3.Y, delta*60f);
            OdePhysicsSystem.obj.get(1).getModelInstance().transform.getRotation(q);
            dRFromAxisAndAngle(R, 0, 1, 0, q.getYawRad());
            OdePhysicsSystem.obj.get(1).body.setRotation(R);
            isGrounded();
        } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            OdePhysicsSystem.obj.get(1).getModelInstance().transform.rotate(Vector3.Y, -delta*60f);
            OdePhysicsSystem.obj.get(1).getModelInstance().transform.getRotation(q);
            dRFromAxisAndAngle(R, 0, 1, 0, q.getYawRad());
            OdePhysicsSystem.obj.get(1).body.setRotation(R);
            isGrounded();
        }

        // Jump
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            linearVelocity.y += JUMP_FACTOR;
        }

        if (!linearVelocity.isZero()) {
            OdePhysicsSystem.obj.get(1).body.addLinearVel(linearVelocity.x, linearVelocity.y, linearVelocity.z);
            OdePhysicsSystem.obj.get(1).body.addForce(linearVelocity.x, linearVelocity.y, linearVelocity.z);
        }
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

        OdePhysicsSystem.obj.get(2).geom[0].setBody(OdePhysicsSystem.obj.get(1).body);
        ((DRay)OdePhysicsSystem.obj.get(2).geom[0]).set(position.x, position.y, position.z, direction.x, direction.y, direction.z);
        ((DRay)OdePhysicsSystem.obj.get(2).geom[0]).setFirstContact(true);

        DContactGeomBuffer contacts = new DContactGeomBuffer(OdePhysicsSystem.MAX_CONTACTS);
        if (OdeHelper.collide(OdePhysicsSystem.obj.get(2).geom[0], OdePhysicsSystem.obj.get(0).geom[0], 1, contacts) != 0) {
            //System.out.println(contacts.get(0).normal);
            normal.set((float)contacts.get(0).normal.get0(),(float)contacts.get(0).normal.get1(), (float)contacts.get(0).normal.get2());
            return true;
        }
        return false;
    }

    private void resetVelocity() {
        angularVelocity.set(0,0,0);
        linearVelocity.set(0,0,0);
    }
}
