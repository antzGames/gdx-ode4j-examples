package com.antz.ode4libGDX.screens;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.screens.demo.DemoCollisionTest;
import com.antz.ode4libGDX.screens.demo.DemoDynamicCharacterScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;

public class LoadingScreen implements Screen {

    private Stage stage;
    private Image logo;
    private Image loadingFrame;
    private Image loadingBarHidden;
    private Image screenBg;
    private Image loadingBg;
    private Actor loadingBar;
    private float startX, endX;
    private boolean isReady;
    private SpriteBatch batch;
    public AssetManager assetManager;
    public AssetManager assetManagerForLoadingBar;
    private BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/lsans-15.fnt"));

    public LoadingScreen() {}

    @Override
    public void show() {
        // Catch Browser keys
        Gdx.input.setCatchKey(Input.Keys.SPACE, true);
        Gdx.input.setCatchKey(Input.Keys.F1, true);
        Gdx.input.setCatchKey(Input.Keys.F2, true);
        Gdx.input.setCatchKey(Input.Keys.F3, true);
        Gdx.input.setCatchKey(Input.Keys.UP, true);
        Gdx.input.setCatchKey(Input.Keys.DOWN, true);
        Gdx.input.setCatchKey(Input.Keys.LEFT, true);
        Gdx.input.setCatchKey(Input.Keys.RIGHT, true);

        //assetManager = Constants.mundus.getAssetManager().getGdxAssetManager();
        assetManager = new AssetManager();
        assetManagerForLoadingBar = new AssetManager();

        // Tell the manager to load assets for the loading screen
        assetManagerForLoadingBar.load("graphics/loading.pack", TextureAtlas.class);
        // Wait until they are finished loading
        assetManagerForLoadingBar.finishLoading();

        // Initialize the stage where we will place everything
        stage = new Stage();
        batch = new SpriteBatch();

        // Get our textureatlas from the manager
        TextureAtlas atlas = assetManagerForLoadingBar.get("graphics/loading.pack", TextureAtlas.class);

        // Grab the regions from the atlas and create some images
        logo = new Image(atlas.findRegion("libgdx-logo"));
        loadingFrame = new Image(atlas.findRegion("loading-frame"));
        loadingBarHidden = new Image(atlas.findRegion("loading-bar-hidden"));
        screenBg = new Image(atlas.findRegion("screen-bg"));
        loadingBg = new Image(atlas.findRegion("loading-frame-bg"));

        // Or if you only need a static bar, you can do
        loadingBar = new Image(atlas.findRegion("loading-bar1"));

        // Add all the actors to the stage
        stage.addActor(screenBg);
        stage.addActor(loadingBar);
        stage.addActor(loadingBg);
        stage.addActor(logo);
    }

    @Override
    public void resize(int width, int height) {
        // Make the background fill the screen
	    screenBg.setSize(stage.getWidth(), stage.getHeight());

        // Place the loading frame in the middle of the screen
        loadingFrame.setX((stage.getWidth() - loadingFrame.getWidth()) / 2);
        loadingFrame.setY((stage.getHeight() - loadingFrame.getHeight()) / 2);

        // Place the loading bar at the same spot as the frame, adjusted a few px
        loadingBar.setX(loadingFrame.getX() + 15);
        loadingBar.setY(loadingFrame.getY() + 5);

	    // Place the logo in the middle of the screen
        logo.setX((stage.getWidth() - logo.getWidth()) / 2);
        logo.setY(loadingFrame.getY() + loadingFrame.getHeight() + 15);

        // Place the image that will hide the bar on top of the bar, adjusted a few px
        loadingBarHidden.setX(loadingBar.getX() + 35);
        loadingBarHidden.setY(loadingBar.getY() - 3);
        // The start position and how far to move the hidden loading bar
        startX = loadingBarHidden.getX();
        endX = 440;

        // The rest of the hidden bar
        loadingBg.setSize(450, 50);
        loadingBg.setX(loadingBarHidden.getX() + 30);
        loadingBg.setY(loadingBarHidden.getY() + 3);
    }

    @Override
    public void render(float delta) {
        // Clear the screen
        ScreenUtils.clear(1, 1, 1, 1);

        // update percent;
        float percent = (assetManager.getProgress());

        // Update positions (and size) to match the percentage
        loadingBarHidden.setX(startX + endX * percent);
        loadingBg.setX(loadingBarHidden.getX() + 30);
        loadingBg.setWidth(450 - 450 * percent);
        loadingBg.invalidate();

        // Show the loading screen
        stage.act();
        stage.draw();

        if (isReady && assetManager.isFinished()){
            batch.begin();
            font.setColor(Color.BLACK);
            font.getData().setScale(2f);
            font.draw(batch, "CLICK TO CONTINUE", stage.getWidth()/2f - 145, 270);
            font.setColor(Color.WHITE);
            batch.end();
        }

        if (assetManager.update()) { // Load some, will return true if done loading
            //Textures

            // Next Screen
            if (!isReady){
                loadingBar.remove();
                loadingBarHidden.remove();
                loadingFrame.remove();
                isReady = true;

            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.justTouched()) {
                Ode4libGDX.game.setScreen(new DemoCollisionTest());
            }
        }
    }

    @Override
    public void hide() {
        // Dispose the loading assets as we no longer need them
        assetManagerForLoadingBar.unload("graphics/loading.pack");
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        assetManagerForLoadingBar.dispose();
        assetManager.dispose();
        batch.dispose();
    }
}
