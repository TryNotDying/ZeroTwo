package com.TryNotDying.zerotwo.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.zerotwo.Bot;
import com.TryNotDying.zerotwo.audio.AudioHandler;
import com.TryNotDying.zerotwo.commands.AdminCommand;
import com.TryNotDying.zerotwo.settings.QueueType;
import com.TryNotDying.zerotwo.settings.Settings;

public class QueueTypeCmd extends AdminCommand
{
    public QueueTypeCmd(Bot bot)
    {
        super();
        this.name = "queuetype";
        this.help = "changes the queue type";
        this.arguments = "[" + String.join("|", QueueType.getNames()) + "]";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String args = event.getArgs();
        QueueType value;
        Settings settings = event.getClient().getSettingsFor(event.getGuild());

        if (args.isEmpty())
        {
            QueueType currentType = settings.getQueueType();
            event.reply(currentType.getEmoji() + " Current queue type is: `" + currentType.getUserFriendlyName() + "`.");
            return;
        }

        try
        {
            value = QueueType.valueOf(args.toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            event.replyError("Invalid queue type. Valid types are: [" + String.join("|", QueueType.getNames()) + "]");
            return;
        }

        if (settings.getQueueType() != value)
        {
            settings.setQueueType(value);

            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            if (handler != null)
                handler.setQueueType(value);
        }

        event.reply(value.getEmoji() + " Queue type was set to `" + value.getUserFriendlyName() + "`.");
    }
}
