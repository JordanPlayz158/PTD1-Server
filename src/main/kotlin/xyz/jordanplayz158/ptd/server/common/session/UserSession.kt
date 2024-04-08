package xyz.jordanplayz158.ptd.server.common.session

import xyz.jordanplayz158.ptd.server.common.csrf.CSRFToken

data class UserSession(val csrf: CSRFToken)
