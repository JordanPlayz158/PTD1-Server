package xyz.jordanplayz158.ptd1.server.session

import xyz.jordanplayz158.ptd1.server.csrf.CSRFToken

data class UserSession(val csrf: CSRFToken)
