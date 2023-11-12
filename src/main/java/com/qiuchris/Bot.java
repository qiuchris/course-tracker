package com.qiuchris;

import net.dv8tion.jda.api.JDA;
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
                channel.sendMessage("<@" + ct.getUserId() + "> " +
                        ct.getSubjectCode() + " " + ct.getCourseNumber() + " " +
                        ct.getSectionNumber() + " is available. Register here: " + url)).queue();
        JDALogger.getLog("Bot").info("Notifying " + ct.getUserId() + " for " + ct.getSubjectCode() + " " +
                ct.getCourseNumber() + " " + ct.getSectionNumber());
    }

    public void checkCourse(CourseTask ct) {
        String url = "https://courses.students.ubc.ca/cs/courseschedule?sesscd=" + ct.getSession() +
                "&pname=subjarea&tname=subj-section&course=" + ct.getCourseNumber() +
                "&sessyr=2023&section=" + ct.getSectionNumber() + "&dept=" + ct.getSubjectCode();
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
                        .addOption(OptionType.STRING, "subject_code",
                                "The subject code of the course. (ex. CPSC)", true)
                        .addOption(OptionType.STRING, "course_number",
                                "The course number of the course. (ex. 310)", true)
                        .addOption(OptionType.STRING, "section_number",
                                "The section number of the course. (ex. L1D)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session",
                                        "The session that the course is in. (ex. Winter, Summer)", true)
                                        .addChoice("Winter", "W")
                                        .addChoice("Summer", "S")
                        ),
                Commands.slash("courses", "List your tracked courses."),
                Commands.slash("remove", "Remove a specific course.")
                        .addOption(OptionType.STRING, "subject_code",
                                "The subject code of the course. (ex. CPSC)", true)
                        .addOption(OptionType.STRING, "course_number",
                                "The course number of the course. (ex. 310)", true)
                        .addOption(OptionType.STRING, "section_number",
                                "The section number of the course. (ex. L1D)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session",
                                        "The session that the course is in. (ex. Winter, Summer)", true)
                                        .addChoice("Winter", "W")
                                        .addChoice("Summer", "S")
                        ),
                Commands.slash("resume", "Resume tracking."),
                Commands.slash("stop", "Stop server.")
        ).queue();
        JDALogger.getLog("Bot").info("Updated slash commands");
    }
}