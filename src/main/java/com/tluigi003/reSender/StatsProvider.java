/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tluigi003.reSender;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author luism
 */
public class StatsProvider extends TimerTask{
    Bot bot;
    @Override
    public void run() {
        for(Votes v: Votes.values()){
            int mensaje=bot.db.getTopMessage(v,2);
            if(mensaje!=bot.db.loadTopMessage(v)){
                try {
                    bot.execute(new SendMessage(bot.chatReport, "Photo with most "+v.name()+" votes has changed, new one has "+bot.db.getTopMessage(v,1)+ " votes"));
                    bot.ReSendMessage(mensaje);
                    bot.db.saveTopMessage(v, mensaje);
                } catch (TelegramApiException ex) {
                    Logger.getLogger(StatsProvider.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public StatsProvider(Bot bot){
        this.bot= bot;
    }
    
}
