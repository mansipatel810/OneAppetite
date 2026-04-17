package com.cts.mfrp.oa.util;

import javazoom.jl.player.Player;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class Mp3Player {

    public static void play(String resourcePath) {
        try {
            InputStream inputStream = Mp3Player.class.getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                System.out.println("MP3 file not found: " + resourcePath);
                return;
            }
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            Player player = new Player(bis);
            player.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
