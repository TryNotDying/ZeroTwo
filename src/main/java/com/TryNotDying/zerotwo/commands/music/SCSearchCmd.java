package com.TryNotDying.zerotwo.commands.music;

import com.TryNotDying.zerotwo.Bot;

public class SCSearchCmd extends SearchCmd 
{
    public SCSearchCmd(Bot bot)
    {
        super(bot);
        this.searchPrefix = "scsearch:";
        this.name = "scsearch";
        this.help = "searches Soundcloud for a provided query";
        this.aliases = bot.getConfig().getAliases(this.name);
    }
}
