package xyz.jordanplayz158.ptd.server.common

enum class ReasonsEnum(val result: Pair<String, String>, val reason: Pair<String, String>) {
    SUCCESS_LOGGED_IN(ResultsEnum.SUCCESS.id, "Reason" to "LoggedIn"),
    // Yes, it was misspelled in the SWF
    SUCCESS_GET_ACHIEVE(ResultsEnum.SUCCESS.id, "Reason" to "GetAchive"),
    SUCCESS_GET_PRIZE_1(ResultsEnum.SUCCESS.id, "Reason" to "getPrize1"),
    SUCCESS_GET_PRIZE_2(ResultsEnum.SUCCESS.id, "Reason" to "getPrize2"),
    SUCCESS_GET_PRIZE_3(ResultsEnum.SUCCESS.id, "Reason" to "getPrize3"),
    SUCCESS_GET_PRIZE_4(ResultsEnum.SUCCESS.id, "Reason" to "getPrize4"),
    SUCCESS_GET_PRIZE_5(ResultsEnum.SUCCESS.id, "Reason" to "getPrize5"),
    SUCCESS_GET_PRIZE_6(ResultsEnum.SUCCESS.id, "Reason" to "getPrize6"),
    SUCCESS_GET_PRIZE_7(ResultsEnum.SUCCESS.id, "Reason" to "getPrize7"),
    SUCCESS_GET_PRIZE_8(ResultsEnum.SUCCESS.id, "Reason" to "getPrize8"),
    SUCCESS_GET_PRIZE_9(ResultsEnum.SUCCESS.id, "Reason" to "getPrize9"),
    SUCCESS_GET_PRIZE_10(ResultsEnum.SUCCESS.id, "Reason" to "getPrize10"),
    SUCCESS_GET_PRIZE_11(ResultsEnum.SUCCESS.id, "Reason" to "getPrize11"),
    SUCCESS_GET_PRIZE_12(ResultsEnum.SUCCESS.id, "Reason" to "getPrize12"),
    SUCCESS_GET_PRIZE_13(ResultsEnum.SUCCESS.id, "Reason" to "getPrize13"),
    SUCCESS_GET_PRIZE_14(ResultsEnum.SUCCESS.id, "Reason" to "getPrize14"),
    SUCCESS_USE_CODE(ResultsEnum.SUCCESS.id, "Reason" to "UseCode"),
    SUCCESS_SAVED(ResultsEnum.SUCCESS.id, "Reason" to "saved"),

    // PTD2 Specific
    SUCCESS_LOADED_ACCOUNT(ResultsEnum.SUCCESS.id, "Reason" to "loadedAccount"),



    FAILURE_DATABASE_CONNECTION(ResultsEnum.FAILURE.id, "Reason" to "DatabaseConnection"),
    FAILURE_OLD_VERSION(ResultsEnum.FAILURE.id, "Reason" to "oldVersion"),
    FAILURE_NOT_FOUND(ResultsEnum.FAILURE.id, "Reason" to "NotFound"),
    FAILURE_TAKEN(ResultsEnum.FAILURE.id, "Reason" to "taken"),
    FAILURE_MAINTENANCE(ResultsEnum.FAILURE.id, "Reason" to "maintenance"),
    FAILURE_NO_REWARD(ResultsEnum.FAILURE.id, "Reason" to "NoReward"),
    FAILURE_CODE_USED(ResultsEnum.FAILURE.id, "Reason" to "CodeUsed"),
    FAILURE_CODE_NOT_RECOGNIZED(ResultsEnum.FAILURE.id, "Reason" to "CodeNotRecognized"),
    FAILURE_MISSING_CODE(ResultsEnum.FAILURE.id, "Reason" to "MissingCode"),
    FAILURE_PREVIEW_NOT_FOUND(ResultsEnum.FAILURE.id, "Reason" to "PreviewNotFound"),
    FAILURE_VALIDATION(ResultsEnum.FAILURE.id, "Reason" to "Validation"),

    // My personal non-swf-recognized error message
    FAILURE_NOT_ALL_PARAMETERS_SUPPLIED(ResultsEnum.FAILURE.id, "Reason" to "NotAllParametersSupplied");

    fun fullReason(): ArrayList<Pair<String, String>> {
        val list = ArrayList<Pair<String, String>>(2)
        list.add(result)
        list.add(reason)

        return list
    }
}