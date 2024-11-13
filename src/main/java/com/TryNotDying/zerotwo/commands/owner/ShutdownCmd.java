package com.TryNotDying.zerotwo.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.commands.OwnerCommand;

public class ShutdownCmd extends OwnerCommand
{
    private final Bot bot;
    
    public ShutdownCmd(Bot bot)
    {
        this.bot = bot;
        this.name = "shutdown";
        this.help = "safely shuts down";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        event.replyWarning("Shutting down...");
        bot.shutdown();
    }
}
