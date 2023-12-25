package com.qiuchris;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashListener extends ListenerAdapter {
    private CourseTaskManager tm;
    private TokenBucketLimiter tl;

    public SlashListener(CourseTaskManager tm) {
        this.tm = tm;
        this.tl = new TokenBucketLimiter(3, 5000);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        event.deferReply().queue();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setFooter("ubc bot", Bot.ICON_URL);
        if (tl.canUseToken(userId)) {
            tl.useToken(userId);
        } else {
            eb.setColor(0xff0000);
            eb.setDescription("You are sending commands too quickly! " +
                    "Please wait and try again.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }
        if (!event.isFromGuild()) {
            eb.setColor(0x6568c2);
            eb.setDescription("To use commands, join the Discord server linked in my About Me.");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }
        if (event.getName().equals("add")) {
            String course = event.getOption("course").getAsString();
            String session = event.getOption("session").getAsString();
            String seatType = event.getOption("seat_type").getAsString();
            try {
                if (tm.addCourseTask(course, session, seatType, userId)) {
                    eb.setColor(0x6568c2);
                    eb.setDescription("Added `" + course + "` to your tracked courses.");
                } else {
                    eb.setColor(0xff0000);
                    eb.setDescription("Too many courses are currently tracked! Remove some courses and try again.");
                }
            } catch (IllegalArgumentException e) {
                eb.setColor(0xff0000);
                eb.setDescription("Unable to add the course to your tracked courses. Check the spelling of the " +
                        "course and try again, or open a ticket if you believe this is in error.");
            } catch (CourseAvailableException e) {
                eb.setColor(0xff0000);
                eb.setDescription("The course is already available for registration. Check the spelling of the " +
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
                eb.setDescription("Unable to remove the course from your tracked courses. Check the spelling of the " +
                        "course and try again, or open a ticket if you believe this is in error.");
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
