package org.user.sqlservice.annotation;

import org.springframework.context.annotation.Import;
import org.user.dao.SqlServiceContext;

@Import(value= SqlServiceContext.class)
public @interface EnableSqlService {
}
