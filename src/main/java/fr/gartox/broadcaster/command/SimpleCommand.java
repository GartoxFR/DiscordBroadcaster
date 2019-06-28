package fr.gartox.broadcaster.command;

import java.lang.reflect.Method;

/**
 * Created by Ewan on 07/07/2017.
 */
public final class SimpleCommand {

    private final String name, description;
    private final Command.ExecutorType executorType;
    private Object object;
    private final Method method;

    public SimpleCommand(String name, String description, Command.ExecutorType executorType, Object object, Method method) {
        this.name = name;
        this.description = description;
        this.executorType = executorType;
        this.object = object;
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Command.ExecutorType getExecutorType() {
        return executorType;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Method getMethod() {
        return method;
    }
}
