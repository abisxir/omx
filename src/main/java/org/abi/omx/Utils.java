package org.abi.omx;

/**
 * Created by abi on 28.05.15.
 */
class Utils {
    public static String[] objectArrayToStringArray(Object[] objects) {
        String[] results = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            results[i] = objects[i].toString();
        }
        return results;
    }
}
