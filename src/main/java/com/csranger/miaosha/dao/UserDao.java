package com.csranger.miaosha.dao;

import com.csranger.miaosha.model.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface UserDao {

    /**
     * 根据id查询user表中的User对象
     * @param id
     * @return
     */
    @Select("select id, name from user where id = #{id}")
    User getUserById(@Param("id") int id);

    /**
     * 向user表中插入一条记录
     * @return
     */
    @Insert("insert into user(id, name) values (#{id}, #{name})")
    int insert(@Param("id") int id, @Param("name") String name);
}
