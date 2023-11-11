package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;

public class Bot {
    private JDA jda;
    private TaskScheduler ts;
    private SlashListener sl;

    public Bot(JDA jda) {
        this.jda = jda;
        this.ts = new TaskScheduler(this);
        this.sl = new SlashListener(this, ts);
        jda.addEventListener(sl);
        addCommands();

        File f = new File("tasks.txt");
        try {
            if (f.createNewFile())
                JDALogger.getLog("Bot").info("Created tasks.txt");
        } catch (IOException e) {
            JDALogger.getLog("Bot").info("Unable to create tasks.txt");
            throw new RuntimeException(e);
        }
    }

    public void sendAvailableMessage(String userId, String subjectCode, String courseNumber, String sectionNumber,
                                     String session, String url) {
        jda.getUserById(userId).openPrivateChannel().flatMap(channel ->
                channel.sendMessage("<@" + userId + "> " +
                        subjectCode + " " + courseNumber + " " +
                        sectionNumber + " is available. Register here: " + url)).queue();
        JDALogger.getLog("Bot").info("Notifying " + userId + " for " + subjectCode + " " + courseNumber + " " +
                sectionNumber);
    }

    public void checkCourse(String userId, String subjectCode, String courseNumber, String sectionNumber,
                         String session) {
        String url = "https://courses.students.ubc.ca/cs/courseschedule?sesscd=" + session +
                "&pname=subjarea&tname=subj-section&course=" + courseNumber +
                "&sessyr=2023&section=" + sectionNumber + "&dept=" + subjectCode;
        try {
            JDALogger.getLog("Bot").info("Checking SSC at url: " + url);
            Document d = Jsoup.connect(url).timeout(3000).get();
            if (Integer.parseInt(d.select("table > tbody > tr:nth-of-type(1) > td:nth-of-type(2) > strong")
                    .get(0).text()) > 0) {
                sendAvailableMessage(userId, subjectCode, courseNumber, sectionNumber, session, url);
                ts.cancelTask(userId, subjectCode, courseNumber, sectionNumber, session);
            }
        } catch (SocketTimeoutException e) {
            JDALogger.getLog("Bot").info("SocketTimeoutException checking url: " + url);
            e.printStackTrace();
        } catch (Exception e) {
            JDALogger.getLog("Bot").info("Failed to check SSC at url: " + url);
            e.printStackTrace();
        }
    }

    public void stopServer() {
        JDALogger.getLog("Bot").info("Server shutting down...");
        ts.shutdown();
        jda.shutdown();
    }

    public void addCommands() {
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
                                        .addChoice("Winter", "Winter")
                                        .addChoice("Summer", "Summer")
                        ),
                Commands.slash("resume", "Resume tracking."),
                Commands.slash("stop", "Stop server.")
        ).queue();
        JDALogger.getLog("Bot").info("Updated slash commands");
    }
}