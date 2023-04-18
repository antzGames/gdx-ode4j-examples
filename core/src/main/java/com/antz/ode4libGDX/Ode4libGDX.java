package com.antz.ode4libGDX;

import com.antz.ode4libGDX.screens.CrashTest;
import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Ode4libGDX extends Game {
    @Override
    public void create() {
        setScreen(new CrashTest());
    }
}
