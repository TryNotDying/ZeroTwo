package com.TryNotDying.zerotwo.settings;

import com.TryNotDying.zerotwo.queue.AbstractQueue;
import com.TryNotDying.zerotwo.queue.FairQueue;
import com.TryNotDying.zerotwo.queue.LinearQueue;
import com.TryNotDying.zerotwo.queue.Queueable;
import com.TryNotDying.zerotwo.queue.QueueSupplier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum QueueType
{
    LINEAR("\u23E9", "Linear", LinearQueue::new),     // ⏩
    FAIR("\uD83D\uDD22", "Fair", FairQueue::new);     // 🔢

    private final String userFriendlyName;
    private final String emoji;
    private final QueueSupplier supplier;

    QueueType(final String emoji, final String userFriendlyName, QueueSupplier supplier)
    {
        this.userFriendlyName = userFriendlyName;
        this.emoji = emoji;
        this.supplier = supplier;
    }

    public static List<String> getNames()
    {
        return Arrays.stream(QueueType.values())
                .map(type -> type.name().toLowerCase())
                .collect(Collectors.toList());
    }

    public <T extends Queueable> AbstractQueue<T> createInstance(AbstractQueue<T> previous)
    {
        return supplier.apply(previous);
    }

    public String getUserFriendlyName()
    {
        return userFriendlyName;
    }

    public String getEmoji()
    {
        return emoji;
    }
}
