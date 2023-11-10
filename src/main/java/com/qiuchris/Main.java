package com.qiuchris;

import net.dv8tion.jda.api.JDABuilder;

public class Main {
    public static void main(String[] args) {
        String TOKEN = System.getenv("BOT_DISCORD_TOKEN");
        new Bot(JDABuilder.createDefault(TOKEN).build());
    }
}
