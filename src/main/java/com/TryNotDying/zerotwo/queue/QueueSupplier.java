package com.TryNotDying.zerotwo.queue;

@FunctionalInterface
public interface QueueSupplier
{
    <T extends Queueable> AbstractQueue<T> apply(AbstractQueue<T> queue);
}
