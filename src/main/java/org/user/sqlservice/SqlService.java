package org.user.sqlservice;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.Map;

public interface SqlService {

    String getSql(String key) throws SqlRetrievalFailureException;
}
