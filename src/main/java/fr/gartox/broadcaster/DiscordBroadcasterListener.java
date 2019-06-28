package fr.gartox.broadcaster;

import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.model.Updates;
import fr.gartox.broadcaster.command.CommandMap;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.bson.Document;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * Created by GartoxFR on 25/06/2019 for DiscordBroadcaster.
 */
public class DiscordBroadcasterListener implements EventListener {

    private MongoDatabase db;

    public DiscordBroadcasterListener(MongoDatabase database) {
        this.db = database;
    }

    @Override
    public void onEvent(@Nonnull GenericEvent event) {
        if(event instanceof MessageReceivedEvent)onMessage(((MessageReceivedEvent) event));
        if (event instanceof TextChannelDeleteEvent) onChannelDelete((TextChannelDeleteEvent) event);
    }

    private void onChannelDelete(TextChannelDeleteEvent event) {
        if (db.getCollection("channels").find(eq("textChannelId", event.getChannel().getId())).first() != null) {
            db.getCollection("subscriptions").deleteMany(eq("broadcasterChannelId", event.getChannel().getId()));
            db.getCollection("channels").deleteMany(eq("textChannelId", event.getChannel().getId()));
        }

        Iterator<Document> iterator = db.getCollection("subscriptions").find(eq("subscriberChannelId", event.getChannel().getId())).iterator();
        while (iterator.hasNext()) {
            Document doc = iterator.next();
            db.getCollection("channels").updateOne(eq("textChannelId", doc.get("broadcasterChannelId")), Updates.inc("subscribers", -1));
        }
        db.getCollection("subscriptions").deleteMany(eq("subscriberChannelId", event.getChannel().getId()));
    }

    private void onMessage(MessageReceivedEvent event) {
        if(event.getAuthor().equals(event.getJDA().getSelfUser()))return;
        String message = event.getMessage().getContentRaw();
        if(message.startsWith(DiscordBroadcaster.getInstance().getCommandMap().getTag())) {
            message = message.replaceFirst(DiscordBroadcaster.getInstance().getCommandMap().getTag(), "");
            if(DiscordBroadcaster.getInstance().getCommandMap().commandUser(event.getAuthor(), message, event.getMessage())) {
                if(event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE)) {
                    event.getMessage().delete().queue();
                }
            }
        } else {
            Document broadcastChannel = db.getCollection("channels").find(eq("textChannelId", event.getTextChannel().getId())).first();
            if (broadcastChannel != null) {
                Iterator<Document> iterator = db.getCollection("subscriptions").find(eq("broadcasterChannelId", event.getChannel().getId())).iterator();
                while (iterator.hasNext()) {
                    TextChannel subChannel = DiscordBroadcaster.getInstance().getJda().getTextChannelById(iterator.next().get("subscriberChannelId").toString());
                    MessageBuilder builder = new MessageBuilder().setContent(message).stripMentions(event.getGuild());
                    for (Message.Attachment attachment : event.getMessage().getAttachments()) {
                        builder.append("\n" + attachment.getUrl());
                    }
                    subChannel.sendMessage(builder.build()).queue();
                }

            }
        }
    }
}
