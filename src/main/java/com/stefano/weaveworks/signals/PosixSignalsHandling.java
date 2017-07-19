package com.stefano.weaveworks.signals;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Author stefanofranz
 */
public class PosixSignalsHandling {

    public static void registerHandler(String signalName, SignalHandler handler) {
        Signal signal = new Signal(signalName);
        Signal.handle(signal, handler);
    }

}
