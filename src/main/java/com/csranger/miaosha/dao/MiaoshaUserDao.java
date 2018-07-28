package com.csranger.miaosha.dao;

import com.csranger.miaosha.model.MiaoshaUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface MiaoshaUserDao {

    /**
     * 查询：根据手机号 id 查询 MiaoshaUser 对象
     */
    @Select("select * from miaosha_user where id = #{id}")
    MiaoshaUser getById(@Param("id") Long id);


    /**
     * 更新：根据 id 只更新用户密码，不更新的属性不要传
     */
    @Update("update miaosha_user set password = #{password} where id = #{id}")
    void updatePassword(MiaoshaUser toBeUpdate);


}
