package com.antz.ode4libGDX.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CapsuleShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * @author JamesTKhan
 * @version October 01, 2022
 */
public class Utils3D {
    private static final Vector3 tmpVec = new Vector3();

    /**
     * Gets the current facing direction of transform, assuming the default forward is Z vector.
     *
     * @param transform modelInstance transform
     * @param out       out vector to be populated with direction
     */
    public static void getDirection(Matrix4 transform, Vector3 out) {
        tmpVec.set(Vector3.Z);
        out.set(tmpVec.rot(transform).nor());
    }

    /**
     * Gets the world position of modelInstance and sets it on the out vector
     *
     * @param transform modelInstance transform
     * @param out       out vector to be populated with position
     */
    public static void getPosition(Matrix4 transform, Vector3 out) {
        transform.getTranslation(out);
    }

    /**
     * Simple load of .obj model
     */
    public static Model loadOBJ(FileHandle fileHandle) {
        ObjLoader loader = new ObjLoader();
        return loader.loadModel(fileHandle);
    }

    public static Model buildCapsuleCharacter() {
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        Material bodyMaterial = new Material();
        bodyMaterial.set(ColorAttribute.createDiffuse(Color.YELLOW));

        Material armMaterial = new Material();
        armMaterial.set(ColorAttribute.createDiffuse(Color.BLUE));

        // Build the cylinder body
        MeshPartBuilder builder = modelBuilder.part("body", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, bodyMaterial);
        CapsuleShapeBuilder.build(builder, .5f, 2f, 12);

        // Build the arms
        builder = modelBuilder.part("arms", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, armMaterial);
        BoxShapeBuilder.build(builder, .5f, 0, 0f, .25f, 1f, .25f);
        BoxShapeBuilder.build(builder, -.5f, 0, 0f, .25f, 1f, .25f);

        // Hat
        builder.setVertexTransform(new Matrix4().trn(0, 1f, 0));
        ConeShapeBuilder.build(builder, .75f, .5f, .75f, 12);

        // Left Eye
        builder.setVertexTransform(new Matrix4().trn(-.15f, .5f, .5f));
        SphereShapeBuilder.build(builder, .15f, .15f, .15f, 12, 12);

        // Right Eye
        builder.setVertexTransform(new Matrix4().trn(.15f, .5f, .5f));
        SphereShapeBuilder.build(builder, .15f, .15f, .15f, 12, 12);

        // Finish building
        return modelBuilder.end();
    }

    public static int getVerticesIndicesFromModel(ModelInstance modelInstance, Array<Float> vertOut, Array<Integer> indexOut, int indicesOffset) {
        for (Mesh mesh : modelInstance.model.meshes) {
            VertexAttributes vertexAttributes = mesh.getVertexAttributes();
            int offset = vertexAttributes.getOffset(VertexAttributes.Usage.Position);

            int vertexSize = mesh.getVertexSize() / 4;
            int vertCount = mesh.getNumVertices() * mesh.getVertexSize() / 4;

            float[] vertices = new float[vertCount];
            short[] indices = new short[mesh.getNumIndices()];

            mesh.getVertices(vertices);
            mesh.getIndices(indices);

            // Get XYZ vertices position data
            for (int i = 0; i < vertices.length; i += vertexSize) {
                float x = vertices[i + offset];
                float y = vertices[i + 1 + offset];
                float z = vertices[i + 2 + offset];

                // Apply the world transform to the vertices
                tmpVec.set(x, y, z);
                tmpVec.mul(modelInstance.transform);

                vertOut.add(tmpVec.x);
                vertOut.add(tmpVec.y);
                vertOut.add(tmpVec.z);
            }

            for (short index : indices) {
                indexOut.add((int) index + indicesOffset);
            }

            indicesOffset += vertices.length / vertexSize;
        }
        return indicesOffset;
    }

    public static ModelInstance getModelFromVerticesIndices(Array<Float> vertices, Array<Integer> indices) {

        float[] vert = new float[vertices.size];
        int[] ind = new int[indices.size];
        for (int x = 0; x < vertices.size ; x++) vert[x] = vertices.get(x).floatValue();
        for (int x = 0; x < indices.size ; x++) ind[x] = indices.get(x).intValue();

        return  getModelFromVerticesIndices(vert, ind);
    }

    public static ModelInstance getModelFromVerticesIndices(float[] vertices, int[] indices) {
        Model model;
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        MeshPartBuilder meshPartBuilder = modelBuilder.part("line", 1, 3, new Material());
        meshPartBuilder.setColor(Color.CYAN);

        for (int ii = 0; ii < indices.length; ii += 3) {
            int v0 = indices[ii + 0] * 3;
            int v1 = indices[ii + 1] * 3;
            int v2 = indices[ii + 2] * 3;

            float[] a = new float[3], b = new float[3];
            float[] c = new float[3], d = new float[3];

            a[0] = vertices[v0];
            a[1] = vertices[v0 + 1];
            a[2] = vertices[v0 + 2];

            b[0] = vertices[v1];
            b[1] = vertices[v1 + 1];
            b[2] = vertices[v1 + 2];

            c[0] = vertices[v2];
            c[1] = vertices[v2 + 1];
            c[2] = vertices[v2 + 2];

            meshPartBuilder.line(a[0], a[1], a[2], b[0], b[1], b[2]);
            meshPartBuilder.line(b[0], b[1], b[2], c[0], c[1], c[2]);
            meshPartBuilder.line(c[0], c[1], c[2], a[0], a[1], a[2]);
            //dsDrawTriangle(Pos, Rot, Vertices, v0, v1, v2, true);
        }
        model = modelBuilder.end();
        return new ModelInstance(model);
    }
}
