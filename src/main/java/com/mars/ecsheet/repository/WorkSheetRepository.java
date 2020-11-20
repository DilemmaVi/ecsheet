package com.mars.ecsheet.repository;

import com.mars.ecsheet.entity.WorkSheetEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Mars
 * @date 2020/10/29
 * @description
 */
@Repository
public interface WorkSheetRepository extends MongoRepository<WorkSheetEntity,String> {


    @Query(value = "{'wbId':?0,'deleteStatus':0}")
    List<WorkSheetEntity> findAllBywbId(String wbId);

    @Query(value = "{'data.index':?0,'wbId':?1}")
    WorkSheetEntity findByindexAndwbId(String index,String wbId);


    @Query(value = "{'data.status':?0,'wbId':?1}")
    WorkSheetEntity findBystatusAndwbId(int status,String wbId);


}
