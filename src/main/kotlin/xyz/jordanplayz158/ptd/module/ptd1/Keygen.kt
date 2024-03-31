package xyz.jordanplayz158.ptd.module.ptd1

class Keygen {
    companion object {
        fun generateProfileId(currentSave: String, trainerId: Int): String {
            if (currentSave.length != 14 || trainerId < 333 || trainerId > 99999) {
                // This used to call a function which outputted a bunch of garbage for the profileId,
                // but now it gives you a helpful, clear message to let you know that the currentSave or trainerId was invalid
                return "invalidCurrentSaveOrTrainerId"
            }

            val currentSaveInt = currentSaveToInt(currentSave)

            // CurrentSave must be 14 characters long, trainerId must be between 333 and 99999, and currentSaveInt must not be 0.
            if (currentSaveInt == 0) {
                return "invalidCurrentSaveOrTrainerId"
            }

            val profileId = StringBuilder()
            val loc5 = ((trainerId * currentSaveInt) * 14).toString()

            for (char in loc5) {
                profileId.append(numToChar(char.digitToInt() + loc5[0].digitToInt()))
            }

            return profileId.toString()
        }

        private fun currentSaveToInt(currentSave: String): Int {
            var num = 0

            for (currentSaveChar in currentSave)
                num += charToInt(currentSaveChar.toString())

            return num
        }

        /**
         * This function converts a char to its ascii decimal value,
         * unless the char is a number.
         * ex. '0' = 0, '1' = 1, '2' = 2, 'a' = 1, 'b' = 2
         */
        private fun charToInt(char: String): Int {
            return try {
                char.toInt()
            } catch (e: NumberFormatException) {
                char.toCharArray()[0].code - 96
            }
        }

        /**
         * Converts a number into a letter in the alphabet
         * ex. 0 = a
         *     1 = b
         *     2 = c, etc.
         */
        private fun numToChar(num: Int): Char {
            return Char(num + 97)
        }
    }
}