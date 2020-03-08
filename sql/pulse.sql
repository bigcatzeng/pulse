create database pulse default character set utf8mb4 collate utf8mb4_unicode_ci;
-- we don't know how to generate schema pulse (class Schema) :(
-- grant all privileges on `pulse`.* to 'root'@'192.168.135.80' identified by 'VwUt4NxGw8p3Bj5S';

drop table if exists p_domains;
drop table if exists p_subsystems;
drop table if exists p_parameters;
drop table if exists p_instances;
drop table if exists p_tasks;
drop table if exists p_cron_tasks;
drop table if exists p_http_jobs;
drop table if exists p_http_job_responses;


create table p_domains
(
	id int auto_increment primary key,
	domain varchar(64) not null,
	reference int not null,
	status tinyint null comment '主机状态, 暂时未用, 可以用来标示主机类型,黑白名单,是否启用等等'
);

create table p_subsystems
(
	id int auto_increment primary key,
	service_name varchar(64) null comment '请求所属服务名称',
	ip varchar(32) null,
	secret_key varchar(128) not null comment '密钥',
	create_time datetime default CURRENT_TIMESTAMP null
);

create table p_parameters
(
	id int not null comment 'http_requests->id' primary key,
	headers varchar(1024) null,
	text_body varchar(1024) null comment 'HTTP请求参数正文'
);

create table p_instances
(
	host_name varchar(64) not null primary key,
	last_access_time datetime null
) engine=MEMORY;

create table p_tasks
(
	id int auto_increment primary key,
	job_type tinyint not null comment '任务动作类型 0-http 1-MQ ...',
	job_id int null comment '任务动作id',
	state tinyint null comment '0-等待加载 1-等待执行 2-执行中 3-结束 4-执行错误 5-结果未知',
	plan_time datetime default CURRENT_TIMESTAMP null
);

create table p_cron_tasks
(
	id int auto_increment primary key,
	subsystem int null comment '定时任务所属子系统, 用于区别请求来源',
	title varchar(64) null comment '任务名称',
	job_type tinyint not null comment '任务动作类型 0-http 1-MQ ...',
	job_id int null comment '任务动作id',
	state tinyint null comment '0-停止状态 1-激活状态',
	cront_expression varchar(128) not null comment 'cron 表达式',
	create_time datetime default CURRENT_TIMESTAMP not null
);

create table p_http_jobs
(
	id int auto_increment primary key,
	subsystem int null comment '定时任务所属子系统, 用于区别请求来源',
	title varchar(64) null comment '任务名称',
	protocol tinyint default '0' null comment '0-http 1-https',
	host_id int null,
	method tinyint null comment '0-POST 1-PUT 2-GET 3-DELETE',
	create_time datetime default CURRENT_TIMESTAMP null,
	url varchar(1024) null
);

create table p_http_job_responses
(
	id int primary key not null comment 'p_http_jobs->id',
	create_time datetime default CURRENT_TIMESTAMP not null,
	content varchar(1024) null
);