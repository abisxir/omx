package org.abi.omx;

/**
 * Created by abi on 04.05.15.
 */
public abstract class Entity {
    public interface Factory {
        Entity create();
    }
}
