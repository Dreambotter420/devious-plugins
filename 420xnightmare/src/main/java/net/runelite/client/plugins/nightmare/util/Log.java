package net.runelite.client.plugins.nightmare.util;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.unethicalite.api.utils.MessageUtils;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

@Slf4j
public class Log {
    public static void log(String toLog) {
        log.info(toLog);
        MessageUtils.addMessage(toLog, Color.BLUE);
    }
}
