package com.qiuchris;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashListener extends ListenerAdapter {
    private final String ICON_URL = "https://cdn.discordapp.com/avatars/1166868697542045788/43f2e4d9190d1ba5309bd4adec6207f0.webp?size=160";
    private CourseTaskManager tm;

    public SlashListener(CourseTaskManager tm) {
        this.tm = tm;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        String userId = event.getUser().getId();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setFooter("ubc-bot", ICON_URL);
        if (event.getName().equals("add")) {
            String course = event.getOption("course").getAsString();
            String session = event.getOption("session").getAsString();
            String seatType = event.getOption("seat_type").getAsString();
            try {
                tm.addCourseTask(course, session, seatType, userId);
                eb.setColor(0x6568c2);
                eb.setDescription("Added `" + course + "` to your tracked courses.");
            } catch (IllegalArgumentException e) {
                eb.setColor(0xff0000);
                eb.setDescription("Unable to add the course to your tracked courses. Check the spelling of the " +
                        "course and try again, or open a ticket if you believe this is in error.");
            }
        } else if (event.getName().equals("remove")) {
            String course = event.getOption("course").getAsString();
            String session = event.getOption("session").getAsString();
            String seatType = event.getOption("seat_type").getAsString();
            try {
                tm.removeCourseTask(course, session, seatType, userId);
                eb.setColor(0x6568c2);
                eb.setDescription("Removed `" + course + "` from your tracked courses.");
            } catch (IllegalArgumentException e) {
                eb.setColor(0xff0000);
                eb.setDescription("Unable to remove the course from your tracked courses.");
            }
        } else if (event.getName().equals("courses")) {
            eb.setColor(0x6568c2);
            StringBuilder sb = new StringBuilder();
            try {
                for (CourseTask ct : tm.getUserIdTasks(userId)) {
                    sb.append("`" + ct.toString() + "`\n");
                }
                if (sb.length() > 0) {
                    eb.setDescription("Currently tracked courses:\n" + sb);
                } else {
                    eb.setDescription("No courses tracked.");
                }
            } catch (NullPointerException e) {
                eb.setDescription("No courses tracked.");
            }
        }
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }
}
