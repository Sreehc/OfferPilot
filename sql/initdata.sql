-- ByteCoach 测试数据
-- 在 init.sql 执行后运行此文件，插入初始测试数据。

USE bytecoach;
SET NAMES utf8mb4;

-- ============================================================
-- 默认账号（密码: 123456）
-- ============================================================
DELETE FROM user WHERE id BETWEEN 6 AND 12;

INSERT INTO user (id, username, password, nickname, email, role, status, source, remark, last_login_time, create_time, update_time)
VALUES
    (1, 'demo', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', 'ByteCoach', 'demo@bytecoach.local', 'ADMIN', 1, 'seed', '默认后台测试管理员', NOW() - INTERVAL 20 MINUTE, NOW() - INTERVAL 20 DAY, NOW()),
    (2, 'learner', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '林简', 'linjian@bytecoach.local', 'USER', 1, 'seed', '主学习链路测试账号', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 15 DAY, NOW()),
    (3, 'mentor', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '周顾问', 'mentor@bytecoach.local', 'USER', 1, 'seed', '社区高活跃回答用户', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 12 DAY, NOW()),
    (4, 'teammate', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '陈同学', 'teammate@bytecoach.local', 'USER', 1, 'seed', '社区提问用户', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 9 DAY, NOW()),
    (5, 'reviewer', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '许面试官', 'reviewer@bytecoach.local', 'USER', 1, 'seed', '面试与内容审核测试用户', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 8 DAY, NOW()),
    (6, 'ops_admin', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '运维台', 'ops@bytecoach.local', 'ADMIN', 1, 'seed', '第二管理员账号', NOW() - INTERVAL 35 MINUTE, NOW() - INTERVAL 7 DAY, NOW()),
    (7, 'campus_newbie', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '校招练习生', 'campus@bytecoach.local', 'USER', 1, 'seed', '新注册轻度活跃用户', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 2 DAY, NOW()),
    (8, 'silent_reader', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '潜水用户', 'reader@bytecoach.local', 'USER', 1, 'seed', '只浏览不发言的用户', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 6 DAY, NOW()),
    (9, 'blocked_case', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '封禁样例', 'blocked@bytecoach.local', 'USER', 0, 'seed', '用于后台封禁状态展示', NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 11 DAY, NOW()),
    (10, 'pending_author', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '待审作者', 'pending@bytecoach.local', 'USER', 1, 'seed', '用于内容审核列表展示', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 DAY, NOW()),
    (11, 'answer_drafter', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '回答草稿人', 'drafter@bytecoach.local', 'USER', 1, 'seed', '用于不同回答状态展示', NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 4 DAY, NOW()),
    (12, 'growth_user', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', '成长样例', 'growth@bytecoach.local', 'USER', 1, 'seed', '用于排行榜与社区积分展示', NOW() - INTERVAL 50 MINUTE, NOW() - INTERVAL 3 DAY, NOW())
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    password = VALUES(password),
    nickname = VALUES(nickname),
    role = VALUES(role),
    status = VALUES(status),
    source = VALUES(source),
    remark = VALUES(remark),
    last_login_time = VALUES(last_login_time),
    update_time = NOW();

-- ============================================================
-- 分类数据
-- ============================================================
INSERT INTO category (id, name, type, sort_order, status, create_time, update_time)
VALUES
    (1, 'Spring', 'question', 1, 1, NOW(), NOW()),
    (2, 'JVM', 'knowledge', 2, 1, NOW(), NOW()),
    (3, 'MySQL', 'interview', 3, 1, NOW(), NOW()),
    (4, 'JVM', 'question', 4, 1, NOW(), NOW()),
    (5, 'MySQL', 'question', 5, 1, NOW(), NOW()),
    (6, 'Spring', 'interview', 6, 1, NOW(), NOW()),
    (7, 'Redis', 'question', 7, 1, NOW(), NOW()),
    (8, 'Redis', 'interview', 8, 1, NOW(), NOW()),
    (9, '并发', 'question', 9, 1, NOW(), NOW()),
    (10, '并发', 'interview', 10, 1, NOW(), NOW()),
    (11, '微服务', 'question', 11, 1, NOW(), NOW()),
    (12, '微服务', 'interview', 12, 1, NOW(), NOW()),
    (13, 'Spring', 'knowledge', 13, 1, NOW(), NOW()),
    (14, 'MySQL', 'knowledge', 14, 1, NOW(), NOW()),
    (15, 'Redis', 'knowledge', 15, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    update_time = NOW();

-- ============================================================
-- 测试数据清理（固定测试 ID）
-- ============================================================
DELETE FROM community_vote WHERE id BETWEEN 9001 AND 9099;
DELETE FROM community_answer WHERE id BETWEEN 8001 AND 8099;
DELETE FROM community_question WHERE id BETWEEN 7001 AND 7099;
DELETE FROM notification WHERE id BETWEEN 6501 AND 6599;
DELETE FROM user_stats WHERE id BETWEEN 6001 AND 6099;
DELETE FROM review_log WHERE id BETWEEN 5601 AND 5699;
DELETE FROM wrong_question WHERE id BETWEEN 5501 AND 5599;
DELETE FROM voice_record WHERE id BETWEEN 5401 AND 5499;
DELETE FROM interview_record WHERE id BETWEEN 5201 AND 5399;
DELETE FROM interview_session WHERE id BETWEEN 5101 AND 5199;
DELETE FROM chat_message WHERE id BETWEEN 4301 AND 4499;
DELETE FROM chat_session WHERE id BETWEEN 4201 AND 4299;
DELETE FROM knowledge_chunk WHERE id BETWEEN 4101 AND 4299;
DELETE FROM knowledge_doc WHERE id BETWEEN 4001 AND 4099;
DELETE FROM login_log WHERE id BETWEEN 3201 AND 3299;
DELETE FROM login_device WHERE id BETWEEN 3101 AND 3199;
DELETE FROM question WHERE id BETWEEN 1001 AND 1099;

-- ============================================================
-- 面试题库
-- ============================================================
INSERT INTO question (
    id, title, category_id, type, difficulty, frequency, tags, standard_answer, score_standard, source, create_time, update_time
)
VALUES
    (1001, '解释 Spring AOP 的动态代理实现', 1, 'short_answer', 'medium', 8, 'Spring,AOP,代理', 'Spring AOP 常见实现方式是 JDK 动态代理和 CGLIB。接口代理优先使用 JDK 动态代理，目标类没有接口时通常使用 CGLIB。', '至少提到 JDK 动态代理、CGLIB 以及两者适用边界。', 'seed', NOW(), NOW()),
    (1002, 'JVM CMS 和 G1 的主要差异是什么？', 4, 'short_answer', 'hard', 7, 'JVM,GC,CMS,G1', 'CMS 以最短停顿为目标但会产生内存碎片，G1 面向服务端大堆内存，按 Region 回收并兼顾吞吐与停顿。', '至少说明目标、内存管理方式和典型问题。', 'seed', NOW(), NOW()),
    (1003, 'MySQL 为什么会出现索引失效？', 5, 'short_answer', 'medium', 6, 'MySQL,索引,执行计划', '常见原因包括最左前缀被破坏、列上使用函数或表达式、隐式类型转换、范围查询后继续使用联合索引列等。', '至少给出 3 个典型场景。', 'seed', NOW(), NOW()),
    (1004, 'Spring Bean 的生命周期是怎样的？', 1, 'short_answer', 'medium', 7, 'Spring,Bean,生命周期', 'Spring Bean 的生命周期包括：实例化、属性注入、Aware 回调、BeanPostProcessor 处理、初始化回调、使用以及销毁回调。', '至少提到实例化、属性注入、初始化回调、销毁回调四个阶段。', 'seed', NOW(), NOW()),
    (1005, '什么是 Spring 循环依赖？如何解决？', 1, 'short_answer', 'hard', 6, 'Spring,循环依赖,三级缓存', '循环依赖指两个或多个 Bean 相互持有引用。Spring 通过三级缓存解决 Setter 注入的循环依赖，构造器注入无法自动解决。', '至少提到三级缓存机制和构造器注入的限制。', 'seed', NOW(), NOW()),
    (1006, 'JVM 内存模型中堆和方法区的区别？', 4, 'short_answer', 'easy', 5, 'JVM,内存模型,堆,方法区', '堆是线程共享的对象存储区，也是 GC 主要区域；方法区主要存放类信息、常量和静态变量，JDK 8 后由元空间实现。', '至少说明线程共享特性和存储内容差异。', 'seed', NOW(), NOW()),
    (1007, 'MySQL 事务隔离级别有哪些？InnoDB 默认是哪个？', 5, 'short_answer', 'medium', 8, 'MySQL,事务,隔离级别', '四个隔离级别分别是读未提交、读已提交、可重复读和串行化。InnoDB 默认是可重复读，并结合 MVCC 与锁机制保证一致性。', '至少列出四个级别并说明 InnoDB 默认级别。', 'seed', NOW(), NOW()),
    (1008, '解释 TCP 三次握手和四次挥手', 3, 'short_answer', 'medium', 6, 'TCP,网络,三次握手', '三次握手用于建立可靠连接，四次挥手用于双方分别关闭发送通道，因为 TCP 是全双工协议。', '至少说明握手目的和挥手为何需要四次。', 'seed', NOW(), NOW()),
    (1009, 'Redis 为什么适合做缓存？需要关注哪些风险？', 7, 'short_answer', 'easy', 7, 'Redis,缓存,雪崩,击穿', 'Redis 读写快、支持多种数据结构，适合高频读场景。使用时需要关注缓存穿透、击穿、雪崩以及一致性问题。', '至少说明 Redis 的性能优势和两个常见风险。', 'seed', NOW(), NOW()),
    (1010, '说说线程池的核心参数以及拒绝策略', 9, 'short_answer', 'medium', 8, '并发,线程池,拒绝策略', '线程池需要关注 corePoolSize、maximumPoolSize、keepAliveTime、workQueue 和 ThreadFactory。拒绝策略包括 Abort、CallerRuns、Discard 和 DiscardOldest。', '至少说明核心参数含义，并列出常见拒绝策略。', 'seed', NOW(), NOW()),
    (1011, '微服务中为什么需要服务注册与发现？', 11, 'short_answer', 'medium', 6, '微服务,注册中心,服务发现', '服务注册与发现用于动态维护服务实例地址，降低服务间硬编码耦合，并支持扩缩容和故障摘除。', '至少说明动态寻址、解耦和扩缩容价值。', 'seed', NOW(), NOW()),
    (1012, 'Redis 持久化 RDB 和 AOF 的区别是什么？', 7, 'short_answer', 'medium', 7, 'Redis,RDB,AOF', 'RDB 适合做快照备份，恢复快但可能丢失最后一次快照后的数据；AOF 记录写命令，数据更完整，但文件更大、恢复更慢。', '至少比较数据完整性、恢复速度和使用场景。', 'seed', NOW(), NOW());

-- ============================================================
-- 知识库文档与分片
-- ============================================================
INSERT INTO knowledge_doc (id, title, category_id, user_id, source_type, file_url, summary, status, create_time, update_time)
VALUES
    (4001, 'Spring Bean 生命周期笔记', 13, 1, 'seed', 'seed://knowledge/spring-bean-life.md', '梳理 Bean 的创建、初始化、依赖注入和销毁过程，适合复习 Spring 基础题。', 'indexed', NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 2 DAY),
    (4002, 'MySQL 索引与事务复习', 14, 1, 'seed', 'seed://knowledge/mysql-index-transaction.md', '覆盖最左前缀、回表、锁、MVCC 和事务隔离级别等高频题。', 'indexed', NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 1 DAY),
    (4003, 'Redis 缓存治理清单', 15, 2, 'user_upload', 'upload://user/2/redis-cache-checklist.md', '从缓存穿透、击穿、雪崩到双写一致性，整理上线前需要检查的关键点。', 'indexed', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 1 DAY),
    (4004, 'JVM 调优面试速记', 2, 2, 'user_upload', 'upload://user/2/jvm-tuning-notes.pdf', '总结内存结构、常见 GC、线上排查思路和调优步骤。', 'parsed', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 HOUR)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    category_id = VALUES(category_id),
    user_id = VALUES(user_id),
    source_type = VALUES(source_type),
    file_url = VALUES(file_url),
    summary = VALUES(summary),
    status = VALUES(status),
    update_time = VALUES(update_time);

INSERT INTO knowledge_chunk (id, doc_id, chunk_index, content, token_count, vector_id, create_time, update_time)
VALUES
    (4101, 4001, 1, 'Bean 生命周期可以分为实例化、属性注入、初始化和销毁四个大阶段。初始化前后会经过 BeanPostProcessor 处理。', 42, NULL, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 2 DAY),
    (4102, 4001, 2, '如果 Bean 实现了 Aware 接口，Spring 会在初始化前注入 BeanName、BeanFactory、ApplicationContext 等上下文对象。', 39, NULL, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 2 DAY),
    (4103, 4002, 1, 'MySQL 索引失效通常与最左前缀被破坏、索引列使用函数、隐式类型转换以及范围条件后的列无法继续命中有关。', 48, NULL, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 1 DAY),
    (4104, 4002, 2, 'InnoDB 默认隔离级别是可重复读，通过 MVCC 和 Next-Key Lock 降低幻读影响。', 30, NULL, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 1 DAY),
    (4105, 4003, 1, '缓存穿透可以通过布隆过滤器或缓存空值缓解，缓存击穿需要热点保护，缓存雪崩需要过期时间打散与降级策略。', 45, NULL, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 1 DAY),
    (4106, 4003, 2, '当缓存与数据库双写时，需要定义失效策略、重试策略以及最终一致性的兜底方案。', 32, NULL, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 1 DAY),
    (4107, 4004, 1, 'GC 调优通常从确认内存分布、定位对象分配热点、观察 Full GC 频率和停顿时间开始。', 34, NULL, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 HOUR)
ON DUPLICATE KEY UPDATE
    doc_id = VALUES(doc_id),
    chunk_index = VALUES(chunk_index),
    content = VALUES(content),
    token_count = VALUES(token_count),
    vector_id = VALUES(vector_id),
    update_time = VALUES(update_time);

-- ============================================================
-- 聊天会话与消息
-- ============================================================
INSERT INTO chat_session (id, user_id, title, mode, last_message_time, create_time, update_time)
VALUES
    (4201, 1, 'Spring 循环依赖怎么答', 'chat', NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY),
    (4202, 1, 'MySQL 索引复习', 'rag', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
    (4203, 2, 'Redis 缓存问题排查', 'rag', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    mode = VALUES(mode),
    last_message_time = VALUES(last_message_time),
    update_time = VALUES(update_time);

INSERT INTO chat_message (id, session_id, user_id, role, message_type, content, reference_json, create_time)
VALUES
    (4301, 4201, 1, 'user', 'text', 'Spring 循环依赖面试里应该怎么答，重点说什么？', NULL, NOW() - INTERVAL 5 DAY),
    (4302, 4201, 1, 'assistant', 'text', '可以先定义什么是循环依赖，再说明 Spring 主要解决的是单例 Bean 的 Setter 注入循环依赖，核心机制是三级缓存，最后补一句构造器注入无法自动解决。', NULL, NOW() - INTERVAL 5 DAY + INTERVAL 2 MINUTE),
    (4303, 4202, 1, 'user', 'text', 'MySQL 索引失效常见场景有哪些？', NULL, NOW() - INTERVAL 2 DAY),
    (4304, 4202, 1, 'assistant', 'reference', '常见场景包括联合索引未命中最左前缀、对索引列使用函数、发生隐式类型转换，以及范围查询后继续依赖后续索引列。', '[{"docId":4002,"docTitle":"MySQL 索引与事务复习","chunkId":4103,"snippet":"MySQL 索引失效通常与最左前缀被破坏、索引列使用函数、隐式类型转换以及范围条件后的列无法继续命中有关。","score":0.94},{"docId":4002,"docTitle":"MySQL 索引与事务复习","chunkId":4104,"snippet":"InnoDB 默认隔离级别是可重复读，通过 MVCC 和 Next-Key Lock 降低幻读影响。","score":0.81}]', NOW() - INTERVAL 2 DAY + INTERVAL 1 MINUTE),
    (4305, 4203, 2, 'user', 'text', '线上 Redis 命中率下降时先看什么？', NULL, NOW() - INTERVAL 1 DAY),
    (4306, 4203, 2, 'assistant', 'reference', '建议先看热点 key 分布、过期策略、回源耗时以及近期发布是否改动了缓存 key 规则，再判断是否存在击穿或雪崩。', '[{"docId":4003,"docTitle":"Redis 缓存治理清单","chunkId":4105,"snippet":"缓存穿透可以通过布隆过滤器或缓存空值缓解，缓存击穿需要热点保护，缓存雪崩需要过期时间打散与降级策略。","score":0.92}]', NOW() - INTERVAL 1 DAY + INTERVAL 3 MINUTE)
ON DUPLICATE KEY UPDATE
    session_id = VALUES(session_id),
    user_id = VALUES(user_id),
    role = VALUES(role),
    message_type = VALUES(message_type),
    content = VALUES(content),
    reference_json = VALUES(reference_json),
    create_time = VALUES(create_time);

-- ============================================================
-- 面试会话、记录与语音记录
-- ============================================================
INSERT INTO interview_session (id, user_id, direction, status, total_score, question_count, current_index, start_time, end_time, mode, create_time, update_time)
VALUES
    (5101, 1, 'Spring', 'finished', 84.00, 3, 3, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY + INTERVAL 18 MINUTE, 'text', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY + INTERVAL 18 MINUTE),
    (5102, 1, 'MySQL', 'finished', 71.00, 3, 3, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 21 MINUTE, 'voice', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 21 MINUTE),
    (5103, 1, '并发', 'finished', 78.00, 2, 2, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 15 MINUTE, 'text', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 15 MINUTE),
    (5104, 2, 'Redis', 'in_progress', NULL, 2, 2, NOW() - INTERVAL 2 HOUR, NULL, 'text', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 30 MINUTE)
ON DUPLICATE KEY UPDATE
    direction = VALUES(direction),
    status = VALUES(status),
    total_score = VALUES(total_score),
    question_count = VALUES(question_count),
    current_index = VALUES(current_index),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    mode = VALUES(mode),
    update_time = VALUES(update_time);

INSERT INTO interview_record (id, session_id, user_id, question_id, user_answer, score, comment, follow_up, is_wrong, create_time, update_time)
VALUES
    (5201, 5101, 1, 1001, '我会先说 AOP 常用 JDK 动态代理和 CGLIB，再补充接口优先 JDK，目标类无接口时会走 CGLIB。', 88.00, '回答完整，能够说明两种代理方案和适用场景。', '如果目标类和接口都存在，Spring 默认优先哪种代理方式？', 0, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY),
    (5202, 5101, 1, 1004, 'Bean 会先实例化，再注入属性，之后执行初始化方法，最后在容器关闭时销毁。', 82.00, '主流程正确，但可以再补充 BeanPostProcessor 和 Aware 回调。', 'BeanPostProcessor 会在生命周期中的哪两个阶段插入？', 0, NOW() - INTERVAL 6 DAY + INTERVAL 6 MINUTE, NOW() - INTERVAL 6 DAY + INTERVAL 6 MINUTE),
    (5203, 5101, 1, 1005, '循环依赖就是两个 Bean 相互引用。Spring 通过三级缓存解决，但构造器注入不行。', 82.00, '核心点提到了，若能说出三级缓存名称会更扎实。', '为什么三级缓存比两级缓存更合适？', 0, NOW() - INTERVAL 6 DAY + INTERVAL 12 MINUTE, NOW() - INTERVAL 6 DAY + INTERVAL 12 MINUTE),
    (5204, 5102, 1, 1003, '索引失效通常是用了函数，或者没走最左前缀。', 68.00, '只说到了两种情况，缺少隐式类型转换、范围查询后的联合索引失效等典型场景。', '如何用 explain 快速判断索引是否命中？', 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
    (5205, 5102, 1, 1007, '事务隔离级别有四个，默认是可重复读。', 74.00, '回答正确但较简略，最好补上 MVCC 和幻读处理。', 'InnoDB 是如何降低幻读影响的？', 0, NOW() - INTERVAL 3 DAY + INTERVAL 7 MINUTE, NOW() - INTERVAL 3 DAY + INTERVAL 7 MINUTE),
    (5206, 5102, 1, 1008, '三次握手是建立连接，四次挥手是断开连接，因为双方都要单独关闭。', 71.00, '能说明基础流程，但如果能把每一步报文说清楚会更好。', '为什么挥手阶段客户端进入 TIME_WAIT？', 0, NOW() - INTERVAL 3 DAY + INTERVAL 14 MINUTE, NOW() - INTERVAL 3 DAY + INTERVAL 14 MINUTE),
    (5207, 5103, 1, 1010, '线程池要看核心线程数、最大线程数、队列和拒绝策略。', 76.00, '基本点覆盖了，建议再补充 keepAliveTime 和不同队列对线程增长的影响。', '有界队列和无界队列分别适合什么场景？', 0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
    (5208, 5103, 1, 1011, '服务注册与发现是为了让服务之间不用写死地址，方便扩缩容和故障切换。', 80.00, '表达清晰，已经覆盖了主要价值。', '注册中心除了存地址，还常负责哪些能力？', 0, NOW() - INTERVAL 1 DAY + INTERVAL 8 MINUTE, NOW() - INTERVAL 1 DAY + INTERVAL 8 MINUTE),
    (5209, 5104, 2, 1009, 'Redis 适合做缓存，因为读写快。', NULL, NULL, NULL, 0, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR),
    (5210, 5104, 2, 1012, NULL, NULL, NULL, NULL, 0, NOW() - INTERVAL 90 MINUTE, NOW() - INTERVAL 90 MINUTE)
ON DUPLICATE KEY UPDATE
    session_id = VALUES(session_id),
    user_id = VALUES(user_id),
    question_id = VALUES(question_id),
    user_answer = VALUES(user_answer),
    score = VALUES(score),
    comment = VALUES(comment),
    follow_up = VALUES(follow_up),
    is_wrong = VALUES(is_wrong),
    update_time = VALUES(update_time);

INSERT INTO voice_record (id, session_id, record_id, user_id, audio_url, transcript, transcript_confidence, transcript_time_ms, create_time, update_time)
VALUES
    (5401, 5102, 5204, 1, '/uploads/audio/mock-mysql-1.webm', '索引失效通常出现在没走最左前缀、索引列上用了函数，或者发生隐式类型转换的时候。', 0.962, 1240, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
    (5402, 5102, 5205, 1, '/uploads/audio/mock-mysql-2.webm', '事务隔离级别有四个，InnoDB 默认是可重复读。', 0.974, 880, NOW() - INTERVAL 3 DAY + INTERVAL 7 MINUTE, NOW() - INTERVAL 3 DAY + INTERVAL 7 MINUTE)
ON DUPLICATE KEY UPDATE
    session_id = VALUES(session_id),
    record_id = VALUES(record_id),
    user_id = VALUES(user_id),
    audio_url = VALUES(audio_url),
    transcript = VALUES(transcript),
    transcript_confidence = VALUES(transcript_confidence),
    transcript_time_ms = VALUES(transcript_time_ms),
    update_time = VALUES(update_time);

-- ============================================================
-- 错题与复习记录
-- ============================================================
INSERT INTO wrong_question (id, user_id, question_id, source_type, user_answer, standard_answer, error_reason, mastery_level, review_count, last_review_time, ease_factor, interval_days, next_review_date, streak, create_time, update_time)
VALUES
    (5501, 1, 1003, 'interview', '索引失效一般是函数或没走最左前缀。', '常见原因包括最左前缀被破坏、列上使用函数或表达式、隐式类型转换、范围查询后继续使用联合索引列等。', '回答覆盖面不够，缺少隐式转换与范围查询后的索引问题。', 'reviewing', 3, NOW() - INTERVAL 1 DAY, 2.20, 1, CURDATE() - INTERVAL 1 DAY, 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY),
    (5502, 1, 1007, 'interview', '事务隔离级别有四个，默认是可重复读。', '四个隔离级别分别是读未提交、读已提交、可重复读和串行化。InnoDB 默认是可重复读，并结合 MVCC 与锁机制保证一致性。', '答案正确但不完整，没有解释 MVCC 和锁。', 'reviewing', 2, NOW() - INTERVAL 2 DAY, 2.35, 3, CURDATE(), 2, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),
    (5503, 1, 1010, 'interview', '线程池主要看核心线程数和队列。', '线程池需要关注 corePoolSize、maximumPoolSize、keepAliveTime、workQueue 和 ThreadFactory。拒绝策略包括 Abort、CallerRuns、Discard 和 DiscardOldest。', '遗漏了 keepAliveTime、拒绝策略和不同队列的影响。', 'not_started', 0, NULL, 2.50, 0, CURDATE(), 0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
    (5504, 2, 1009, 'interview', 'Redis 读写快。', 'Redis 读写快、支持多种数据结构，适合高频读场景。使用时需要关注缓存穿透、击穿、雪崩以及一致性问题。', '只说了性能，没有提到缓存风险与治理方案。', 'not_started', 0, NULL, 2.50, 0, CURDATE() + INTERVAL 1 DAY, 0, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    question_id = VALUES(question_id),
    source_type = VALUES(source_type),
    user_answer = VALUES(user_answer),
    standard_answer = VALUES(standard_answer),
    error_reason = VALUES(error_reason),
    mastery_level = VALUES(mastery_level),
    review_count = VALUES(review_count),
    last_review_time = VALUES(last_review_time),
    ease_factor = VALUES(ease_factor),
    interval_days = VALUES(interval_days),
    next_review_date = VALUES(next_review_date),
    streak = VALUES(streak),
    update_time = VALUES(update_time);

INSERT INTO review_log (id, user_id, wrong_question_id, rating, response_time_ms, ease_factor_before, interval_before, ease_factor_after, interval_after, create_time)
VALUES
    (5601, 1, 5501, 2, 42000, 2.50, 0, 2.35, 1, NOW() - INTERVAL 2 DAY),
    (5602, 1, 5501, 3, 31000, 2.35, 1, 2.20, 1, NOW() - INTERVAL 1 DAY),
    (5603, 1, 5502, 3, 28000, 2.50, 0, 2.36, 1, NOW() - INTERVAL 3 DAY),
    (5604, 1, 5502, 4, 25000, 2.36, 1, 2.46, 6, NOW() - INTERVAL 2 DAY),
    (5605, 1, 5501, 1, 39000, 2.20, 1, 2.00, 1, NOW() - INTERVAL 6 HOUR),
    (5606, 2, 5504, 2, 47000, 2.50, 0, 2.35, 1, NOW() - INTERVAL 30 MINUTE)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    wrong_question_id = VALUES(wrong_question_id),
    rating = VALUES(rating),
    response_time_ms = VALUES(response_time_ms),
    ease_factor_before = VALUES(ease_factor_before),
    interval_before = VALUES(interval_before),
    ease_factor_after = VALUES(ease_factor_after),
    interval_after = VALUES(interval_after),
    create_time = VALUES(create_time);

-- ============================================================
-- 社区数据
-- ============================================================
INSERT INTO community_question (id, user_id, title, content, category_id, status, upvote_count, answer_count, create_time, update_time)
VALUES
    (7001, 2, 'Spring 循环依赖面试题应该答到什么深度？', '我现在能讲出三级缓存这个关键词，但每次都说不清为什么是三级缓存，也不知道面试官会不会继续追问构造器注入的问题。想请大家给一个更稳妥的回答结构。', 1, 'approved', 6, 2, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY),
    (7002, 4, 'MySQL Explain 里哪些字段最值得先看？', '每次排查 SQL 我都会看 explain，但字段很多。除了 type 和 key，还有哪些字段应该优先关注？有没有一套快速判断是否需要加索引的方法？', 5, 'approved', 4, 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY),
    (7003, 3, 'Redis 缓存雪崩和击穿在面试里怎么区分？', '我知道它们都和缓存失效有关，但总会混。有没有一个便于记忆的解释方式，最好还能顺手带上常见解决方案。', 7, 'approved', 3, 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 12 HOUR),
    (7004, 10, '微服务链路追踪应该先理解哪些基础概念？', '我在看 SkyWalking 和 OpenTelemetry，但概念很多，像 trace、span、context propagation 总是混在一起。想知道面试回答时最少要把哪些概念讲清楚。', 11, 'pending', 0, 0, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR),
    (7005, 7, '并发题里讲 CAS 时，除了 ABA 还会被追问什么？', '最近刷并发题时发现自己只会背 CAS 的定义和 ABA 问题，但面试官经常继续追问自旋开销、总线竞争以及和 synchronized 的取舍。有没有一份更完整的回答框架？', 9, 'pending', 0, 0, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR),
    (7006, 8, 'JVM 调优时应该先看 GC 日志还是监控面板？', '线上偶尔会遇到接口抖动，但日志和监控数据都很多。我想知道定位 Full GC 或内存泄漏时，第一步应该从哪里下手。', 4, 'rejected', 1, 0, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY),
    (7007, 9, '请问有人整理过常见系统设计八股模板吗？', '想找一些现成模板快速背诵，最好不用自己理解太多。', 11, 'hidden', 0, 0, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 2 DAY),
    (7008, 12, 'Redis 双写一致性最容易在哪些场景翻车？', '我们项目最近准备把热点接口接入 Redis 缓存，我担心上线后会出现缓存和数据库不一致。想提前梳理几个最容易出问题的场景和排查方式。', 7, 'approved', 7, 3, NOW() - INTERVAL 18 HOUR, NOW() - INTERVAL 1 HOUR)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    title = VALUES(title),
    content = VALUES(content),
    category_id = VALUES(category_id),
    status = VALUES(status),
    upvote_count = VALUES(upvote_count),
    answer_count = VALUES(answer_count),
    update_time = VALUES(update_time);

INSERT INTO community_answer (id, question_id, user_id, content, status, is_accepted, upvote_count, create_time, update_time)
VALUES
    (8001, 7001, 3, '可以按“定义 -> 解决范围 -> 核心机制 -> 例外情况”来答。先定义循环依赖，再说明 Spring 主要解决单例 Bean 的 Setter 注入循环依赖，核心是三级缓存，最后补一句构造器注入和多例 Bean 不能靠这套机制自动解决。', 'approved', 1, 8, NOW() - INTERVAL 4 DAY + INTERVAL 1 HOUR, NOW() - INTERVAL 2 DAY),
    (8002, 7001, 5, '如果时间不够，最少要说出“三级缓存 + Setter 注入 + 构造器不支持”这三个点，否则很容易被追问。', 'approved', 0, 2, NOW() - INTERVAL 3 DAY + INTERVAL 3 HOUR, NOW() - INTERVAL 3 DAY + INTERVAL 3 HOUR),
    (8003, 7002, 2, '我一般先看 type、key、rows、Extra。type 能快速判断扫描方式，rows 反映预估扫描量，Extra 里如果出现 Using filesort 或 Using temporary 就要警惕。', 'approved', 0, 5, NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR, NOW() - INTERVAL 1 DAY),
    (8004, 7003, 4, '击穿更像“一个热点 key 失效后大量请求打到数据库”，雪崩更像“很多 key 在同一时间集中失效”。回答时顺手带上互斥锁、热点永不过期和过期时间打散就够用了。', 'approved', 0, 4, NOW() - INTERVAL 1 DAY + INTERVAL 1 HOUR, NOW() - INTERVAL 12 HOUR),
    (8005, 7008, 3, '最常见的问题是数据库写成功但缓存删除失败，或者读请求在双删间隙读到了旧值。回答时可以按“写路径、读路径、重试补偿、最终一致性”四个角度展开。', 'approved', 1, 6, NOW() - INTERVAL 17 HOUR, NOW() - INTERVAL 3 HOUR),
    (8006, 7008, 11, '如果是高并发热点 key，除了双删，还可以补一个延迟删和消息重试，否则很难保证所有节点同时感知到更新。', 'approved', 0, 3, NOW() - INTERVAL 15 HOUR, NOW() - INTERVAL 2 HOUR),
    (8007, 7008, 2, '先从最容易出错的更新路径画时序图，再决定是否要引入 binlog 或消息队列做异步修正。', 'approved', 0, 2, NOW() - INTERVAL 10 HOUR, NOW() - INTERVAL 1 HOUR),
    (8008, 7004, 11, '如果从面试回答角度说，可以先讲 trace 是一次请求，span 是链路中的一个节点，再补一句跨服务传播上下文依赖 traceId 和 spanId。', 'pending', 0, 0, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR),
    (8009, 7002, 10, '我一般只看 possible_keys 和 key，别的字段感觉意义不大。', 'rejected', 0, 0, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 5 HOUR)
ON DUPLICATE KEY UPDATE
    question_id = VALUES(question_id),
    user_id = VALUES(user_id),
    content = VALUES(content),
    status = VALUES(status),
    is_accepted = VALUES(is_accepted),
    upvote_count = VALUES(upvote_count),
    update_time = VALUES(update_time);

INSERT INTO community_vote (id, user_id, target_type, target_id, value, create_time)
VALUES
    (9001, 1, 'question', 7001, 1, NOW() - INTERVAL 3 DAY),
    (9002, 3, 'question', 7001, 1, NOW() - INTERVAL 3 DAY),
    (9003, 4, 'question', 7001, 1, NOW() - INTERVAL 2 DAY),
    (9004, 5, 'question', 7002, 1, NOW() - INTERVAL 1 DAY),
    (9005, 2, 'answer', 8001, 1, NOW() - INTERVAL 2 DAY),
    (9006, 4, 'answer', 8001, 1, NOW() - INTERVAL 2 DAY),
    (9007, 1, 'answer', 8003, 1, NOW() - INTERVAL 1 DAY),
    (9008, 3, 'answer', 8004, 1, NOW() - INTERVAL 12 HOUR)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    target_type = VALUES(target_type),
    target_id = VALUES(target_id),
    value = VALUES(value),
    create_time = VALUES(create_time);

-- ============================================================
-- 用户统计
-- ============================================================
INSERT INTO user_stats (id, user_id, total_score, interview_count, avg_score, review_streak, total_reviews, community_score, community_questions, community_answers, community_accepted, rank_title, create_time, update_time)
VALUES
    (6001, 1, 233.00, 3, 77.67, 2, 5, 96, 0, 0, 0, '见习生', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 HOUR),
    (6002, 2, 64.00, 1, 64.00, 1, 1, 168, 1, 1, 0, '初级工程师', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 HOUR),
    (6003, 3, 0.00, 0, 0.00, 0, 0, 326, 1, 1, 1, '中级工程师', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 HOUR),
    (6004, 4, 0.00, 0, 0.00, 0, 0, 214, 1, 1, 0, '初级工程师', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 HOUR),
    (6005, 5, 0.00, 0, 0.00, 0, 0, 112, 0, 1, 0, '初级工程师', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 HOUR),
    (6006, 6, 0.00, 0, 0.00, 0, 0, 18, 0, 0, 0, '见习生', NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 HOUR),
    (6007, 7, 88.00, 1, 88.00, 1, 2, 34, 1, 0, 0, '见习生', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 HOUR),
    (6008, 8, 0.00, 0, 0.00, 0, 0, 6, 1, 0, 0, '见习生', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 1 HOUR),
    (6009, 9, 0.00, 0, 0.00, 0, 0, 0, 1, 0, 0, '见习生', NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 1 HOUR),
    (6010, 10, 0.00, 0, 0.00, 0, 0, 12, 1, 0, 0, '见习生', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 HOUR),
    (6011, 11, 0.00, 0, 0.00, 0, 0, 46, 0, 2, 0, '见习生', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 1 HOUR),
    (6012, 12, 142.00, 2, 71.00, 2, 4, 412, 1, 2, 1, '中级工程师', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 HOUR)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    total_score = VALUES(total_score),
    interview_count = VALUES(interview_count),
    avg_score = VALUES(avg_score),
    review_streak = VALUES(review_streak),
    total_reviews = VALUES(total_reviews),
    community_score = VALUES(community_score),
    community_questions = VALUES(community_questions),
    community_answers = VALUES(community_answers),
    community_accepted = VALUES(community_accepted),
    rank_title = VALUES(rank_title),
    update_time = VALUES(update_time);

-- ============================================================
-- 通知数据
-- ============================================================
INSERT INTO notification (id, user_id, type, title, content, link, is_read, create_time, update_time)
VALUES
    (6501, 1, 'review_remind', '今日复习已到期', '你有 2 道错题今天需要复习，建议先完成错题再做新面试。', '/review', 0, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR),
    (6502, 1, 'interview_feedback', '面试结果已生成', 'MySQL 模拟面试结果已经生成，平均分 71 分，可以查看逐题点评。', '/interview/detail/5102', 0, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),
    (6503, 1, 'community_vote', '你的收藏问题收到新点赞', '社区里关于 Spring 循环依赖的问题又收到新的点赞。', '/community/question/7001', 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
    (6504, 2, 'community_accept', '你的回答被采纳', '你在问题「Spring 循环依赖面试题应该答到什么深度？」下的回答已被采纳。', '/community/question/7001', 0, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),
    (6505, 3, 'rank_upgrade', '社区等级提升', '你的社区等级已提升为中级工程师。', '/community/leaderboard', 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    type = VALUES(type),
    title = VALUES(title),
    content = VALUES(content),
    link = VALUES(link),
    is_read = VALUES(is_read),
    update_time = VALUES(update_time);

-- ============================================================
-- 设备与登录日志
-- ============================================================
INSERT INTO login_device (id, user_id, device_fingerprint, device_name, ip, city, last_active_time, status, create_time, update_time)
VALUES
    (3101, 1, 'seed-macbook-demo', 'Chrome on macOS', '127.0.0.1', '上海', NOW() - INTERVAL 20 MINUTE, 1, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 20 MINUTE),
    (3102, 1, 'seed-ipad-demo', 'Safari on iPad', '127.0.0.1', '上海', NOW() - INTERVAL 2 DAY, 0, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 2 DAY),
    (3103, 2, 'seed-windows-learner', 'Edge on Windows', '127.0.0.1', '杭州', NOW() - INTERVAL 1 HOUR, 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 1 HOUR)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    device_fingerprint = VALUES(device_fingerprint),
    device_name = VALUES(device_name),
    ip = VALUES(ip),
    city = VALUES(city),
    last_active_time = VALUES(last_active_time),
    status = VALUES(status),
    update_time = VALUES(update_time);

INSERT INTO login_log (id, user_id, ip, city, device, status, fail_reason, create_time)
VALUES
    (3201, 1, '127.0.0.1', '上海', 'Chrome on macOS', 1, NULL, NOW() - INTERVAL 20 MINUTE),
    (3202, 1, '127.0.0.1', '上海', 'Safari on iPad', 1, NULL, NOW() - INTERVAL 2 DAY),
    (3203, 1, '127.0.0.1', '上海', 'Chrome on macOS', 0, 'Bad credentials', NOW() - INTERVAL 3 DAY),
    (3204, 2, '127.0.0.1', '杭州', 'Edge on Windows', 1, NULL, NOW() - INTERVAL 1 HOUR),
    (3205, 3, '127.0.0.1', '北京', 'Chrome on macOS', 1, NULL, NOW() - INTERVAL 4 HOUR),
    (3206, 4, '127.0.0.1', '深圳', 'Firefox on Linux', 1, NULL, NOW() - INTERVAL 1 DAY)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    ip = VALUES(ip),
    city = VALUES(city),
    device = VALUES(device),
    status = VALUES(status),
    fail_reason = VALUES(fail_reason),
    create_time = VALUES(create_time);
