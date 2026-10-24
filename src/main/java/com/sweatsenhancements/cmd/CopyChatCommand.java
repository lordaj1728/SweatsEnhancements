package com.sweatsenhancements.cmd;

import java.util.Collections;
import java.util.List;

import com.sweatsenhancements.feature.CopyChatMessage;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;

/** Client-only command behind a chat button; accepts an ID, never untrusted chat as a command. */
public final class CopyChatCommand implements ICommand {
    @Override public String getCommandName() { return "se-copy"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/se-copy <message-id>"; }
    @Override public List<String> getCommandAliases() { return Collections.emptyList(); }
    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }
    @Override public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return null;
    }
    @Override public boolean isUsernameIndex(String[] args, int index) { return false; }
    @Override public int compareTo(ICommand other) { return getCommandName().compareTo(other.getCommandName()); }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        try {
            if (args == null || args.length != 1 || !args[0].matches("[0-9]{1,18}")) {
                CommandBridge.reply(sender, EnumChatFormatting.RED + "Invalid copy button.");
                return;
            }
            String message = CopyChatMessage.find(Long.parseLong(args[0]));
            if (message == null) {
                CommandBridge.reply(sender, EnumChatFormatting.GRAY + "That chat line is no longer cached.");
                return;
            }
            GuiScreen.setClipboardString(message);
            CommandBridge.reply(sender, EnumChatFormatting.GREEN + "Message copied.");
        } catch (Throwable t) {
            CommandBridge.reply(sender, EnumChatFormatting.RED + "Could not copy message: " + t);
        }
    }
}
