package com.csranger.miaosha.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.csranger.miaosha.model.MiaoshaUser;

/**
 * 生成了 1000 个用户以及对应的 token
 */
public class UserUtil {

    private static void createUser(int count) throws Exception{
        List<MiaoshaUser> users = new ArrayList<MiaoshaUser>(count);
        // 1. 生成用户
        for(int i=0;i<count;i++) {
            MiaoshaUser user = new MiaoshaUser();
            user.setId(13000000000L+i);
            user.setLoginCount(1);
            user.setNickname("user"+i);
            user.setRegisterDate(new Date());
            user.setSalt("1a2b3c4d");
            user.setPassword(MD5Util.encryPassword("123456", user.getSalt()));  // 两次加密后的值
            users.add(user);
        }
        System.out.println("create user");
		// 2. 将生成的用户插入数据库
//		Connection conn = DBUtil.getConn();
//		String sql = "insert into miaosha_user(login_count, nickname, register_date, salt, password, id)values(?,?,?,?,?,?)";
//		PreparedStatement pstmt = conn.prepareStatement(sql);
//		for(int i=0;i<users.size();i++) {
//			MiaoshaUser user = users.get(i);
//			pstmt.setInt(1, user.getLoginCount());
//			pstmt.setString(2, user.getNickname());
//			pstmt.setTimestamp(3, new Timestamp(user.getRegisterDate().getTime()));
//			pstmt.setString(4, user.getSalt());
//			pstmt.setString(5, user.getPassword());
//			pstmt.setLong(6, user.getId());
//			pstmt.addBatch();
//		}
//		pstmt.executeBatch();
//		pstmt.close();
//		conn.close();
//		System.out.println("insert to db");
        // 3. 使用生成的用户进行登录，服务端生成token并缓存到redis，获取这些token到tokens.txt文件
        String urlString = "http://localhost:8080/login/do_login";
        File file = new File("/Users/hailong/Documents/jmeter/tokens.txt");
        if(file.exists()) {
            file.delete();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        file.createNewFile();
        raf.seek(0);
        // 循环获取用户 MiaoshaUser
        for(int i=0;i<users.size();i++) {
            MiaoshaUser user = users.get(i);
            // url -> HttpURLConnection -> OutputStream
            // url -> HttpURLConnection -> InputStream => ByteArrayOutputStream -> response -> JSONObject -> token
            // HttpURLConnection   和页面建立连接
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            // OutputStream       利用 user 信息进行登录
            OutputStream out = connection.getOutputStream();
            String params = "mobile="+user.getId()+"&password="+MD5Util.inputPassToFormPass("123456");
            out.write(params.getBytes());
            out.flush();
            // InputStream        代表请求 /login/do_login 页面返回的数据
            // do_login 页面 controller 返回的是 Result<Boolean>，为了获取 token，需要对controller 进行稍许改造
            InputStream inputStream = connection.getInputStream();
            // InputStream => ByteArrayOutputStream
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int len = 0;
            while((len = inputStream.read(buff)) >= 0) {
                bout.write(buff, 0 ,len);
            }
            inputStream.close();
            bout.close();
            // ByteArrayOutputStream -> response -> JSONObject -> token
            String response = new String(bout.toByteArray());
            JSONObject jo = JSON.parseObject(response);
            String token = jo.getString("data");
            System.out.println("create token : " + user.getId());

            // 将获取的 token 写入到文件
            String row = user.getId()+","+token;
            raf.seek(raf.length());
            raf.write(row.getBytes());
            raf.write("\n".getBytes());   // mac 换行符\n windows 换行符 \r\n
            System.out.println("write to file : " + user.getId() + "," + token);
        }
        raf.close();

        System.out.println("over");
    }

    public static void main(String[] args)throws Exception {
        createUser(1000);
    }
}
