package com.antz.ode4libGDX.screens;

import com.antz.ode4libGDX.Ode4libGDX;
import com.antz.ode4libGDX.screens.demo.DemoDynamicCharacterScreen;
import com.antz.ode4libGDX.util.Ode2GdxMathUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.mbrlabs.mundus.commons.Scene;
import com.mbrlabs.mundus.commons.assets.meta.MetaFileParseException;
import com.mbrlabs.mundus.commons.scene3d.GameObject;
import com.mbrlabs.mundus.runtime.Mundus;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DGeom.DNearCallback;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSapSpace;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import java.util.ArrayList;
import static org.ode4j.ode.OdeMath.dContactApprox1;
import static org.ode4j.ode.OdeMath.dContactSlip1;
import static org.ode4j.ode.OdeMath.dContactSlip2;
import static org.ode4j.ode.OdeMath.dContactSoftCFM;
import static org.ode4j.ode.OdeMath.dContactSoftERP;
import static org.ode4j.ode.OdeMath.dMultiply0;
import static org.ode4j.ode.OdeMath.dRFromAxisAndAngle;

public class LibGDXScreen implements Screen {

    // My stuff
    private Mundus mundus;
    private Scene scene;
    enum GameState {
        LOADING,
        RENDER
    }

    private GameState gameState = GameState.LOADING;
    private TextureRegion texture;
    private TextureRegion[] boxTextures;
    private int boxTextureIndex, logicIndex;
    private float logicTimer;
    private ModelBatch modelBatch;
    private ModelBuilder modelBuilder;
    private MeshPartBuilder meshBuilder;
    private Model model;
    private ArrayList<GameObject> wallBoxGameObjects = new ArrayList<>();
    private Music zebraMusic, zebraTalk, libGDXMusic, antzMusic;

    // some constants
    private static final float RADIUS = 0.5f;	            // canon ball radius
    private static final float WALLMASS = 1	;	            // wall box mass
    private static final int   ITERS = 20;		            // number of iterations
    private static final float WBOXSIZE = 1.0f;		        // size of wall boxes
    private static final int   WALLWIDTH = 10;		        // width of wall
    private static final int   WALLHEIGHT = 10;		        // height of wall
    private static final float DISABLE_THRESHOLD = 0.008f;	// maximum velocity (squared) a body can have and be disabled
    private static final float DISABLE_STEPS = 10;	        // number of steps a box has to have been disable-able before it will be disabled
    private static final float CANNON_X = 100;		        // x position of cannon
    private static final float CANNON_Z = 90;	            // y position of cannon
    private static final float CANNON_BALL_MASS = 10;	    // mass of the cannon ball
    private static final float CANNON_BALL_RADIUS = 0.5f;   // cannon ball radius

    // dynamics and collision objects
    private static DWorld world;
    private static DSpace space;
    private static int bodies;
    private static DJointGroup contactGroup;

    private static final DBox[] wall_boxes = new DBox[1000];
    private static final DBody[] wall_bodies = new DBody[1000];
    private static final int[] wb_stepsdis = new int[1000];

    private static DBody cannon_ball_body;

    private static DBody antzBody;

    private static int wb;
    private static boolean doFast;

    private static float cannon_angle = 0;
    private static float cannon_elevation = 0f;

    @Override
    public void show() {
        Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());

        // From Mundus Example Project
        Mundus.Config config = new Mundus.Config();
        config.autoLoad = false; // Do not autoload, we want to queue custom assets
        config.asyncLoad = true; // Do asynchronous loading

        // Start asynchronous loading
        mundus = new Mundus(Gdx.files.internal("mundus"), config);
        try {
            mundus.getAssetManager().queueAssetsForLoading(true);
        } catch (MetaFileParseException e) {
            e.printStackTrace();
        }

        mundus.getAssetManager().getGdxAssetManager().load("sounds/base3.mp3", Music.class);
        mundus.getAssetManager().getGdxAssetManager().load("sounds/base5.mp3", Music.class);
        mundus.getAssetManager().getGdxAssetManager().load("sounds/antz.mp3", Music.class);
        mundus.getAssetManager().getGdxAssetManager().load("sounds/zebraTalk.mp3", Music.class);

        modelBatch = new ModelBatch();
        modelBuilder = new ModelBuilder();
        meshBuilder = new MeshBuilder();
        texture = new TextureRegion(new Texture(Gdx.files.internal("graphics/img_0.png")));

        doFast = true;
        bodies = 0;
        logicTimer = 0;
        logicIndex = 0;
        OdeHelper.initODE2(0);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        switch (gameState) {
            case LOADING:
                continueLoading();
                break;
            case RENDER:
                doLogic();
                simLoop();
                draw();
                break;
        }
    }

    private void draw() {
        scene.sceneGraph.update();  // update Mundus
        scene.render();             // render Mundus scene
    }

    // libGDX screen logic
    private void doLogic() {
        logicTimer += Gdx.graphics.getDeltaTime();

        switch (logicIndex){
            case 0: // fire cannon after 3s
                if (!zebraTalk.isPlaying()){
                    zebraTalk.setVolume(1);
                    zebraTalk.play();
                }
                if (logicTimer > 3.75f){
                    logicIndex = 1;
                    logicTimer = 0;
                    fireCannon();
                    zebraMusic.setVolume(1);
                    zebraMusic.play();
                }
                break;
            case 1: // wait 6 sec and then switch to libGDX wall scene
                if (logicTimer > 7){
                    logicIndex = 2;
                    logicTimer = 0;
                    texture = new TextureRegion(new Texture(Gdx.files.internal("graphics/img_1.png")));
                    shutdownSimulation();
                    setupSimulation();
                    scene.cam.position.set(100,5f,85);
                    scene.cam.lookAt(100,5,100);
                    scene.cam.up.set(Vector3.Y);
                    scene.cam.update();
                }
                break;
            case 2: // wait 2.5s and then fire cannon
                if (logicTimer > 2.5f){
                    logicIndex = 3;
                    logicTimer = 0;
                    fireCannon();
                    libGDXMusic.setVolume(1);
                    libGDXMusic.play();

                }
                break;
            case 3: // slide Antz across
                if (logicTimer > 3 && !antzMusic.isPlaying()){
                    antzMusic.setVolume(1);
                    antzMusic.play();
                }
                if (logicTimer > 7){
                    logicIndex = 4;
                    logicTimer = 0;
                    antzBody.setLinearVel(25,0,0);
                }
                break;
            case 4: // when it hits middle stop
                if (antzBody.getPosition().get0() > 100) {
                    antzBody.setLinearVel(0,0,0);
                    logicIndex = 5;
                    logicTimer = 0;
                }
                break;
            case 5: // after 3s start spinning
                if (logicTimer < 12.5f) {
                    Quaternion q = new Quaternion();
                    scene.sceneGraph.getGameObjects().get(scene.sceneGraph.getGameObjects().size-1).getRotation(q);
                    q.set(Vector3.Y, Gdx.graphics.getDeltaTime()*15);
                    scene.sceneGraph.getGameObjects().get(scene.sceneGraph.getGameObjects().size-1).rotate(q);
                }
                if (logicTimer >= 12.5f) {
                    Ode4libGDX.game.setScreen(new DemoDynamicCharacterScreen());
                }
                break;
        }
    }

    // simulation loop
    private void simLoop ()  {
        if (doFast) {
            space.collide (null,nearCallback);
            world.quickStep (0.05);
            contactGroup.empty ();
        } else {
            space.collide (null,nearCallback);
            world.step (0.05);
            contactGroup.empty ();
        }

        for (int i = 0; i < wb; i++) {
            DBody b = wall_boxes[i].getBody();
            if (b.isEnabled()) {
                boolean disable = true;
                DVector3C lvel = b.getLinearVel();
                double lspeed = lvel.lengthSquared();
                if (lspeed > DISABLE_THRESHOLD)
                    disable = false;
                DVector3C avel = b.getAngularVel();
                double aspeed = avel.lengthSquared();
                if (aspeed > DISABLE_THRESHOLD)
                    disable = false;

                if (disable)
                    wb_stepsdis[i]++;
                else
                    wb_stepsdis[i] = 0;

                if (wb_stepsdis[i] > DISABLE_STEPS) {
                    b.disable();
                }
            }
            DVector3 ss = new DVector3();
            wall_boxes[i].getLengths(ss);
            wallBoxGameObjects.get(i).setLocalRotation(Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion()).x,Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion()).y, Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion()).z, Ode2GdxMathUtils.getGdxQuaternion(wall_boxes[i].getQuaternion()).w);
            wallBoxGameObjects.get(i).setLocalPosition((float)wall_boxes[i].getPosition().get0(), (float)wall_boxes[i].getPosition().get1(), (float)wall_boxes[i].getPosition().get2());

        }
        scene.sceneGraph.getGameObjects().get(scene.sceneGraph.getGameObjects().size-2).setLocalPosition((float)cannon_ball_body.getPosition().get0(),(float)cannon_ball_body.getPosition().get1(),(float)cannon_ball_body.getPosition().get2());
        scene.sceneGraph.getGameObjects().get(scene.sceneGraph.getGameObjects().size-1).setLocalPosition((float)antzBody.getPosition().get0(),(float)antzBody.getPosition().get1(),(float)antzBody.getPosition().get2());
    }

    private void setupSimulation() {
        cannon_angle = 0;
        cannon_elevation = 0;
        boxTextureIndex = 0;

        for (int i = 0; i < 1000; i++) wb_stepsdis[i] = 0;

        // recreate world
        world = OdeHelper.createWorld();
        space = OdeHelper.createSapSpace(null, DSapSpace.AXES.XZY );
        DMass m = OdeHelper.createMass();

        contactGroup = OdeHelper.createJointGroup();
        world.setGravity(0,-1.5,0);
        world.setCFM(1e-5);
        world.setERP(0.8);
        world.setQuickStepNumIterations (ITERS);

        OdeHelper.createPlane(space,0,1,0,0);
        bodies = 0;
        wb = 0;

        initBoxTextures(texture);

        // Wall boxes
        for (float y = WBOXSIZE/2f; y < WALLHEIGHT; y += WBOXSIZE) {
            for (float x = -WALLWIDTH/2f; x < WALLWIDTH/2f; x += WBOXSIZE) {
                wall_bodies[wb] = OdeHelper.createBody(world);

                wall_bodies[wb].setPosition(100.5f + x , y ,100 );
                m.setBox(1,WBOXSIZE,WBOXSIZE,WBOXSIZE);
                m.adjust(WALLMASS);
                wall_bodies[wb].setMass(m);
                wall_boxes[wb] = OdeHelper.createBox (space,WBOXSIZE,WBOXSIZE,WBOXSIZE);
                wall_boxes[wb].setBody(wall_bodies[wb]);

                // libGDX model code
                wallBoxGameObjects.add(createBox());
                wb++;
            }
        }

        // Cannon ball
        cannon_ball_body = OdeHelper.createBody(world);
        DSphere cannon_ball_geom = OdeHelper.createSphere(space, CANNON_BALL_RADIUS);
        m.setSphereTotal(CANNON_BALL_MASS,CANNON_BALL_RADIUS);
        cannon_ball_body.setMass(m);
        cannon_ball_geom.setBody(cannon_ball_body);
        cannon_ball_body.setPosition(CANNON_X, CANNON_BALL_RADIUS, CANNON_Z);

        // libGDX model code
        model = modelBuilder.createSphere(RADIUS, RADIUS, RADIUS, 10, 10,
            new Material(ColorAttribute.createDiffuse(Color.BLACK)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
        scene.sceneGraph.addGameObject(new ModelInstance(model), new Vector3(0,0,0));

        // antz box
        antzBody = OdeHelper.createBody(world);
        DBox antzGeom = OdeHelper.createBox(space, 11, 11, 11);
        m.setBoxTotal(CANNON_BALL_MASS*100, 10,10, 10);
        antzBody.setMass(m);
        antzGeom.setBody(antzBody);
        antzBody.setPosition(0,5f,100);
        createAntzBox();
    }

    private void initBoxTextures(TextureRegion t) {
        TextureRegion[][] tmp = t.split(
            texture.getRegionWidth() / WALLWIDTH,
            texture.getRegionHeight() / WALLHEIGHT);

        boxTextures = new TextureRegion[WALLWIDTH * WALLHEIGHT];
        int index = 0;
        for (int i = 0; i < WALLWIDTH; i++) {
            for (int j = 0; j < WALLHEIGHT; j++) {
                boxTextures[index++] = tmp[i][j];
            }
        }
    }

    private GameObject createBox(){
        float size = WBOXSIZE/2f;

        Material material = new Material();
        material.set(PBRTextureAttribute.createBaseColorTexture(boxTextures[boxTextureIndex]));
        material.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1 / 256f));
        VertexAttributes attributes = new VertexAttributes(
            VertexAttribute.Position(),
            VertexAttribute.Normal(),
            VertexAttribute.TexCoords(0)
        );

        modelBuilder.begin();
        meshBuilder = modelBuilder.part("front",GL20.GL_TRIANGLES,attributes,material);
        meshBuilder.rect(new Vector3(-size,size,-size), new Vector3(size,size,-size), new Vector3(size,-size,-size), new Vector3(-size,-size,-size), new Vector3(0,0,-1));

        // Now do other sides
        meshBuilder = modelBuilder.part("others",GL20.GL_TRIANGLES,attributes,material);

        // back
        meshBuilder.setUVRange(1,0,0,1);
        meshBuilder.rect(new Vector3(size,size,size), new Vector3(-size,size,size), new Vector3(-size,-size,size), new Vector3(size,-size,size),  new Vector3(0,0,1));
        meshBuilder.setUVRange(0,0,1,1);

        //right
        meshBuilder.rect(new Vector3(-size,size,size), new Vector3(-size,size,-size), new Vector3(-size,-size,-size), new Vector3(-size,-size,size), new Vector3(-1,0,0));
        //left
        meshBuilder.rect(new Vector3(size,size,-size), new Vector3(size,size,size), new Vector3(size,-size,size), new Vector3(size,-size,-size),  new Vector3(0,0,1));

        // top
        meshBuilder.rect(new Vector3(-size,size,size), new Vector3(size,size,size), new Vector3(size,size,-size), new Vector3(-size,size,-size), new Vector3(0,1,0));
        //bottom
        meshBuilder.rect(new Vector3(-size,-size,-size), new Vector3(size,-size,-size), new Vector3(size,-size,size), new Vector3(-size,-size,size), new Vector3(0,-1,0));

        boxTextureIndex++;
        model = modelBuilder.end();
        return scene.sceneGraph.addGameObject(new ModelInstance(model), new Vector3(0,0,0));
    }

    private void createAntzBox(){
        float size = 10/2f;

        Material material = new Material();
        material.set(PBRTextureAttribute.createBaseColorTexture(new TextureRegion(new Texture(Gdx.files.internal("graphics/img_2.png")))));
        material.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1 / 256f));
        VertexAttributes attributes = new VertexAttributes(
            VertexAttribute.Position(),
            VertexAttribute.Normal(),
            VertexAttribute.TexCoords(0)
        );

        Material material2 = new Material();
        material2.set(PBRTextureAttribute.createBaseColorTexture(new TextureRegion(new Texture(Gdx.files.internal("graphics/img_3.png")))));
        material2.set(new PBRFloatAttribute(PBRFloatAttribute.ShadowBias, 1 / 256f));

        modelBuilder.begin();
        meshBuilder = modelBuilder.part("front",GL20.GL_TRIANGLES,attributes,material);
        meshBuilder.rect(new Vector3(-size,size,-size), new Vector3(size,size,-size), new Vector3(size,-size,-size), new Vector3(-size,-size,-size), new Vector3(0,0,-1));

        // Now do other sides
        meshBuilder = modelBuilder.part("others",GL20.GL_TRIANGLES,attributes,material);
        //right
        meshBuilder.rect(new Vector3(-size,size,size), new Vector3(-size,size,-size), new Vector3(-size,-size,-size), new Vector3(-size,-size,size), new Vector3(-1,0,0));
        //left
        meshBuilder.rect(new Vector3(size,size,-size), new Vector3(size,size,size), new Vector3(size,-size,size), new Vector3(size,-size,-size),  new Vector3(0,0,1));

        // top
        meshBuilder.rect(new Vector3(-size,size,size), new Vector3(size,size,size), new Vector3(size,size,-size), new Vector3(-size,size,-size), new Vector3(0,1,0));
        //bottom
        meshBuilder.rect(new Vector3(-size,-size,-size), new Vector3(size,-size,-size), new Vector3(size,-size,size), new Vector3(-size,-size,size), new Vector3(0,-1,0));

        // back
        meshBuilder = modelBuilder.part("back",GL20.GL_TRIANGLES,attributes,material2);
        meshBuilder.setUVRange(1,0,0,1);
        meshBuilder.rect(new Vector3(size,size,size), new Vector3(-size,size,size), new Vector3(-size,-size,size), new Vector3(size,-size,size),  new Vector3(0,0,1));
        meshBuilder.setUVRange(0,0,1,1);

        boxTextureIndex++;
        model = modelBuilder.end();
        scene.sceneGraph.addGameObject(new ModelInstance(model), new Vector3(0,0,0));
    }

    @Override
    public void resize(int width, int height) {
        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        shutdownSimulation();

        // Destroy screen's assets here.
        model.dispose();
        modelBatch.dispose();
        mundus.dispose();
        scene.dispose();

        contactGroup.destroy();
        space.destroy();
        world.destroy();
        OdeHelper.closeODE();
    }

     private void fireCannon() {
        DMatrix3 R2 = new DMatrix3(), R3 = new DMatrix3(), R4 = new DMatrix3();
        dRFromAxisAndAngle (R2,0,1,0,cannon_angle);
        dRFromAxisAndAngle (R3,1,0,0,cannon_elevation);
        dMultiply0 (R4,R2,R3);
        double[] cpos = {CANNON_X,1, CANNON_Z};
        for (int i=0; i<3; i++)
            cpos[i] += 3*R4.get(i, 2);

        cannon_ball_body.setPosition(cpos[0],cpos[1],cpos[2]);
        double force = 10;
        cannon_ball_body.setLinearVel(force*R4.get(0, 2),force*R4.get(1,2),force*R4.get(2,2));
        cannon_ball_body.setAngularVel(0,0,0);
    }

    private final DNearCallback nearCallback = (data, o1, o2) -> nearCallback(o1, o2);

    private void nearCallback (DGeom o1, DGeom o2) {
        int i,n;

        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1!=null && b2!=null && OdeHelper.areConnected(b1, b2))
            return;

        final int N = 4;
        DContactBuffer contacts = new DContactBuffer(N);
        n = OdeHelper.collide(o1,o2,N,contacts.getGeomBuffer());//[0].geom,sizeof(dContact));
        if (n > 0) {
            for (i=0; i<n; i++) {
                DContact contact = contacts.get(i);
                contact.surface.mode = dContactSlip1 | dContactSlip2 | dContactSoftERP | dContactSoftCFM | dContactApprox1;
                if ( o1 instanceof DSphere || o2 instanceof DSphere )
                    contact.surface.mu = 20;
                else
                    contact.surface.mu = 0.5;

                contact.surface.slip1 = 0.0;
                contact.surface.slip2 = 0.0;
                contact.surface.soft_erp = 0.8;
                contact.surface.soft_cfm = 0.01;
                DJoint c = OdeHelper.createContactJoint(world, contactGroup,contact);
                c.attach (o1.getBody(), o2.getBody());

                if (o1 instanceof DSphere && o2 instanceof DBox){
                    o2.getBody().addLinearVel(25,50,0);
                } else if (o2 instanceof DSphere && o1 instanceof DBox){
                    o1.getBody().addLinearVel(-25,50,0);
                }
            }
        }
    }

    private void shutdownSimulation() {
        // destroy world if it exists
        if (bodies!=0)  {
            contactGroup.destroy();
            space.destroy();
            world.destroy();
            bodies = 0;
        }
        scene.sceneGraph.getGameObjects().get(scene.sceneGraph.getGameObjects().size-2).remove();
        scene.sceneGraph.getGameObjects().get(scene.sceneGraph.getGameObjects().size-1).remove();
        for (GameObject go: wallBoxGameObjects) go.remove();
        wallBoxGameObjects = new ArrayList<>();
    }

    private void continueLoading() {
        if (mundus.continueLoading()) {
            zebraMusic = mundus.getAssetManager().getGdxAssetManager().get("sounds/base5.mp3");
            zebraTalk = mundus.getAssetManager().getGdxAssetManager().get("sounds/zebraTalk.mp3");
            libGDXMusic = mundus.getAssetManager().getGdxAssetManager().get("sounds/base3.mp3");
            antzMusic = mundus.getAssetManager().getGdxAssetManager().get("sounds/antz.mp3");

            // Loading complete, load a scene.
            scene = mundus.loadScene("libGDX.mundus");
            scene.cam.position.set(91.60605f,5.2253003f,90.154625f);
            scene.cam.lookAt(100,5,100);
            scene.cam.up.set(Vector3.Y);
            scene.cam.update();
            scene.skybox.active = false;
            gameState = GameState.RENDER;
            setupSimulation();
        }
    }
}
