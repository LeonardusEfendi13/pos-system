package com.pos.posApps.Util;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

public class Generator {
    //Account = ACC
    //Customer = CST
    //Customer Level = CSL
    //Login Token = LTN
    //Product = PDT
    //Product Prices = PRS
    //Purchased Product = PPC
    //Purchasing = PCG
    //Preorder = POR
    //Preorder Detail = POL
    //Supplier = SPP
    //Transaction Detail = TDL
    //Transaction = TSC
    //Client = CLN
    public static Long generateId(Long lastId){
        return lastId + 1;
    }

    public static String generateToken(){
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        //Use URL Encoder to make it more URL-safe
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); //Convert byte into base64
    }

    public static LocalDateTime getCurrentTimestamp(){
        return LocalDateTime.now(ZoneId.of("Asia/Jakarta"));
    }

    public static String generateNotaNumber(Long counter) {
        // Format tanggal saat ini ke YYYYMMDD
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String datePart = sdf.format(new Date());

        // Format nomor urut (3 digit minimal, lebih jika counter > 999)
        String counterPart = String.format("%03d", counter);

        // Gabungkan
        return datePart + counterPart;
    }
}
