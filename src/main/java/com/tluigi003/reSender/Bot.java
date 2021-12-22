/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tluigi003.reSender;

import com.vdurmont.emoji.EmojiManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.api.methods.ForwardMessage;
import org.telegram.telegrambots.api.methods.send.SendDocument;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.send.SendVideo;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author Acer
 */
public class Bot extends TelegramLongPollingBot {

    final long chatFrom;
    final long chatTo, chatReport;
    String token;
    public final String upVote, heart, downVote, drops, neutral;
    private final InlineKeyboardMarkup votingBoard;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    SQLiteJDBC db = new SQLiteJDBC();
    Editter edit = null;
    public boolean reserva;
    public long[] authID;
    public BackupRunner backup;

    @Override
    public String getBotUsername() {
        return "ReSenderBot";
    }

    @Override
    public String getBotToken() {
        return token;
    }

//TODO comprobacion para evitar duplicados
    public void sendImageFromUrl(String url, long chatId) {
        // Create send method
        SendPhoto sendPhotoRequest = new SendPhoto();
        // Set destination chat id
        sendPhotoRequest.setChatId(chatId);
        // Set the photo url as a simple photo
        sendPhotoRequest.setPhoto(url);
        sendPhotoRequest.setReplyMarkup(votingBoard);
        try {
            // Execute the method
            Message men = sendPhoto(sendPhotoRequest);
            db.saveMessage(men.getMessageId(), url, men.getDate());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendVideoFromUrl(String url, long chatId) throws TelegramApiException {
        // Create send method
        SendVideo send = new SendVideo();
        // Set destination chat id
        send.setChatId(chatId);
        // Set the photo url as a simple photo
        send.setVideo(url);
        send.setReplyMarkup(votingBoard);
        // Execute the method
        sendVideo(send);
    }

    public void sendDocumentFromUrl(String url, long chatId) {
        // Create send method
        SendDocument send = new SendDocument();
        // Set destination chat id
        send.setChatId(chatId);
        // Set the photo url as a simple photo
        send.setDocument(url);
        send.setReplyMarkup(votingBoard);
        try {
            // Execute the method
            sendDocument(send);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public Bot postInit() {
        if (System.getenv("prod") != null) {
            edit = new Editter(this);
        }
        if (!reserva) {
            new Thread(edit).start();
        }
        new Timer().scheduleAtFixedRate(new StatsProvider(this), 0, 7200000);
        backup = new BackupRunner(this);
        //new Timer().scheduleAtFixedRate(backup, 0, chatTo);
        //new Thread(new InfoGatherer(this)).start();
        return this;
    }

    public void ReSendMessage(int MessageID) {
        try {
            execute(new ForwardMessage(chatReport, chatTo, MessageID));
        } catch (TelegramApiException ex) {
            Logger.getLogger(Bot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean checkURL(String url) {
        return url.contains("http"); // Really basic check, more complex check inside
    }

    @Override
    public void onUpdateReceived(Update update) {

        System.out.println("Update " + update.getUpdateId() + " recived at " + System.currentTimeMillis());
        if (update.hasCallbackQuery()
                || (update.hasMessage()
                && update.getMessage().hasPhoto()
                && update.getMessage().getChat().getId().equals(chatFrom)
                && update.getMessage().getCaption() == null)
                || (update.hasMessage()
                && update.getMessage().hasDocument()
                && update.getMessage().getDocument().getMimeType().equals("video/mp4")
                && update.getMessage().getChat().getId().equals(chatFrom)
                && update.getMessage().getCaption() == null)
                || (update.hasMessage()
                && update.getMessage().getVideo() != null
                && update.getMessage().getChat().getId().equals(chatFrom)
                && update.getMessage().getCaption() == null)
                || (update.hasMessage()
                && chatAuth(update.getMessage().getChatId())
                && update.getMessage().hasText()
                && update.getMessage().getText().charAt(0) == '/')
                || (update.hasMessage()
                && update.getMessage().hasText()
                && this.checkURL(update.getMessage().getText()))) {
            pool.execute(new RequestDispatcher(update, this));
        }
    }

    public boolean chatAuth(long chat) {
        for (long l : authID) {
            if (l == chat) {
                return true;
            }
        }
        return false;
    }

    private InlineKeyboardMarkup makeKeyboard() {
        return makeKeyboard(0, 0, 0, 0, 0);
    }

    public InlineKeyboardMarkup makeKeyboard(int up, int down, int heart, int drops, int neutral) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(new InlineKeyboardButton().setText(upVote + " " + up).setCallbackData("+1"));
        rowInline.add(new InlineKeyboardButton().setText(downVote + " " + down).setCallbackData("-1"));
        rowInline.add(new InlineKeyboardButton().setText(this.neutral + " " + neutral).setCallbackData("neutral"));
        rowInline.add(new InlineKeyboardButton().setText(this.heart + " " + heart).setCallbackData("heart"));
        rowInline.add(new InlineKeyboardButton().setText(this.drops + " " + drops).setCallbackData("drops"));
        // Set the keyboard to the markup
        rowsInline.add(rowInline);
        // Add it to the message
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    public Bot() throws FileNotFoundException {

        if (System.getenv("prod") != null && System.getenv("prod").equals("true")) {
            chatTo = new Long(System.getenv("chatTo"));
            chatFrom = new Long(System.getenv("chatFrom"));
            upVote = EmojiManager.getForAlias("+1").getUnicode();
            downVote = EmojiManager.getForAlias("-1").getUnicode();
            heart = EmojiManager.getForAlias("heart").getUnicode();
            drops = EmojiManager.getForAlias("sweat_drops").getUnicode();
            neutral = EmojiManager.getForAlias("male_shrug").getUnicode();
            votingBoard = makeKeyboard();
            this.token = System.getenv("token");
            this.reserva = false;
            this.chatReport = new Long(System.getenv("publishUpdatesOn"));
            authID = new long[new Integer(System.getenv("authNum"))];
            String a;
            for (int i = 0; (a = System.getenv("auth" + i)) != null; i++) {
                authID[i] = new Long(a);
            }
        } else {
            String content = new Scanner(new File("settings.json")).useDelimiter("\\Z").next();
            JSONObject settings = new JSONObject(content);
            chatTo = settings.getLong("chatTo");
            chatFrom = settings.getLong("chatFrom");
            upVote = EmojiManager.getForAlias("+1").getUnicode();
            downVote = EmojiManager.getForAlias("-1").getUnicode();
            heart = EmojiManager.getForAlias("heart").getUnicode();
            drops = EmojiManager.getForAlias("sweat_drops").getUnicode();
            neutral = EmojiManager.getForAlias("male_shrug").getUnicode();
            votingBoard = makeKeyboard();
            this.token = settings.getString("token");
            this.reserva = settings.getBoolean("reserva");
            this.chatReport = settings.getLong("publishUpdatesOn");
            JSONArray array = settings.getJSONArray("authChats");
            authID = new long[array.length()];
            for (int i = 0; i < array.length(); i++) {
                authID[i] = array.getJSONObject(i).getLong("chatId");
            }
        }
    }
}
