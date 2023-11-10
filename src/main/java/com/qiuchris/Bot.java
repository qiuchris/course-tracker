package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Bot {
    private JDA jda;
    private TaskScheduler ts;
    private SlashListener sl;

    public Bot(JDA jda) {
        this.jda = jda;
        this.ts = new TaskScheduler(this);
        this.sl = new SlashListener(this, ts);
        jda.addEventListener(sl);
//        addCommands();
    }

    public void addCommands() {
        jda.updateCommands().addCommands(
                Commands.slash("add", "Track a specific course.")
                        .addOption(OptionType.STRING, "subject_code", "The subject code of the course. (ex. CPSC)", true)
                        .addOption(OptionType.STRING, "course_number", "The course number of the course. (ex. 310)", true)
                        .addOption(OptionType.STRING, "section_number", "The section number of the course. (ex. L1D)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session", "The session that the course is in. (ex. Winter, Summer)", true)
                                        .addChoice("Winter", "W")
                                        .addChoice("Summer", "S")
                        ),
                Commands.slash("courses", "List your tracked courses."),
                Commands.slash("remove", "Remove a specific course.")
                        .addOption(OptionType.STRING, "subject_code", "The subject code of the course. (ex. CPSC)", true)
                        .addOption(OptionType.STRING, "course_number", "The course number of the course. (ex. 310)", true)
                        .addOption(OptionType.STRING, "section_number", "The section number of the course. (ex. L1D)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session", "The session that the course is in. (ex. Winter, Summer)", true)
                                        .addChoice("Winter", "W")
                                        .addChoice("Summer", "S")
                        ),
                Commands.slash("resume", "Resume tracking."),
                Commands.slash("stop", "Stop server.")
        ).queue();
    }

    public void sendAvailableMessage(String subjectCode, String courseNumber, String sectionNumber,
                                     String session, String userId, String url) {
        jda.getUserById(userId).openPrivateChannel().flatMap(channel ->
                channel.sendMessage("<@" + userId + "> " +
                        subjectCode + " " + courseNumber + " " +
                        sectionNumber + " is available. Register here: " + url)).queue();
        JDALogger.getLog("Bot").info("Notifying " + userId + " for " + subjectCode + " " + courseNumber + " " +
                sectionNumber);
    }

    public void checkSSC(String userId, String subjectCode, String courseNumber, String sectionNumber,
                         String session) {
        String url = "https://courses.students.ubc.ca/cs/courseschedule?sesscd=" + session +
                "&pname=subjarea&tname=subj-section&course=" + courseNumber +
                "&sessyr=2023&section=" + sectionNumber + "&dept=" + subjectCode;
        try {
            JDALogger.getLog("Bot").info("Checking SSC at url: " + url);
            Document d = Jsoup.connect(url).timeout(3000).get();
            if (Integer.parseInt(d.select("td > strong").get(0).text()) > 0) {
                sendAvailableMessage(subjectCode, courseNumber, sectionNumber,
                        session, userId, url);
                ts.cancelTask(userId, subjectCode, courseNumber, sectionNumber, session);
            }
        } catch (Exception e) {
            JDALogger.getLog("Bot").info("Failed to check SSC at url: " + url);
            e.printStackTrace();
        }
    }

    public void stopServer() {
        ts.shutdown();
        jda.shutdown();
    }
}