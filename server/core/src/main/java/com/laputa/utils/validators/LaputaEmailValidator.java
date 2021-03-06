package com.laputa.utils.validators;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 09.03.17.
 */
public class LaputaEmailValidator {

    public static boolean isNotValidEmail(String email) {
        return email == null || email.isEmpty() || email.length() > 255 ||
                email.contains("?") || !email.contains("@") ||
                !EmailValidator.getInstance().isValid(email);
    }

}
