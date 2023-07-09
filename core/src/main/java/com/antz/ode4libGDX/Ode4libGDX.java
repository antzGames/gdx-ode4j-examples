package com.antz.ode4libGDX;

import com.antz.ode4libGDX.screens.demo.ModelInstancedRendering;
import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Ode4libGDX extends Game {

    public static Game game;

    @Override
    public void create() {
        this.game = this;
        //setScreen(new LoadingScreen());
        //setScreen(new OdeTest());
        setScreen(new ModelInstancedRendering());
    }
}
