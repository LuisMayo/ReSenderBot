/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tluigi003.reSender;

import static java.lang.Math.toIntExact;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author luism
 */
public class Editter implements Runnable {

    private final LinkedHashSet<Long> lista;
    Lock bloqueo = new ReentrantLock(true);
    private Bot bot;

    public Editter(Bot b) {
        bot = b;
        lista = bot.db.getUpdatableMessages();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                bloqueo.lock();
                System.out.println("Sistema apagandose");
                bot.db.saveUpdatableMessages(lista);
            }
        });
    }

    public void addLista(long l) {
        bloqueo.lock();
        try {
            lista.add(l);
        } finally {
            bloqueo.unlock();
        }
    }

    @Override
    public void run() {
        Long id;
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Editter.class.getName()).log(Level.SEVERE, null, ex);
            }

            bloqueo.lock();
            try {
                id = lista.iterator().next();
                lista.remove(id);
            } catch (NoSuchElementException e) {
                id = null;
            } finally {
                bloqueo.unlock();
            }
            try {
                if (id != null) {
                    EnumMap<Votes, Integer> votesByMessage = bot.db.getVotesByMessage(id);
                    InlineKeyboardMarkup makeKeyboard = bot.makeKeyboard(votesByMessage.getOrDefault(Votes.UP, 0), votesByMessage.getOrDefault(Votes.DOWN, 0), votesByMessage.getOrDefault(Votes.HEART, 0), votesByMessage.getOrDefault(Votes.DROPS, 0), votesByMessage.getOrDefault(Votes.NEUTRAL, 0));
                    EditMessageReplyMarkup new_message = new EditMessageReplyMarkup()
                            .setChatId(bot.chatTo)
                            .setMessageId(toIntExact(id))
                            .setReplyMarkup(makeKeyboard);
                    long time = System.currentTimeMillis();
                    bot.execute(new_message);
                    System.out.println((System.currentTimeMillis() - time) + "ms");
                }
            } catch (TelegramApiException ex) {
                Logger.getLogger(Editter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
