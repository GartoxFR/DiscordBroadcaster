package fr.gartox.broadcaster;


import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import fr.gartox.broadcaster.command.CommandMap;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by GartoxFR on 25/06/2019 for DiscordBroadcaster.
 */
public class DiscordBroadcaster implements Runnable {

    private JDA jda;
    private static DiscordBroadcaster instance;
    public Scanner scanner = new Scanner(System.in);
    private CommandMap commandMap;
    private MongoClient mongoClient;
    private MongoDatabase db;
    private boolean running;


    public static void main(String[] args) {
        instance = new DiscordBroadcaster(args);

    }

    public DiscordBroadcaster(String[] args) {
        String token = "default test bot token";
        if(args.length > 0) token = args[0];
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(token).build().awaitReady();
            running = true;
            commandMap = new CommandMap(this);
            mongoClient = new MongoClient("127.0.0.1");
            db = mongoClient.getDatabase("DiscordBroadcaster");
            jda.getEventManager().register(new DiscordBroadcasterListener(db));
            jda.getPresence().setActivity(Activity.playing("!help"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (LoginException e) {
            System.out.println("Connection Refused : InvalidToken");
        }
    }

    @Override
    public void run() {
        while(running) {
            if(scanner.hasNextLine()) commandMap.commandConsole(scanner.nextLine());
        }

        if (jda != null) jda.shutdownNow();
        scanner.close();
        System.out.println("Stopping the bot");
        System.exit(0);
    }

    public static DiscordBroadcaster getInstance() {
        return instance;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public MongoDatabase getDB() {
        return db;
    }

    public JDA getJda() {
        return jda;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }
}
