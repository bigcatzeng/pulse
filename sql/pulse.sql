create database pulse default character set utf8mb4 collate utf8mb4_unicode_ci;

drop table if exists hosts;
create table hosts
(
	id int null,
	domain varchar(64) not null,
	status tinyint null comment '主机状态, 暂时未用, 可以用来标示主机类型,黑白名单,是否启用等等'
);

drop table if exists instances;
create table instances
(
	host_name varchar(64) not null primary key,
	last_access_time datetime null
);

drop table if exists message_params;
create table message_params
(
	id int not null primary key,
	message_id int not null,
	key_name varchar(64) null comment '参数名',
	key_value varchar(1024) null comment '参数值'
);

create index message_params_message_id_index on message_params (message_id);

drop table if exists message_states;
create table message_states
(
	id int not null primary key,
	retry tinyint not null ,
	status tinyint not null comment '任务状态 0等待发送 1监视中 2发送中 3结束',
	fire_time datetime not null comment '触发时间',
	actual_time datetime null comment '实际触发时间',
	finish_time datetime null comment '完成时间'
) comment '消息任务' ;

drop table if exists messages;
create table messages
(
	id int auto_increment primary key,
	protocol tinyint null comment '0-http, 1-https',
	host_id smallint(6) null comment 'http 请求主机id',
	content_path varchar(1024) null comment 'http content path'
);

drop table if exists message_results;
create table message_results
(
	id int not null primary key,
	message_id int not null,
	result varchar(1024) not null
);

drop table if exists message_logs;
create table message_logs
(
	id int not null primary key,
	message_id int not null,
	actual_time datetime not null comment '实际触发时间',
	finish_time datetime null comment '完成时间',
	status_code smallint null
);

drop table if exists scheduled_jobs;
create table scheduled_jobs
(
	id int auto_increment not null primary key,
	protocol tinyint null comment '0-http, 1-https',
	host_id int(6) null comment 'http 请求主机id',
	cron_expression varchar(32),
	content_path varchar(1024) null comment 'http content path'
);

drop table if exists scheduled_params;
create table scheduled_params
(
	id int not null primary key,
	scheduled_id int not null,
	key_name varchar(64) null comment '参数名',
	key_value varchar(1024) null comment '参数值'
);


drop table if exists scheduled_results;
create table scheduled_results
(
	id int not null primary key,
	scheduled_id int not null,
	result varchar(1024) not null
);

drop table if exists scheduled_logs;
create table scheduled_logs
(
	id int not null primary key,
	scheduled_id int not null,
	actual_time datetime not null comment '实际触发时间',
	finish_time datetime null comment '完成时间',
	status_code smallint null
);
