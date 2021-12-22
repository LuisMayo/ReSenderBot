/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tluigi003.reSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.telegram.telegrambots.api.methods.ForwardMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author luism
 */
public class InfoGatherer implements Runnable {

    private Bot bot;

    private void forwardThemAll() throws TelegramApiException {
        Message m;
        List<Integer> list = new ArrayList<>();
        //bot.execute(new ForwardMessage(bot.chatReport, bot.chatTo, bot.db.getAllMessages().get(0)));
        //bot.execute(new ForwardMessage(bot.chatReport, bot.chatTo, bot.db.getAllMessages().get(0)));
        //bot.execute(new ForwardMessage(bot.chatReport, bot.chatTo, bot.db.getAllMessages().get(0)));
        for (int i : bot.db.getAllMessages()) {
            
            if (!bot.db.isMessageAlready(i)) {
                list.add(i);
            }
        }
        for (int i : list) {
            try {
                m = bot.execute(new ForwardMessage(bot.chatReport, bot.chatTo, i));
                List<PhotoSize> photos = m.getPhoto();
                String f_id = photos.stream()
                        .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                        .findFirst()
                        .orElse(null).getFileId();
                bot.db.saveMessage(i, f_id, m.getForwardDate());
            } catch (TelegramApiException ex) {
                //Logger.getLogger(Bot.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void run() {
        try {
            forwardThemAll();
        } catch (TelegramApiException ex) {
            //Logger.getLogger(InfoGatherer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public InfoGatherer(Bot bot) {
        this.bot = bot;
    }

}
