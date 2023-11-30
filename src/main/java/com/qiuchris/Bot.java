package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.JDALogger;

import java.util.Scanner;

public class Bot {
    public static final String TASKS_PATH = "data/tasks.txt";
    public static final String COURSES_PATH = "data/courses.txt";
    public static final int DEFAULT_TIME = 300;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";

    private JDA jda;
    private CourseTaskManager tm;
    private Thread consoleThread;

    public static void main(String[] args) {
        String TOKEN = System.getenv("BOT_DISCORD_TOKEN");
        Bot b = new Bot(JDABuilder.createDefault(TOKEN).build());
        b.startConsole();
    }

    public Bot(JDA jda) {
        this.jda = jda;
        this.tm = new CourseTaskManager(jda);
        jda.addEventListener(new SlashListener(this, tm));
    }

    public void startConsole() {
        consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (!Thread.currentThread().isInterrupted() && scanner.hasNextLine()) {
                String input = scanner.nextLine();
                switch (input) {
                    case "exit":
                    case "stop":
                        stopServer();
                        break;
                    case "slash":
                        updateSlashCommands();
                        break;
                    case "resume":
                        tm.resumeTasks();
                        break;
                    case "update":
                        tm.updateValidator();
                        break;
                    default:
                        System.out.println("Unknown command: " + input);
                        break;
                }
            }
            scanner.close();
        });
        consoleThread.start();
    }

    public void updateSlashCommands() {
        jda.updateCommands().addCommands(
                Commands.slash("add", "Track a specific course.")
                        .addOption(OptionType.STRING, "course",
                                "The course you want to track. (ex. CPSC 310 L1D, MATH 100 1A2)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session",
                                        "The session that the course is in. (ex. 2023W, 2023S)", true)
                                        .addChoice("2023W", "2023W")
                                        .addChoice("2023S", "2023S")
                        ).addOptions(
                                new OptionData(OptionType.STRING, "seat_type",
                                        "Seat availability to track. (ex. Restricted, General)", true)
                                        .addChoice("Restricted", "Restricted")
                                        .addChoice("General", "General")
                                        .addChoice("Any", "Any")
                        ),
                Commands.slash("courses", "List your tracked courses."),
                Commands.slash("remove", "Stop tracking a specific course.")
                        .addOption(OptionType.STRING, "course",
                                "The course you want to remove. (ex. CPSC 310 L1D, MATH 100 1A2)", true)
                        .addOptions(
                                new OptionData(OptionType.STRING, "session",
                                        "The session that the course is in. (ex. 2023W, 2023S)", true)
                                        .addChoice("2023W", "2023W")
                                        .addChoice("2023S", "2023S")
                        ).addOptions(
                                new OptionData(OptionType.STRING, "seat_type",
                                        "Seat availability to track. (Restricted, General, Any)", true)
                                        .addChoice("Restricted", "Restricted")
                                        .addChoice("General", "General")
                                        .addChoice("Any", "Any")
                        ),
                Commands.slash("resume", "Resume tracking.")
        ).queue();
        JDALogger.getLog("Bot").info("Updated slash commands");
    }

    public void stopServer() {
        JDALogger.getLog("Bot").info("Server shutting down...");
        consoleThread.interrupt();
        tm.shutdown();
        jda.shutdown();
        System.exit(0);
    }
}