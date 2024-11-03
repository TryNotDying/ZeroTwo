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
package com.TryNotDying.ZeroTwo.commands.music;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.TryNotDying.jdautilities.command.CommandEvent;
import com.TryNotDying.jdautilities.menu.Paginator;
import com.TryNotDying.ZeroTwo.Bot;
import com.TryNotDying.ZeroTwo.audio.AudioHandler;
import com.TryNotDying.ZeroTwo.audio.QueuedTrack;
import com.TryNotDying.ZeroTwo.commands.MusicCommand;
import com.TryNotDying.ZeroTwo.settings.QueueType;
import com.TryNotDying.ZeroTwo.settings.RepeatMode;
import com.TryNotDying.ZeroTwo.settings.Settings;
import com.TryNotDying.ZeroTwo.utils.FormatUtil;
import com.TryNotDying.ZeroTwo.utils.TimeUtil;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

/**
 * Above import dependencies
 * Below is the current queue command
 */
public class QueueCmd extends MusicCommand 
{
    private final Paginator.Builder builder;
    
    public QueueCmd(Bot bot)
    {
        super(bot);
        this.name = "queue";
        this.help = "shows the current queue";
        this.arguments = "[pagenum]";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.bePlaying = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_ADD_REACTION,Permission.MESSAGE_EMBED_LINKS};
        builder = new Paginator.Builder()
                .setColumns(1)
                .setFinalAction(m -> {try{m.clearReactions().queue();}catch(PermissionException ignore){}})
                .setItemsPerPage(10)
                .waitOnSinglePage(false)
                .useNumberedItems(true)
                .showPageNumbers(true)
                .wrapPageEnds(true)
                .setEventWaiter(bot.getWaiter())
                .setTimeout(1, TimeUnit.MINUTES);
    }

    @Override
    public void doCommand(CommandEvent event)
    {
        int pagenum = 1;
        try
        {
            pagenum = Integer.parseInt(event.getArgs());
        }
        catch(NumberFormatException ignore){}
        AudioHandler ah = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
        List<QueuedTrack> list = ah.getQueue().getList();
        if(list.isEmpty())
        {
            Message nowp = ah.getNowPlaying(event.getJDA());
            Message nonowp = ah.getNoMusicPlaying(event.getJDA());
            Message built = new MessageBuilder()
                    .setContent(event.getClient().getWarning() + " There is no music in the queue!")
                    .setEmbeds((nowp==null ? nonowp : nowp).getEmbeds().get(0)).build();
            event.reply(built, m -> 
            {
                if(nowp!=null)
                    bot.getNowplayingHandler().setLastNPMessage(m);
            });
            return;
        }
        String[] songs = new String[list.size()];
        long total = 0;
        for(int i=0; i<list.size(); i++)
        {
            total += list.get(i).getTrack().getDuration();
            songs[i] = list.get(i).toString();
        }
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        long fintotal = total;
        builder.setText((i1,i2) -> getQueueTitle(ah, event.getClient().getSuccess(), songs.length, fintotal, settings.getRepeatMode(), settings.getQueueType()))
                .setItems(songs)
                .setUsers(event.getAuthor())
                .setColor(event.getSelfMember().getColor())
                ;
        builder.build().paginate(event.getChannel(), pagenum);
    }
    
    private String getQueueTitle(AudioHandler ah, String success, int songslength, long total, RepeatMode repeatmode, QueueType queueType)
    {
        StringBuilder sb = new StringBuilder();
        if(ah.getPlayer().getPlayingTrack()!=null)
        {
            sb.append(ah.getStatusEmoji()).append(" **")
                    .append(ah.getPlayer().getPlayingTrack().getInfo().title).append("**\n");
        }
        return FormatUtil.filter(sb.append(success).append(" Current Queue | ").append(songslength)
                .append(" entries | `").append(TimeUtil.formatTime(total)).append("` ")
                .append("| ").append(queueType.getEmoji()).append(" `").append(queueType.getUserFriendlyName()).append('`')
                .append(repeatmode.getEmoji() != null ? " | "+repeatmode.getEmoji() : "").toString());
    }
}
