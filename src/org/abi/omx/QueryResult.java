package org.abi.omx;

/**
 * Created by abi on 19.05.15.
 */
public interface QueryResult<T> extends Iterable<T> {
    int count();

    T get(int index);
}
