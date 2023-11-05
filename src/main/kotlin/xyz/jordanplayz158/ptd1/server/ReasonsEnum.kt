package xyz.jordanplayz158.ptd1.server

enum class ReasonsEnum(val id: String) {
    SUCCESS_LOGGED_IN("LoggedIn"),
    // Yes, it was misspelled in the SWF
    SUCCESS_GET_ACHIEVE("GetAchive"),
    SUCCESS_GET_PRIZE_1("getPrize1"),
    SUCCESS_GET_PRIZE_2("getPrize2"),
    SUCCESS_GET_PRIZE_3("getPrize3"),
    SUCCESS_GET_PRIZE_4("getPrize4"),
    SUCCESS_GET_PRIZE_5("getPrize5"),
    SUCCESS_GET_PRIZE_6("getPrize6"),
    SUCCESS_GET_PRIZE_7("getPrize7"),
    SUCCESS_GET_PRIZE_8("getPrize8"),
    SUCCESS_GET_PRIZE_9("getPrize9"),
    SUCCESS_GET_PRIZE_10("getPrize10"),
    SUCCESS_GET_PRIZE_11("getPrize11"),
    SUCCESS_GET_PRIZE_12("getPrize12"),
    SUCCESS_GET_PRIZE_13("getPrize13"),
    SUCCESS_GET_PRIZE_14("getPrize14"),
    SUCCESS_USE_CODE("UseCode"),
    SUCCESS_SAVED("saved"),

    FAILURE_DATABASE_CONNECTION("DatabaseConnection"),
    FAILURE_OLD_VERSION("oldVersion"),
    FAILURE_NOT_FOUND("NotFound"),
    FAILURE_TAKEN("taken"),
    FAILURE_MAINTENANCE("maintenance"),
    FAILURE_NO_REWARD("NoReward"),
    FAILURE_CODE_USED("CodeUsed"),
    FAILURE_CODE_NOT_RECOGNIZED("CodeNotRecognized"),
    FAILURE_MISSING_CODE("MissingCode"),
    FAILURE_PREVIEW_NOT_FOUND("PreviewNotFound"),
    FAILURE_VALIDATION("Validation")
}