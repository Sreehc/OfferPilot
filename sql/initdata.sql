-- ByteCoach 测试数据
-- 在 init.sql 执行后运行此文件，插入初始测试数据。

USE bytecoach;

-- ============================================================
-- 默认管理员账号（密码: 123456）
-- ============================================================
INSERT IGNORE INTO user (id, username, password, nickname, role, status, source, create_time, update_time)
VALUES
    (1, 'demo', '$2a$10$0T3BFXGNjpcmWyfsFieXbudX82IlGMIC96SET0cfSUNFe40UPMjaC', 'ByteCoach', 'ADMIN', 1, 'seed', NOW(), NOW());

-- ============================================================
-- 分类数据
-- ============================================================
INSERT IGNORE INTO category (id, name, type, sort_order, status, create_time, update_time)
VALUES
    (1, 'Spring', 'question', 1, 1, NOW(), NOW()),
    (2, 'JVM', 'knowledge', 2, 1, NOW(), NOW()),
    (3, 'MySQL', 'interview', 3, 1, NOW(), NOW()),
    (4, 'JVM', 'question', 4, 1, NOW(), NOW()),
    (5, 'MySQL', 'question', 5, 1, NOW(), NOW());

-- ============================================================
-- 面试题库
-- ============================================================
INSERT IGNORE INTO question (
    id, title, category_id, type, difficulty, frequency, tags, standard_answer, score_standard, source, create_time, update_time
)
VALUES
    (
        1001,
        '解释 Spring AOP 的动态代理实现',
        1,
        'short_answer',
        'medium',
        8,
        'Spring,AOP,代理',
        'Spring AOP 常见实现方式是 JDK 动态代理和 CGLIB。接口代理优先使用 JDK 动态代理，目标类没有接口时通常使用 CGLIB。',
        '至少提到 JDK 动态代理、CGLIB 以及两者适用边界。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1002,
        'JVM CMS 和 G1 的主要差异是什么？',
        4,
        'short_answer',
        'hard',
        7,
        'JVM,GC,CMS,G1',
        'CMS 以最短停顿为目标但会产生内存碎片，G1 面向服务端大堆内存，按 Region 回收并兼顾吞吐与停顿。',
        '至少说明目标、内存管理方式和典型问题。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1003,
        'MySQL 为什么会出现索引失效？',
        5,
        'short_answer',
        'medium',
        6,
        'MySQL,索引,执行计划',
        '常见原因包括最左前缀被破坏、列上使用函数或表达式、隐式类型转换、范围查询后继续使用联合索引列等。',
        '至少给出 3 个典型场景。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1004,
        'Spring Bean 的生命周期是怎样的？',
        1,
        'short_answer',
        'medium',
        7,
        'Spring,Bean,生命周期',
        'Spring Bean 的生命周期包括：实例化 → 属性注入 → Aware 回调 → BeanPostProcessor 前置处理 → InitializingBean.afterPropertiesSet() → 自定义 init-method → BeanPostProcessor 后置处理 → 使用 → DisposableBean.destroy() → 自定义 destroy-method。',
        '至少提到实例化、属性注入、初始化回调、销毁回调四个阶段。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1005,
        '什么是 Spring 循环依赖？如何解决？',
        1,
        'short_answer',
        'hard',
        6,
        'Spring,循环依赖,三级缓存',
        '循环依赖指两个或多个 Bean 相互持有对方的引用。Spring 通过三级缓存解决 Setter 注入的循环依赖：singletonObjects（完整 Bean）→ earlySingletonObjects（半成品 Bean）→ singletonFactories（ObjectFactory）。构造器注入的循环依赖无法自动解决。',
        '至少提到三级缓存机制和构造器注入的限制。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1006,
        'JVM 内存模型中堆和方法区的区别？',
        4,
        'short_answer',
        'easy',
        5,
        'JVM,内存模型,堆,方法区',
        '堆是线程共享的，存放对象实例，是 GC 的主要区域。方法区（元空间）存放类信息、常量、静态变量，JDK 8 后使用本地内存实现。',
        '至少说明线程共享特性和存储内容差异。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1007,
        'MySQL 事务隔离级别有哪些？InnoDB 默认是哪个？',
        5,
        'short_answer',
        'medium',
        8,
        'MySQL,事务,隔离级别',
        '四个隔离级别：READ UNCOMMITTED、READ COMMITTED、REPEATABLE READ、SERIALIZABLE。InnoDB 默认 REPEATABLE READ，通过 MVCC + Next-Key Lock 解决大部分幻读问题。',
        '至少列出四个级别并说明 InnoDB 默认级别。',
        'seed',
        NOW(),
        NOW()
    ),
    (
        1008,
        '解释 TCP 三次握手和四次挥手',
        3,
        'short_answer',
        'medium',
        6,
        'TCP,网络,三次握手',
        '三次握手：SYN → SYN+ACK → ACK，用于建立可靠连接。四次挥手：FIN → ACK → FIN → ACK，因为 TCP 全双工，双方需各自关闭发送通道。',
        '至少说明握手目的和挥手为何需要四次。',
        'seed',
        NOW(),
        NOW()
    );
