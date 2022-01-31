package service.transfer;

import ui.WindowFrame;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractServiceThread extends Thread {

    final AtomicBoolean cancel;

    public AbstractServiceThread(String threadName, AtomicBoolean cancel) {
        super(threadName);
        this.cancel = cancel;
    }

    void logMsg(String message, boolean isDebug) {
        String msg = String.format("%s - %s%n", this.getName(), message);
        WindowFrame.addMsg(msg, isDebug);
    }
}
