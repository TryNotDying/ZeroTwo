/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
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
package com.TryNotDying.ZeroTwo.commands.admin;

import java.util.List;
import com.TryNotDying.jdautilities.command.CommandEvent;
import com.TryNotDying.jdautilities.commons.utils.FinderUtil;
import com.TryNotDying.ZeroTwo.Bot;
import com.TryNotDying.ZeroTwo.commands.AdminCommand;
import com.TryNotDying.ZeroTwo.settings.Settings;
import com.TryNotDying.ZeroTwo.utils.FormatUtil;
import net.dv8tion.jda.api.entities.Role;

/**
 * Above import dependencies
 * Below is Set DJ Role Command
 */
public class SetdjCmd extends AdminCommand
{
    public SetdjCmd(Bot bot)
    {
        this.name = "setdj";
        this.help = "sets the DJ role for certain music commands";
        this.arguments = "<rolename|NONE>";
        this.aliases = bot.getConfig().getAliases(this.name);
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        if(event.getArgs().isEmpty())
        {
            event.reply(event.getClient().getError()+" Please include a role name or NONE");
            return;
        }
        Settings s = event.getClient().getSettingsFor(event.getGuild());
        if(event.getArgs().equalsIgnoreCase("none"))
        {
            s.setDJRole(null);
            event.reply(event.getClient().getSuccess()+" DJ role cleared; Only Admins can use the DJ commands.");
        }
        else
        {
            List<Role> list = FinderUtil.findRoles(event.getArgs(), event.getGuild());
            if(list.isEmpty())
                event.reply(event.getClient().getWarning()+" No Roles found matching \""+event.getArgs()+"\"");
            else if (list.size()>1)
                event.reply(event.getClient().getWarning()+FormatUtil.listOfRoles(list, event.getArgs()));
            else
            {
                s.setDJRole(list.get(0));
                event.reply(event.getClient().getSuccess()+" DJ commands can now be used by users with the **"+list.get(0).getName()+"** role.");
            }
        }
    }
    
}
