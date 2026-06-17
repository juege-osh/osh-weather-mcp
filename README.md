# osh-weather-mcp

这是一个基于 Spring Boot 和 Spring AI MCP 的天气服务，既可以作为普通 HTTP 服务测试，也可以作为 MCP Server 联调。

## 先说结论

- 不需要大模型密钥也能用。
- 可以直接用 JavaScript 调用普通 HTTP 接口。
- 如果要按 MCP 协议调用，也可以不用大模型，让 Node.js 程序自己作为 MCP client。
- 这个项目里只有国内天气接口依赖高德 `api.key`，国外天气使用 Open-Meteo，不需要模型密钥。

## 当前项目暴露的能力

- 普通 HTTP 接口
  - `GET /getForeignWeather?city=Tokyo`
  - `GET /getWeather?city=110000`
- MCP Tools
  - `getForeignWeather(city)`
  - `getWeather(city)`
- MCP Streamable HTTP 端点
  - 默认是 `/mcp`

说明：`/mcp` 默认路径来自 Spring AI 官方文档中 `spring.ai.mcp.server.streamable-http.mcp-endpoint` 的默认值 `/mcp`。

## Linux 上最省事的测试方式

建议分两步：

1. 先测普通 HTTP 接口，确认天气功能本身没问题。
2. 再测 MCP，确认工具注册和协议调用都正常。

这样排错最快，不会一上来把问题混在一起。

## 环境要求

- 方式 A：本机 JDK 21
- 方式 B：Docker
- Linux 能访问外网

项目的 `pom.xml` 指定了：

```xml
<java.version>21</java.version>
```

## 启动方式

### 方式 A：本机启动

先给 Maven Wrapper 加执行权限：

```bash
chmod +x mvnw
```

启动服务：

```bash
./mvnw spring-boot:run
```

默认端口：

```text
http://localhost:32180
```

### 方式 B：Docker 启动

如果你不想在宿主机安装 JDK 21，直接用容器就行。仓库里已经补了 `Dockerfile` 和 `compose.yaml`。

构建并启动：

```bash
docker compose up --build
```

后台启动：

```bash
docker compose up -d --build
```

停止：

```bash
docker compose down
```

如果你想覆盖高德 Key，可以在启动前传环境变量：

```bash
export AMAP_API_KEY=你的key
docker compose up -d --build
```

## 方式一：直接测普通 HTTP 接口

这是最推荐的冒烟测试方式。

### 国外天气

```bash
curl "http://localhost:32180/getForeignWeather?city=Tokyo"
```

### 国内天气

```bash
curl "http://localhost:32180/getWeather?city=110000"
```

如果你想按城市名试，可以先看高德接口要求；当前代码是把 `city` 原样传给高德天气接口。

## 方式二：用静态网页 + JS 调普通 HTTP

可以，而且不需要大模型。

浏览器侧最简单的例子：

```html
<!doctype html>
<html lang="zh-CN">
<body>
  <button id="btn">查询东京天气</button>
  <pre id="result"></pre>

  <script>
    document.getElementById("btn").onclick = async () => {
      const res = await fetch("http://localhost:32180/getForeignWeather?city=Tokyo");
      const text = await res.text();
      document.getElementById("result").textContent = text;
    };
  </script>
</body>
</html>
```

为了方便静态页面测试，项目已经放开了测试常见来源的 CORS。

## 方式三：用 Node.js 调普通 HTTP

如果你不想处理浏览器跨域，Node.js 更省事。

```js
const res = await fetch("http://localhost:32180/getForeignWeather?city=Tokyo");
const text = await res.text();
console.log(text);
```

## 方式四：按 MCP 协议测试

当你要验证“这个服务是不是一个可用的 MCP server”时，再走这一步。

### MCP 端点

```text
http://localhost:32180/mcp
```

### 推荐方式

- 用 MCP Inspector
- 用支持 Streamable HTTP 的 Node.js MCP client
- 用你自己的 AI Agent 或网关去连接它

## 要不要大模型密钥

不需要，除非你的“客户端”本身就是一个要调用大模型的 AI 应用。

这里分开看：

- 调普通 HTTP 接口：不需要模型密钥
- 调 MCP 协议：不需要模型密钥
- 让 OpenAI / Claude / 其他大模型来自动调用这个 MCP：才需要对应模型密钥

## 实际建议

如果你的目标是“在测试环境先把 weather-mcp 跑通”，建议顺序如下：

1. 用 Docker 启动服务，或者安装 JDK 21 本机启动
2. 用 `curl` 验证 `/getForeignWeather`
3. 用浏览器静态页或 Node.js `fetch` 验证 HTTP 调用
4. 最后再接 MCP client 验证 `/mcp`

## 注意事项

- `src/main/resources/application.yaml` 已支持用环境变量 `AMAP_API_KEY` 覆盖默认值。
- 当前机器如果还是 JDK 8，不影响 Docker 方式运行，但会影响本机直接运行。
- 如果 Linux 机器不能访问外网，Open-Meteo 和高德接口都会失败。
