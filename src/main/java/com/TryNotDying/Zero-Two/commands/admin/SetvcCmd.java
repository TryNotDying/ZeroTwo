/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.TryNotDying.Zero-Two.commands.admin;

import java.util.List;
import com.TryNotDying.jdautilities.command.CommandEvent;
import com.TryNotDying.jdautilities.commons.utils.FinderUtil;
import com.TryNotDying.Zero-Two.Bot;
import com.TryNotDying.Zero-Two.commands.AdminCommand;
import com.TryNotDying.Zero-Two.settings.Settings;
import com.TryNotDying.Zero-Two.utils.FormatUtil;
import net.dv8tion.jda.api.entities.VoiceChannel;

/**
 * Above import dependencies
 * Below is the Set Voice Channel Command
 */
public class SetvcCmd extends AdminCommand 
{
    public SetvcCmd(Bot bot)
    {
        this.name = "setvc";
        this.help = "sets the voice channel for playing music";
        this.arguments = "<channel|NONE>";
        this.aliases = bot.getConfig().getAliases(this.name);
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        if(event.getArgs().isEmpty())
        {
            event.reply(event.getClient().getError()+" Please include a voice channel or NONE");
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if(event.getArgs().equalsIgnoreCase("none"))
        {
            s.setVoiceChannel(null);
            event.reply(event.getClient().getSuccess()+" Music can now be played in any channel");
        }
        else
        {
            List<VoiceChannel> list = FinderUtil.findVoiceChannels(event.getArgs(), event.getGuild());
            if(list.isEmpty())
                event.reply(event.getClient().getWarning()+" No Voice Channels found matching \""+event.getArgs()+"\"");
            else if (list.size()>1)
                event.reply(event.getClient().getWarning()+FormatUtil.listOfVChannels(list, event.getArgs()));
            else
            {
                s.setVoiceChannel(list.get(0));
                event.reply(event.getClient().getSuccess()+" Music can now only be played in "+list.get(0).getAsMention());
            }
        }
    }
}