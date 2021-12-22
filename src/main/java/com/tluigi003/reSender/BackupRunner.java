/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tluigi003.reSender;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author luism
 */
public class BackupRunner extends TimerTask{
    private Bot bot;
    @Override
    public void run() {
        try {
            SendDocument send = new SendDocument();
            send.setChatId(bot.chatReport);
            send.setDocument("BotInfo.db");
            bot.sendDocument(send);
        } catch (TelegramApiException ex) {
            Logger.getLogger(BackupRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
     public BackupRunner(Bot bot) {
        this.bot = bot;
    }
    
}
