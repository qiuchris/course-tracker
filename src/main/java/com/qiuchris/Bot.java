package com.qiuchris;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.JDALogger;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Scanner;

public class Bot {
    public static final String TASKS_PATH = "data/tasks.txt";
    public static final String COURSES_PATH = "data/courses.txt";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    public static final String ICON_URL = "https://cdn.discordapp.com/avatars/1166868697542045788/" +
            "040b9225e2d99b80dd9a1b745ff5d97a.webp?size=160";
    public static final Proxy PROXY = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.1.89", 56908));

    public static int DEFAULT_TIME = 60;

    private JDA jda;
    private CourseTaskManager tm;

    public static void main(String[] args) {
        String TOKEN = System.getenv("BOT_DISCORD_TOKEN");
        Bot b = new Bot(JDABuilder.createDefault(TOKEN).build());
        b.startConsole();
    }

    public Bot(JDA jda) {
        File data = new File("data");
        if (!data.exists()) {
            data.mkdir();
        }

        this.jda = jda;
        this.tm = new CourseTaskManager(jda);
        jda.addEventListener(new SlashListener(tm));
        jda.getPresence().setPresence(OnlineStatus.IDLE,
                Activity.of(Activity.ActivityType.CUSTOM_STATUS, "tracking courses"));
    }

    public void startConsole() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                try {
                    String[] input = scanner.nextLine().split(" ");
                    switch (input[0]) {
                        case "stop":
                            stopServer();
                            continue;
                        case "slash":
                            updateSlashCommands();
                            break;
                        case "resume":
                            tm.resumeTasks();
                            System.out.println("map size: " + tm.numTasks());
                            break;
                        case "update":
                            tm.updateValidator();
                            break;
                        case "add":
                            switch (input[1]) {
                                case "task":
                                    try {
                                        tm.addCourseTask(input[2] + " " + input[3] + " " + input[4],
                                                input[5], input[6], input[7]);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        System.out.println("Invalid command. Usage: add course <course> <session> " +
                                                "<seat_type> <user_id>");
                                    } catch (CourseAvailableException e) {
                                        System.out.println("Course is currently available");
                                    }
                                    break;
                                case "course":
                                    tm.addValidCourse(input[2] + " " + input[3] + " " + input[4]);
                                    break;
                                default:
                                    System.out.println("Unknown command: " + input[1]);
                                    break;
                            }
                            break;
                        case "time":
                            DEFAULT_TIME = Integer.parseInt(input[1]);
                            System.out.println("set time to " + DEFAULT_TIME);
                            break;
                        default:
                            System.out.println("Unknown command: " + input);
                            break;
                    }
                } catch (Exception e) {
                    System.out.println("bad thing happened");
                }
            }
            scanner.close();
        }).start();
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
                                        "Seat availability to track. (General, Restricted, Any)", true)
                                        .addChoice("Any", SeatType.ANY.toString())
                                        .addChoice("General", SeatType.GENERAL.toString())
                                        .addChoice("Restricted", SeatType.RESTRICTED.toString())
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
                                        "Seat availability to stop tracking. (General, Restricted, Any)", true)
                                        .addChoice("Any", SeatType.ANY.toString())
                                        .addChoice("General", SeatType.GENERAL.toString())
                                        .addChoice("Restricted", SeatType.RESTRICTED.toString())
                        )
        ).queue();
        JDALogger.getLog("Bot").info("Updated slash commands.");
    }

    public void stopServer() {
        JDALogger.getLog("Bot").info("Server shutting down...");
        tm.shutdown();
        jda.shutdown();
        System.exit(0);
    }
}