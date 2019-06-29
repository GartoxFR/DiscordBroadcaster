# DiscordBroadcaster

First here is the invite link :  https://discordapp.com/oauth2/authorize?client_id=594295875602481156&permissions=8&scope=bot 

This bot uses MongoDB. That's why it's better to use the version already hosted. If you want to host it yourself you'll need to install a local MondoDB configured like this : A database called `DiscordBroadcaster` with two collections called `channels` and `subscriptions`.

# What does this bot do ?  

This bot allows the creation of broadcast channels with the command `!create <name> <description>`.  
Next, other channels will be able to subscribe to the broadcaster using `!subscribe <name>`.  
When a message is sent in the broadcast channel, it will be sent by the bot in all other subscribers. It can be useful to share informations. We can imagine an announcement channel that can make announcement in multiple server by writing only one message. We can also imagine that you have a lot of memes to share with other people so you can create a broadcast channel to allow people to see your beautiful memes. 
