/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tluigi003.reSender;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.telegram.telegrambots.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.api.methods.ForwardMessage;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author luism
 */
public class RequestDispatcher implements Runnable {

    private final Bot bot;
    Update update;

    @Override
    public void run() {
        System.out.println("Update recived at Thread");
        //System.out.println(update.getMessage().getCaption());
//        System.out.println(Arrays.toString(update.getMessage().getCaption().getBytes()));
        if (false) {

        } else if (update.hasMessage()
                && update.getMessage().hasPhoto()
                && update.getMessage().getChat().getId().equals(bot.chatFrom)
                && update.getMessage().getCaption() == null) {
            System.out.println(update.getMessage().getCaption());
            List<PhotoSize> photos = update.getMessage().getPhoto();
            String f_id = photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null).getFileId();
            /*System.out.println(photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null).getHeight() + ", "+photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null).getWidth());
            for(PhotoSize ph: photos){
                System.out.println(ph.getHeight()+ ", "+ph.getWidth());
            }*/
            bot.sendImageFromUrl(f_id, bot.chatTo);
        } else if (update.hasMessage()
                && update.getMessage().hasDocument()
                && update.getMessage().getDocument().getMimeType().equals("video/mp4")
                && update.getMessage().getChat().getId().equals(bot.chatFrom)
                && update.getMessage().getCaption() == null) {
            bot.sendDocumentFromUrl(update.getMessage().getDocument().getFileId(), bot.chatTo);
        } else if ((update.hasMessage() // URL
                && update.getMessage().hasText()
                && this.checkURL(update.getMessage().getText()))) {
            try {
                bot.sendVideoFromUrl(update.getMessage().getText(), bot.chatTo);
            } catch (TelegramApiException ex) {
                Logger.getLogger(RequestDispatcher.class.getName()).log(Level.SEVERE, null, ex);
                bot.sendImageFromUrl(update.getMessage().getText(), bot.chatTo);
            }
        } else if (update.hasMessage()
                && update.getMessage().getVideo() != null
                && update.getMessage().getChat().getId().equals(bot.chatFrom)
                && update.getMessage().getCaption() == null) {
            try {
                bot.sendVideoFromUrl(update.getMessage().getVideo().getFileId(), bot.chatTo);
            } catch (TelegramApiException ex) {
                Logger.getLogger(RequestDispatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if ((update.hasMessage()
                && bot.chatAuth(update.getMessage().getChatId())
                && update.getMessage().hasText()
                && update.getMessage().getText().charAt(0) == '/')) {
            Scanner sc = new Scanner(update.getMessage().getText());
            String temp = sc.next();
            if (temp.equals("/best") || temp.equals("/worst")) {
                boolean isBest = temp.equals("/best");
                System.out.println(temp);
                int numero = 3;
                long desde = Integer.MIN_VALUE, hasta = Integer.MAX_VALUE;
                while (sc.hasNext()) {
                    String parse = sc.next();
                    if (parse.matches("\\d+")) {
                        numero = new Integer(parse);
                    } else if (parse.contains("desde:")) {
                        try {
                            parse = parse.substring(parse.indexOf(":") + 1);
                            DateFormat date = new SimpleDateFormat("dd-MM-yy", Locale.ITALY);
                            desde = (date.parse(parse).getTime() / 1000);
                        } catch (ParseException ex) {
                            Logger.getLogger(RequestDispatcher.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (parse.contains("hasta:")) {
                        try {
                            parse = parse.substring(parse.indexOf(":") + 1);
                            DateFormat date = new SimpleDateFormat("dd-MM-yy", Locale.ITALY);
                            hasta = (int) (date.parse(parse).getTime() / 1000);
                        } catch (ParseException ex) {
                            Logger.getLogger(RequestDispatcher.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                sc.close();
                long time = System.currentTimeMillis();
                try {
                    //bot.execute(new SendMessage(bot.chatReport, "Gathering info, wait me a while!"));
                    int[] best;

                    best = bot.db.getTopMessages(numero, desde, hasta, isBest);

                    bot.execute(new SendMessage(update.getMessage().getChatId(), "Top " + numero + " images"));
                    for (int i = 0; i < best.length; i++) {
                        try {
                            System.out.println(best[i]);
                            bot.execute(new SendMessage(update.getMessage().getChatId(), "Number " + (i + 1) + ":"));
                            bot.execute(new ForwardMessage(update.getMessage().getChatId(), bot.chatTo, best[i]));
                        } catch (TelegramApiException e) {
                            bot.execute(new SendMessage(update.getMessage().getChatId(), "Error"));
                        }
                    }
                } catch (TelegramApiException ex) {
                    Logger.getLogger(RequestDispatcher.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Best command took " + (System.currentTimeMillis() - time) + "ms to complete");
            } else if (temp.equalsIgnoreCase("/backup")) {
                bot.backup.run();
            }
        } else if (update.hasCallbackQuery()) {
            try {
                Votes voto;
                String emoji;
                String call_data = update.getCallbackQuery().getData();
                long message_id = update.getCallbackQuery().getMessage().getMessageId();
                long chat_id = update.getCallbackQuery().getMessage().getChatId();
                long user_id = update.getCallbackQuery().getFrom().getId();
                switch (call_data) {
                    case "+1":
                        voto = Votes.UP;
                        emoji = bot.upVote;
                        break;
                    case "-1":
                        voto = Votes.DOWN;
                        emoji = bot.downVote;
                        break;
                    case "neutral":
                        voto = Votes.NEUTRAL;
                        emoji = bot.neutral;
                        break;
                    case "heart":
                        voto = Votes.HEART;
                        emoji = bot.heart;
                        break;
                    case "drops":
                        voto = Votes.DROPS;
                        emoji = bot.drops;
                        break;
                    default:
                        voto = Votes.valueOf(call_data);
                        emoji = "";
                }

                // System.out.println("Update "+update.getUpdateId()+" before DB at "+System.currentTimeMillis());
                bot.db.saveVote(user_id, message_id, voto);
                // System.out.println("Update "+update.getUpdateId()+" vote saved at "+System.currentTimeMillis());
                bot.edit.addLista(message_id);
                //System.out.println(makeKeyboard.toString());
                String text;
                text = bot.reserva ? ("Your vote " + emoji + " has been saved. Votes won't be updated in photos yet") : ("Your vote " + emoji + " has been saved");
                bot.execute(new AnswerCallbackQuery().setCallbackQueryId(update.getCallbackQuery().getId()).setText(text));

            } catch (TelegramApiException ex) {
                Logger.getLogger(RequestDispatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private boolean checkURL(String url) {
        if (url.contains("instagram")) {
            return true;
        } else {
            try {
                if (RequestDispatcher.getContentType(url, 0).contains("image")) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException ex) {
                Logger.getLogger(RequestDispatcher.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
    }

    /**
     * Http HEAD Method to get URL content type
     *
     * @param urlString
     * @return content type
     * @throws IOException
     */
    private static String getContentType(String urlString, int deep) throws IOException {
        if (deep > 3) {
            return "";
        }
        deep++;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        if (isRedirect(connection.getResponseCode())) {
            String newUrl = connection.getHeaderField("Location"); // get redirect url from "location" header field
            return getContentType(newUrl, deep);
        }
        String contentType = connection.getContentType();
        return contentType;
    }

    /**
     * Check status code for redirects
     *
     * @param statusCode
     * @return true if matched redirect group
     */
    protected static boolean isRedirect(int statusCode) {
        if (statusCode != HttpURLConnection.HTTP_OK) {
            if (statusCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || statusCode == HttpURLConnection.HTTP_MOVED_PERM
                    || statusCode == HttpURLConnection.HTTP_SEE_OTHER) {
                return true;
            }
        }
        return false;
    }

    public RequestDispatcher(Update update, Bot bot) {
        this.update = update;
        this.bot = bot;
    }
}
