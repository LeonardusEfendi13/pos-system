package com.pos.posApps.Util;

public class IdGenerator {

    public String generateId(String lastId){
        String code = lastId.substring(0,2);
        long id = Long.parseLong(lastId.substring(2));

        //+1 from last id
        long finalId = id + 1;

        return code + finalId;
    }
}
