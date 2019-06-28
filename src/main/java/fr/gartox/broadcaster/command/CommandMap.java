package fr.gartox.broadcaster.command;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import fr.gartox.broadcaster.DiscordBroadcaster;
import fr.gartox.broadcaster.command.commands.CommandDefault;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ewan on 07/07/2017.
 */
public final class CommandMap {

    private final DiscordBroadcaster discordBroadcaster;
    private final Map<String, SimpleCommand> commands = new HashMap<>();
    public static final String tag = "!";
    public static final String DEFAULT_DESCRIPTION = "Sans description";

    public CommandMap(DiscordBroadcaster discordBroadcaster) {
        this.discordBroadcaster = discordBroadcaster;
        registerCommands(new CommandDefault());
    }

    public void registerCommands(Object... objects) {
        for (Object object : objects)registerCommand(object);
    }

    public void registerCommand(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if(method.isAnnotationPresent(Command.class)){
                Command command = method.getAnnotation(Command.class);
                method.setAccessible(true);
                SimpleCommand simpleCommand = new SimpleCommand(command.name(), command.description(), command.type(), object, method);
                commands.put(command.name(), simpleCommand);
            }
        }
    }

    public void commandConsole(String command) {
        Object[] objects = getCommand(command);
        if(objects[0] == null || ((SimpleCommand)objects[0]).getExecutorType() == Command.ExecutorType.USER) {
            return;
        }
        try {
            execute(((SimpleCommand)objects[0]), command, (String[])objects[1], null);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public boolean commandUser(User user, String command, Message message){
        Object[] object = getCommand(command);
        if(object[0] == null || ((SimpleCommand)object[0]).getExecutorType() == Command.ExecutorType.CONSOLE)return false;
        try {
            execute(((SimpleCommand) object[0]), command, ((String[]) object[1]), message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void execute(SimpleCommand simpleCommand, String command, String[] args, Message message) throws Exception {
        Parameter[] parameters = simpleCommand.getMethod().getParameters();
        Object[]  objects = new Object[parameters.length];
        for(int i = 0; i < parameters.length; i++){
            if(parameters[i].getType() == String[].class) objects[i] = args;
            else if(parameters[i].getType() == User.class) objects[i] = message == null ? null : message.getAuthor();
            else if(parameters[i].getType() == TextChannel.class) objects[i] = message == null ? null : message.getTextChannel();
            else if(parameters[i].getType() == PrivateChannel.class) objects[i] = message == null ? null : message.getPrivateChannel();
            else if(parameters[i].getType() == Guild.class) objects[i] = message == null ? null : message.getGuild();
            else if(parameters[i].getType() == String.class) objects[i] = command;
            else if(parameters[i].getType() == Message.class) objects[i] = message == null ? null : message;
            else if(parameters[i].getType() == JDA.class) objects[i] = discordBroadcaster.getJda();
            else if(parameters[i].getType() == MessageChannel.class) objects[i] = message == null ? null : message.getChannel();
            else if(parameters[i].getType() == MongoDatabase.class) objects[i] = DiscordBroadcaster.getInstance().getDB();
        }

        simpleCommand.getMethod().setAccessible(true);
        simpleCommand.getMethod().invoke(simpleCommand.getObject(), objects);
    }

    private Object[] getCommand(String command) {
        String[] commandSplit = command.split(" ");
        String[] args = new String[commandSplit.length-1];
        for (int i = 1; i < commandSplit.length; i++) args[i-1] = commandSplit[i];
        SimpleCommand simpleCommand = commands.get(commandSplit[0]);
        return  new Object[]{simpleCommand, args};
    }

    public Collection<SimpleCommand> getCommands() {
        return commands.values();
    }

    public String getTag() {
        return tag;
    }
}
