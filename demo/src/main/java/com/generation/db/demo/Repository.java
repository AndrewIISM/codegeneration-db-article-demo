package com.generation.db.demo;

import com.generation.db.annotation.GeneratingRepository;
import com.generation.db.annotation.Query;

import java.math.BigDecimal;

@GeneratingRepository
public interface Repository {

    @Query("query/queryExample.sql")
    String findEmployeeName(int minAge, BigDecimal minSalary);

}
