package com.antz.ode4libGDX;

import com.antz.ode4libGDX.screens.DemoCrashScreen;
import com.antz.ode4libGDX.screens.DemoTriMeshHeightFieldScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Ode4libGDX extends Game {

    public static Game game;

    @Override
    public void create() {
        this.game = this;
        setScreen(new DemoCrashScreen());
    }
}
