package org.polygon.engine.core.scene;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

// Joint is a node class, collection of Joint objects represent the model's bone hierarchy.
// Used to calculate bone transformation matrices array sent to the vertex shader.
public class Joint {
    private final List<Joint> children;
    private final String name;
    private final Joint parent;
    private final Matrix4f transform;

    public Joint(String name, Joint parent, Matrix4f transform) {
        this.name = name;
        this.parent = parent;
        this.transform = transform;
        children = new ArrayList<>();
    }

    public void addChild(Joint node) {
        children.add(node);
    }

    public List<Joint> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public Matrix4f getTransform() {
        return transform;
    }

    public Joint getParent() {
        return parent;
    }
}
