package net.runelite.client.plugins.nightmare.util;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.chat.ChatMessageBuilder;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
public class Log {
    public static void log(String toLog) {
        log.info(toLog);
        addMessage(toLog, Color.BLUE);
    }
    public static Queue<String> msgs = new LinkedList<>();
    public static void addMessage(String message, Color color)
    {
        String chatMessage = new ChatMessageBuilder()
                .append(color, message)
                .build();
        msgs.add(chatMessage);
    }
}
