package com.example.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface RowMap<T> {
    T rowMapping(ResultSet rs) throws SQLException;
}