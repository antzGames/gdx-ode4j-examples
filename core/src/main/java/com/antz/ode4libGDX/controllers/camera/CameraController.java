package com.antz.ode4libGDX.controllers.camera;

import com.badlogic.gdx.InputAdapter;

/**
 * @author JamesTKhan
 * @version October 04, 2022
 */
abstract public class CameraController extends InputAdapter {
    abstract public void update(float delta);
}
