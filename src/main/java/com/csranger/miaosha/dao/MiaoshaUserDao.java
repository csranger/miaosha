package com.csranger.miaosha.dao;

import com.csranger.miaosha.model.MiaoshaUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface MiaoshaUserDao {

    /**
     * 查询：根据手机号 id 查询 MiaoshaUser 对象
     * @param id
     * @return
     */
    @Select("select * from miaosha_user where id = #{id}")
    MiaoshaUser getById(@Param("id") Long id);




}
