package com.sweatsenhancements.chat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Keeps the last N chat lines, both raw (with section codes) and formatted.
 *
 * The probe needs this because chat is where most Fakepixel information arrives, and the
 * raw form with colour codes is what patterns get built against.
 *
 * Every entry point is wrapped: a chat hook that throws would spam the log all game.
 */
public class ChatLog {

    public static final int MAX = 1000;

    private final ArrayDeque<String> raw = new ArrayDeque<String>();
    private final ArrayDeque<String> formatted = new ArrayDeque<String>();
    private final ArrayDeque<String> actionBar = new ArrayDeque<String>();

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        try {
            if (event == null || event.message == null) return;
            // Forge sends position-2 chat (the action bar) through the same event.
            if (event.type == 2) {
                addActionBar(event.message.getUnformattedText(), event.message.getFormattedText());
                return;
            }
            add(event.message.getUnformattedText(), event.message.getFormattedText());
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] chat hook failed: " + t);
        }
    }

    public synchronized void add(String rawLine, String formattedLine) {
        try {
            raw.addLast(rawLine == null ? "" : rawLine);
            formatted.addLast(formattedLine == null ? "" : formattedLine);
            while (raw.size() > MAX) raw.removeFirst();
            while (formatted.size() > MAX) formatted.removeFirst();
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] chat store failed: " + t);
        }
    }

    public synchronized void addActionBar(String rawLine, String formattedLine) {
        try {
            actionBar.addLast(rawLine == null ? "" : rawLine);
            while (actionBar.size() > 400) actionBar.removeFirst();
        } catch (Throwable t) {
            System.out.println("[SweatsEnhancements] action bar store failed: " + t);
        }
    }

    public synchronized List<String> actionBarTail(int n) {
        List<String> all = new ArrayList<String>(actionBar);
        if (n <= 0 || all.size() <= n) return all;
        return new ArrayList<String>(all.subList(all.size() - n, all.size()));
    }

    public synchronized int actionBarSize() {
        return actionBar.size();
    }

    public synchronized List<String> raw() {
        return new ArrayList<String>(raw);
    }

    public synchronized List<String> formatted() {
        return new ArrayList<String>(formatted);
    }

    public synchronized List<String> tail(int n) {
        List<String> all = raw();
        if (n <= 0 || all.size() <= n) return all;
        return new ArrayList<String>(all.subList(all.size() - n, all.size()));
    }

    public synchronized int size() {
        return raw.size();
    }

    public synchronized void clear() {
        raw.clear();
        formatted.clear();
        actionBar.clear();
    }
}
