package software.consistent.model.type

import software.consistent.model.value.FALSE
import software.consistent.model.value.TRUE

public val Boolean: Type = Just(TRUE) or Just(FALSE)
