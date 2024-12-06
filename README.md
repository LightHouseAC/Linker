## Linker 后端demo



学习目的的 demo 项目，原型参考主流的媒体平台，用于学习和熟悉一些后端开发技术，提升个人水平

设想为参考小红书&Bilibili仿做一个图文/视频内容分享平台的后端服务，完成部分为 一般用户 和 笔记 维度的数据处理



涉及业务为：

- 用户：注册、登录、注销、关注、取关

- 笔记：发布、点赞、取消点赞、收藏、取消收藏、发布、置顶、删除、仅发布人可见、搜索

- 其他：数据/文件本地存储、计数（发布数、关注数、点赞数、收藏数）、数据对齐



以下为项目包含的全部模块：

- linker-auth 鉴权服务
  - 处理用户登录、注册、账号注销等操作
- linker-count 计数服务
  - 处理接口中笔记和用户维度的数据统计操作
- linker-data-align 数据对齐服务
  - 用于保证本地数据库持久化存储的数据准确性
- linker-distributed-id-generator 分布式 ID 生成服务
  - 使用开源第三方组件用于生成唯一 ID
- linker-framework 基础设施
  - biz-context 上下文组件，用于临时保存用户 ID 供其他服务调用
  - biz-operation-log 业务日志组件，用于打印接口日志
  - jackson 封装 json 序列化和反序列化器的组件
  - linker-common 封装公共的工具类
- linker-gateway 网关
  - 用于接口路由做请求转发
  - 做接口统一鉴权和用户 ID 透传
- linker-kv 键值存储服务
  - 使用本地 Cassandra 数据库进行短文本存储
- linker-note 笔记服务
  - 笔记数据增删改查的业务逻辑
- linker-oss 对象存储服务
  - 使用本地 MinIO 文件存储系统做文件存储服务
- linker-search 搜索服务
  - 使用 ElasticSearch 做数据搜索服务
- linker-user 用户服务
  - 用户数据增删改查的业务逻辑
- linker-user-relation 用户关系服务
  - 处理用户关注、取关等操作
