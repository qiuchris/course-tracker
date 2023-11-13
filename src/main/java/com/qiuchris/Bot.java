package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.SocketTimeoutException;

public class Bot {
    private JDA jda;
    private CourseTaskScheduler ts;
    private CourseTaskManager tm;
    private SlashListener sl;

    public static final String TASKS_PATH = "data/tasks.txt";
    public static final String COURSE_CODES_PATH = "data/course_codes.txt";

    public static void main(String[] args) {
        String TOKEN = System.getenv("BOT_DISCORD_TOKEN");
        new Bot(JDABuilder.createDefault(TOKEN).build());
        // TODO: add console interaction
    }

    public Bot(JDA jda) {
        this.jda = jda;
        this.ts = new CourseTaskScheduler(this);
        this.tm = new CourseTaskManager(ts);
        this.sl = new SlashListener(this, tm, ts);
        jda.addEventListener(sl);
//        updateSlashCommands();
//        ts.loadTasksFromFile();
    }

    public void sendAvailableMessage(CourseTask ct, String url) {
        jda.getUserById(ct.getUserId()).openPrivateChannel().flatMap(channel ->
                channel.sendMessage("<@" + ct.getUserId() + "> A " + ct.getSeatType() + " seat for " +
                        ct.getSubjectCode() + " " + ct.getCourseNumber() + " " + ct.getSectionNumber() +
                        " is available. Register here: " + url)).queue();
        JDALogger.getLog("Bot").info("Notifying " + ct.getUserId() + " for " + ct.getSubjectCode() + " " +
                ct.getCourseNumber() + " " + ct.getSectionNumber());
    }

    public void checkCourse(CourseTask ct) {
        String url = "https://courses.students.ubc.ca/cs/courseschedule?sesscd=" + ct.getSession() +
                "&pname=subjarea&tname=subj-section&course=" + ct.getCourseNumber() +
                "&sessyr=" + ct.getYear() + "&section=" + ct.getSectionNumber() + "&dept=" + ct.getSubjectCode();
        try {
            JDALogger.getLog("Bot").info("Checking SSC at url: " + url);
            Document d = Jsoup.connect(url).timeout(3000).get();
            if (Integer.parseInt(d.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > strong")
                    .get(0).text()) > 0) {
                sendAvailableMessage(ct, url);
                ts.cancelTask(ct);
            }
        } catch (SocketTimeoutException e) {
            JDALogger.getLog("Bot").error("SocketTimeoutException checking url: " + url);
        } catch (Exception e) {
            JDALogger.getLog("Bot").error("Failed to check SSC at url: " + url);
        }
    }

    public void stopServer() {
        JDALogger.getLog("Bot").info("Server shutting down...");
        ts.shutdown();
        jda.shutdown();
    }

    public void updateSlashCommands() {
        jda.updateCommands().addCommands(
                Commands.slash("add", "Track a specific course.")
                        .addOption(OptionType.STRING, "course",
                                "The course you want to track. (ex. CPSC 310 L1D, MATH 100 1A2)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session",
                                        "The session that the course is in. (ex. Winter, Summer)", true)
                                        .addChoice("2023W", "2023W")
                                        .addChoice("2023S", "2023S")
                        ).addOptions(
                                new OptionData(OptionType.STRING, "seat_type",
                                        "Seat availability to track. (ex. Restricted, General)", true)
                                        .addChoice("Restricted", "Restricted")
                                        .addChoice("General", "General")
                        ),
                Commands.slash("courses", "List your tracked courses."),
                Commands.slash("remove", "Remove a specific course.")
                        .addOption(OptionType.STRING, "course",
                                "The course you want to remove. (ex. CPSC 310 L1D, MATH 100 1A2)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session",
                                        "The session that the course is in. (ex. Winter, Summer)", true)
                                        .addChoice("2023W", "2023W")
                                        .addChoice("2023S", "2023S")
                        ).addOptions(
                                new OptionData(OptionType.STRING, "seat_type",
                                        "Seat availability to track. (ex. Restricted, General)", true)
                                        .addChoice("Restricted", "Restricted")
                                        .addChoice("General", "General")
                        ),
                Commands.slash("resume", "Resume tracking."),
                Commands.slash("stop", "Stop server.")
        ).queue();
        JDALogger.getLog("Bot").info("Updated slash commands");
    }
}