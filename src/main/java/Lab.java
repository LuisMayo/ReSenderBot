
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author luism
 */
public class Lab {

    public static void main(String[] args) {
        final DateTimeFormatter formatter
                = DateTimeFormatter.ofPattern("dd-mm-yy");
        formatter.parse("27-31-13");
        //formatter.ge
        final long unixTime = 1372339860;
        final String formattedDtm = Instant.ofEpochSecond(unixTime)
                .atZone(ZoneId.of("GMT+1"))
                .format(formatter);
        
        try {
            DateFormat date = new SimpleDateFormat("dd-MM-yy");
            System.out.println(date.parse("11-05-18").getTime()/1000);
        } catch (ParseException ex) {
            Logger.getLogger(Lab.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println(formattedDtm);
    }
}
