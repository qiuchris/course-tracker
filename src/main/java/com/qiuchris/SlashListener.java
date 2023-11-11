package com.qiuchris;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class SlashListener extends ListenerAdapter {
    private Bot bot;
    private CourseTaskScheduler ts;

    public SlashListener(Bot b, CourseTaskScheduler ts) {
        this.bot = b;
        this.ts = ts;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // do nothing
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        if (event.getName().equals("add")) {
            event.deferReply().queue(); // thinking...
            String subjectCode = event.getOption("subject_code").getAsString();
            String courseNumber = event.getOption("course_number").getAsString();
            String sectionNumber = event.getOption("section_number").getAsString();
            String session = event.getOption("session").getAsString();

            ts.addTask(userId, subjectCode, courseNumber, sectionNumber, session,
                    ThreadLocalRandom.current().nextInt(10), 60, TimeUnit.SECONDS, true);
            event.getHook().sendMessage(subjectCode + " " + courseNumber + " " + sectionNumber + " added").queue();
        } else if (event.getName().equals("remove")) {
            event.deferReply().queue(); // thinking...
            String subjectCode = event.getOption("subject_code").getAsString();
            String courseNumber = event.getOption("course_number").getAsString();
            String sectionNumber = event.getOption("section_number").getAsString();
            String session = event.getOption("session").getAsString();

            ts.cancelTask(userId, subjectCode, courseNumber, sectionNumber, session);
            event.getHook().sendMessage(subjectCode + " " + courseNumber + " " + sectionNumber + " removed").queue();
        } else if (event.getName().equals("courses")) {
            StringBuilder sb = new StringBuilder("your courses: ");
            for (String str : ts.getUserIdTasks(userId)) {
                sb.append("\n");
                sb.append(ts.idToCourse(str));
            }
            event.reply("map size: " + ts.numTasks() + " " +
                    ts.getUserIdTasks(userId) + "\n" + sb.toString()).queue();
        } else if (event.getName().equals("resume")) {
            event.deferReply().queue();
            ts.loadTasksFromFile();
            event.getHook().sendMessage("resumed. map size:" + ts.numTasks()).queue();
        } else if (event.getName().equals("stop")) {
            event.reply("stopping server...").queue();
            bot.stopServer();
        }
    }
}
