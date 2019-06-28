package fr.gartox.broadcaster.command.commands;

import com.mongodb.*;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import fr.gartox.broadcaster.DiscordBroadcaster;
import fr.gartox.broadcaster.command.Command;
import fr.gartox.broadcaster.command.CommandMap;
import fr.gartox.broadcaster.command.SimpleCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;

import static com.mongodb.client.model.Filters.*;

/**
 * Created by Ewan on 07/07/2017.
 */
public class CommandDefault {

    @Command(name = "stop", type = Command.ExecutorType.CONSOLE)
    public void stop(String command) {
        DiscordBroadcaster.getInstance().setRunning(false);
    }

    @Command(name = "create", type = Command.ExecutorType.USER, description = "Make this channel a broadcaster. Usage : create <name> <description>")
    public void create(TextChannel channel, Guild guild, User user, String[] args, MongoDatabase db) {
        if (!guild.getMember(user).hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage("You need to be administrator to do that").queue();
            return;
        }

        if (args.length < 2) {
            channel.sendMessage("Wrong arguments. Usage : create <name> <description>").queue();
            return;
        }
        if (db.getCollection("channels").find(or(eq("name", args[0]), eq("textChannelId", channel.getId()))).first() != null) {
            channel.sendMessage("This name name is aldready used or this channel is already registered as broadcaster").queue();
            return;
        }

        if (db.getCollection("subscriptions").find(eq("subsrciberChannelId", channel.getId())).first() != null) {
            channel.sendMessage("This channel is subscribed to a broadcaster so it can't be one itself. Use !unsubscribe first if you want to change").queue();
            return;
        }
        Map<String, Object> map = new HashMap();
        map.put("textChannelId", channel.getId());
        map.put("name", args[0]);
        map.put("description", String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        map.put("subscribers", 0);
        db.getCollection("channels").insertOne(new Document(map));
        channel.sendMessage("Broadcast channel successfully created").queue();
    }

    @Command(name = "list", type = Command.ExecutorType.USER, description = "Print the list of top broadcast channel based on their subscribers")
    public void list(TextChannel channel, Guild guild, User user, String[] args, MongoDatabase db) {
        EmbedBuilder embed = new EmbedBuilder().setTitle("Most subscribed channels").setColor(new Color(147, 13, 111));
        int i = 0;
        for (Document document : db.getCollection("channels").find().sort(Sorts.descending("subscribers"))) {
            if (i == 10) break;
            embed.addField(document.get("name").toString(), document.get("description").toString(), false);
            i++;
        }
        channel.sendMessage(embed.build()).queue();
    }

    @Command(name = "info", type = Command.ExecutorType.USER, description = "Print for the informations of a given channel. Usage : info <name>")
    public void info(TextChannel channel, Guild guild, User user, String[] args, MongoDatabase db) {
        if (args.length < 1) {
            channel.sendMessage("Please, specify a channel name").queue();
            return;
        }

        Document broadcastChannel = db.getCollection("channels").find(eq("name", args[0])).first();

        if (broadcastChannel == null) {
            channel.sendMessage("Channel with name " + args[0] + " does not exist").queue();
            return;
        }

        channel.sendMessage(new EmbedBuilder().setTitle(broadcastChannel.get("name").toString())
                .setDescription(broadcastChannel.get("description").toString())
                .setColor(new Color(147, 13, 111))
                .addField("Subscribers : ", broadcastChannel.get("subscribers").toString(), true)
                .build())
        .queue();
    }

    @Command(name = "subscribe", type = Command.ExecutorType.USER, description = "Subscribe this chanel to a broadcaster. Usage : subscribe <name>")
    public void subscribe(TextChannel channel, Guild guild, User user, String[] args, MongoDatabase db) {
        if (!guild.getMember(user).hasPermission(Permission.ADMINISTRATOR)) {
            channel.sendMessage("You need to be administrator to do that").queue();
            return;
        }

        if (db.getCollection("subscriptions").find(eq("subscriberChannelId", channel.getId())).first() != null) {
            channel.sendMessage("This channel is already subscribed to a broadcaster. Use !unsubscribe first if you want to change").queue();
            return;
        }

        if (db.getCollection("channels").find(eq("textChannelId", channel.getId())).first() != null) {
            channel.sendMessage("This channel is broadcaster. It can't subscribe to anything").queue();
            return;
        }

        if (args.length < 1) {
            channel.sendMessage("Please, specify a channel name").queue();
            return;
        }

        Document broadcastChannel = db.getCollection("channels").find(eq("name", args[0])).first();

        if (broadcastChannel == null) {
            channel.sendMessage("Channel with name " + args[0] + " does not exist").queue();
            return;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("broadcasterChannelId", broadcastChannel.get("textChannelId"));
        map.put("subscriberChannelId", channel.getId());
        db.getCollection("subscriptions").insertOne(new Document(map));
        db.getCollection("channels").updateOne(eq("textChannelId",broadcastChannel.get("textChannelId")), Updates.inc("subscribers", 1));

        channel.sendMessage("This channel is now subscribed to " + broadcastChannel.get("name")).queue();
    }

    @Command(name = "unsubscribe", type = Command.ExecutorType.USER, description = "Unsusbribe this chanel to a broadcaster. Usage : subscribe <name>")
    public void unsubscribe(TextChannel channel, Guild guild, User user, String[] args, MongoDatabase db) {
        Document sub = db.getCollection("subscriptions").find(eq("subscriberChannelId", channel.getId())).first();

        if (sub == null) {
            channel.sendMessage("This channel isn't subscribed to anything").queue();
            return;
        }


        db.getCollection("channels").updateOne(eq("textChannelId",sub.get("broadcasterChannelId")), Updates.inc("subscribers", -1));
        db.getCollection("subscriptions").deleteOne(eq("subscriberChannelId", channel.getId()));
        channel.sendMessage("This channel is no longer subscribed").queue();
    }

    @Command(name = "help", type = Command.ExecutorType.USER, description = "Show help menu")
    public void help(TextChannel channel) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("DiscordBroadcaster's help menu")
                .setDescription("Here is all commands")
                .setColor(Color.GREEN);

        DiscordBroadcaster.getInstance().getCommandMap().getCommands().stream().sorted(Comparator.comparing(SimpleCommand::getName)).forEach(simpleCommand -> {
            if(!simpleCommand.getDescription().equals(CommandMap.DEFAULT_DESCRIPTION)) {
                builder.addField(CommandMap.tag + simpleCommand.getName(), simpleCommand.getDescription(), false);
            }
        });
        channel.sendMessage(builder.build()).queue();

    }
}

