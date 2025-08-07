package com.pos.posApps.Util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

public class Generator {
    //Account = ACC
    //Customer = CST
    //Customer Level = CSL
    //Login Token = LTN
    //Product = PDT
    //Product Prices = PRS
    //Purchased Product = PPC
    //Purchasing = PCG
    //Supplier = SPP
    //Transaction Detail = TDL
    //Transaction = TSC
    //Client = CLN
    public static String generateId(String lastId){
        String code = lastId.substring(0,3);
        long id = Long.parseLong(lastId.substring(3));

        //+1 from last id
        long finalId = id + 1;

        return code + finalId;
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
}
