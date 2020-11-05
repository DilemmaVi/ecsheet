package com.mars.ecsheet.repository;

import com.mars.ecsheet.entity.WorkBookEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Mars
 * @date 2020/10/29
 * @description
 */
@Repository
public interface WorkBookRepository extends MongoRepository<WorkBookEntity,String> {
}
