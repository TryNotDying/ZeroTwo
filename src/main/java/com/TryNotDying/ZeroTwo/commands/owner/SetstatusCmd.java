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
package com.TryNotDying.ZeroTwo.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.ZeroTwo.Bot;
import com.TryNotDying.ZeroTwo.commands.OwnerCommand;
import net.dv8tion.jda.api.OnlineStatus;

/**
 * Above import dependencies
 * Below is the set status command
 */
public class SetstatusCmd extends OwnerCommand
{
    public SetstatusCmd(Bot bot)
    {
        this.name = "setstatus";
        this.help = "sets the status Zero-Two displays";
        this.arguments = "<status>";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.guildOnly = false;
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        try {
            OnlineStatus status = OnlineStatus.fromKey(event.getArgs());
            if(status==OnlineStatus.UNKNOWN)
            {
                event.replyError("Please include one of the following statuses: `ONLINE`, `IDLE`, `DND`, `INVISIBLE`, `STREAMING`, `PLAYING`");
            }
            else
            {
                event.getJDA().getPresence().setStatus(status);
                event.replySuccess("Set the status to `"+status.getKey().toUpperCase()+"`");
            }
        } catch(Exception e) {
            event.reply(event.getClient().getError()+" The status could not be set!");
        }
    }
}
