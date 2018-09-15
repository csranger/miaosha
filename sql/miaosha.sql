# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: localhost (MySQL 5.7.18)
# Database: miaosha
# Generation Time: 2018-09-15 05:33:19 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table goods
# ------------------------------------------------------------

DROP TABLE IF EXISTS `goods`;

CREATE TABLE `goods` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '商品id',
  `goods_name` varchar(16) DEFAULT NULL COMMENT '商品名称',
  `goods_title` varchar(64) DEFAULT NULL COMMENT '商品的标题',
  `goods_img` varchar(64) DEFAULT NULL COMMENT '商品的图片',
  `goods_detail` longtext COMMENT '商品详情介绍',
  `goods_price` decimal(10,2) DEFAULT '0.00' COMMENT '商品单价',
  `goods_stock` int(11) DEFAULT '0' COMMENT '库存商品, -1 表示无限制',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商品表';

LOCK TABLES `goods` WRITE;
/*!40000 ALTER TABLE `goods` DISABLE KEYS */;

INSERT INTO `goods` (`id`, `goods_name`, `goods_title`, `goods_img`, `goods_detail`, `goods_price`, `goods_stock`)
VALUES
	(1,'iphonX','Apple iPhone X (A1865) 64GB 银色 移动联通电信4g手机','/img/iphonex.png','Apple iPhone X (A1865) 64GB 银色 移动联通电信4g手机',8765.00,10000),
	(2,'华为Mate9','华为 Mate 9 4GB+32GB版 月光银 双卡双待','/img/mate10.png','华为 Mate 9 4GB+32GB版 月光银 双卡双待',3212.00,-1);

/*!40000 ALTER TABLE `goods` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table miaosha_goods
# ------------------------------------------------------------

DROP TABLE IF EXISTS `miaosha_goods`;

CREATE TABLE `miaosha_goods` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '秒杀的商品表',
  `goods_id` bigint(20) DEFAULT NULL COMMENT '商品id',
  `miaosha_price` decimal(10,2) DEFAULT '0.00' COMMENT '秒杀价',
  `stock_count` int(11) DEFAULT NULL COMMENT '库存数量',
  `start_time` datetime DEFAULT NULL COMMENT '秒杀开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '秒杀结束时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀商品表';

LOCK TABLES `miaosha_goods` WRITE;
/*!40000 ALTER TABLE `miaosha_goods` DISABLE KEYS */;

INSERT INTO `miaosha_goods` (`id`, `goods_id`, `miaosha_price`, `stock_count`, `start_time`, `end_time`)
VALUES
	(1,1,0.01,48,'2018-08-09 14:00:00','2019-07-01 14:12:00'),
	(2,2,0.01,597,'2018-07-20 19:30:00','2019-07-20 19:30:00');

/*!40000 ALTER TABLE `miaosha_goods` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table miaosha_order
# ------------------------------------------------------------

DROP TABLE IF EXISTS `miaosha_order`;

CREATE TABLE `miaosha_order` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  `order_id` bigint(20) DEFAULT NULL COMMENT '订单id',
  `goods_id` bigint(20) DEFAULT NULL COMMENT '商品id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `u_uid_gid` (`user_id`,`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀订单表';

LOCK TABLES `miaosha_order` WRITE;
/*!40000 ALTER TABLE `miaosha_order` DISABLE KEYS */;

INSERT INTO `miaosha_order` (`id`, `user_id`, `order_id`, `goods_id`)
VALUES
	(1,12345678905,1,2),
	(2,12345678907,2,2),
	(3,12345678909,3,2),
	(4,12345678900,4,1),
	(5,12345678909,5,1);

/*!40000 ALTER TABLE `miaosha_order` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table miaosha_user
# ------------------------------------------------------------

DROP TABLE IF EXISTS `miaosha_user`;

CREATE TABLE `miaosha_user` (
  `id` bigint(20) unsigned NOT NULL COMMENT '用户id，手机号码',
  `nickname` varchar(255) DEFAULT NULL,
  `password` varchar(32) DEFAULT NULL COMMENT 'MD5(MD5(pass明文+固定salt)+salt）',
  `salt` varchar(10) DEFAULT NULL,
  `head` varchar(128) DEFAULT NULL COMMENT '头像，云存储id',
  `register_date` datetime DEFAULT NULL COMMENT '注册时间',
  `last_login_date` datetime DEFAULT NULL COMMENT '上次登陆时间',
  `login_count` int(11) DEFAULT '0' COMMENT '登陆次数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';



# Dump of table order_info
# ------------------------------------------------------------

DROP TABLE IF EXISTS `order_info`;

CREATE TABLE `order_info` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户id',
  `goods_id` bigint(20) DEFAULT NULL COMMENT '商品id',
  `delivery_addr_id` bigint(20) DEFAULT NULL COMMENT '收货地址',
  `goods_name` varchar(16) DEFAULT NULL COMMENT '冗余过来的商品商品名称',
  `goods_count` int(11) DEFAULT '0' COMMENT '购买数量',
  `goods_price` decimal(10,2) DEFAULT NULL COMMENT '购买价格',
  `order_channel` tinyint(4) DEFAULT NULL COMMENT '1-pc 2-android 3-ios',
  `status` tinyint(4) DEFAULT '0' COMMENT '0-新建未支付 1-已支付 2-已发货 3-已收获 4-已退款 5-已完成',
  `create_date` datetime DEFAULT NULL COMMENT '订单创建时间',
  `pay_day` datetime DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='订单表';

LOCK TABLES `order_info` WRITE;
/*!40000 ALTER TABLE `order_info` DISABLE KEYS */;

INSERT INTO `order_info` (`id`, `user_id`, `goods_id`, `delivery_addr_id`, `goods_name`, `goods_count`, `goods_price`, `order_channel`, `status`, `create_date`, `pay_day`)
VALUES
	(1,12345678905,2,0,'华为Mate9',1,0.01,1,0,'2018-08-07 09:37:22',NULL),
	(2,12345678907,2,0,'华为Mate9',1,0.01,1,0,'2018-08-07 09:38:49',NULL),
	(3,12345678909,2,0,'华为Mate9',1,0.01,1,0,'2018-08-07 20:46:18',NULL),
	(4,12345678900,1,0,'iphonX',1,0.01,1,0,'2018-09-06 21:33:16',NULL),
	(5,12345678909,1,0,'iphonX',1,0.01,1,0,'2018-09-07 21:48:55',NULL);

/*!40000 ALTER TABLE `order_info` ENABLE KEYS */;
UNLOCK TABLES;



/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
