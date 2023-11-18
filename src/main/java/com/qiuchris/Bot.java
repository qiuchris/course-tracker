package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.JDALogger;

public class Bot {
    public static final String TASKS_PATH = "data/tasks.txt";
    public static final String COURSE_CODES_PATH = "data/course_codes.txt";

    private JDA jda;
    private CourseTaskManager tm;

    public static void main(String[] args) {
        String TOKEN = System.getenv("BOT_DISCORD_TOKEN");
        new Bot(JDABuilder.createDefault(TOKEN).build());
        // TODO: add console interaction
    }

    public Bot(JDA jda) {
        this.jda = jda;
        this.tm = new CourseTaskManager(jda);
        jda.addEventListener(new SlashListener(this, tm));
//        updateSlashCommands();
    }

    public void stopServer() {
        JDALogger.getLog("Bot").info("Server shutting down...");
        tm.shutdown();
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