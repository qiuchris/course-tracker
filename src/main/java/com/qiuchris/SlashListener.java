package com.qiuchris;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashListener extends ListenerAdapter {
    private CourseTaskManager tm;

    public SlashListener(CourseTaskManager tm) {
        this.tm = tm;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        if (event.getName().equals("add")) {
            event.deferReply().queue();
            String course = event.getOption("course").getAsString();
            String session = event.getOption("session").getAsString();
            String seatType = event.getOption("seat_type").getAsString();
            try {
                tm.addCourseTask(course, session, seatType, userId);
                event.getHook().sendMessage(event.getOption("course").getAsString() + " added").queue();
            } catch (IllegalArgumentException e) {
                event.getHook().sendMessage("Invalid course").queue();
            }
        } else if (event.getName().equals("remove")) {
            event.deferReply().queue();
            String course = event.getOption("course").getAsString();
            String session = event.getOption("session").getAsString();
            String seatType = event.getOption("seat_type").getAsString();
            try {
                tm.removeCourseTask(course, session, seatType, userId);
                event.getHook().sendMessage(event.getOption("course").getAsString() + " removed").queue();
            } catch (IllegalArgumentException e) {
                event.getHook().sendMessage("Invalid course").queue();
            }
        } else if (event.getName().equals("courses")) {
            StringBuilder sb = new StringBuilder("your courses: ");
            for (CourseTask ct : tm.getUserIdTasks(userId)) {
                sb.append("\n");
                sb.append(ct.toString());
            }
            event.reply("map size: " + tm.numTasks() + "\n" + sb).queue();
        }
    }
}
