package com.csranger.miaosha.dao;

import com.csranger.miaosha.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserDao {

    /**
     * 根据id查询user表中的User对象
     * @param id
     * @return
     */
    User getUserById(@Param("id") int id);

    /**
     * 向user表中插入一条记录
     * @return
     */
    int insert(@Param("id") int id, @Param("name") String name);
}
