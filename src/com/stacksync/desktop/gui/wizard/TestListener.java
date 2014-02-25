package com.stacksync.desktop.gui.wizard;

public interface TestListener {
    public void actionCompleted(boolean success);
    public void setError(Throwable e);
    public void setStatus(String s);
}
