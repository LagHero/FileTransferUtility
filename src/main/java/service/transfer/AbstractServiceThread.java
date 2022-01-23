package service.transfer;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractServiceThread extends Thread {

    final AtomicBoolean cancel;

    public AbstractServiceThread(String threadName, AtomicBoolean cancel) {
        super(threadName);
        this.cancel = cancel;
    }

    void logMsg(String msg) {
        System.out.printf("%s - %s%n", this.getName(), msg);
    }
}
