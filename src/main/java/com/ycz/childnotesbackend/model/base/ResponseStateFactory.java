package com.ycz.childnotesbackend.model.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ResponseStateFactory {

    private static ResponseState OK = new ResponseState("000000", null);

    private static ResponseState FAIL = new ResponseState("000520", null);

    private static final List<Function<String, ResponseState>> TRANSLATORS = new ArrayList<>();

    private ResponseStateFactory() {
    }

    public static ResponseState getOk() {
        return OK;
    }

    public static ResponseState getFail() {
        return FAIL;
    }

    public static void setOK(ResponseState ok) {
        OK = ok;
    }

    public static void setFAIL(ResponseState fail) {
        FAIL = fail;
    }

    public static void register(Function<String, ResponseState> translator) {
        TRANSLATORS.add(translator);
    }

    public static Optional<ResponseState> getByCode(String code) {
        for (Function<String, ResponseState> translator : TRANSLATORS) {
            ResponseState responseState = translator.apply(code);
            if (responseState != null) {
                return Optional.of(responseState);
            }
        }
        return Optional.empty();
    }
}

