package org.abi.omx;

/**
 * Created by abi on 04.05.15.
 */
public class OmxException extends Exception {
    public OmxException(String message) {
        super(message);
    }

//    public OmxException(SQLException e) {
//        super(e);
//    }

    public OmxException(Exception e) {
        super(e);
    }
}
