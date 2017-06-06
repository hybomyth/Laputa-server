package com.laputa.server.core.protocol.exceptions;

import com.laputa.server.core.protocol.enums.Response;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/3/2015.
 */
public class ParseException extends BaseServerException {

    public ParseException(String message) {
        super(message, Response.ILLEGAL_COMMAND);
    }

}
