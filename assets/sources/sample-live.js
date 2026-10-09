// M1 · 真实网络 TVBox JS 源示例（QuickJS 侧）。
// 不再内联数据，全部走 Kotlin 注入的内置 get(url) 取真实 JSON，再 parse。
// 合规：只对接"自有/授权"源；下面 base 是占位（换成你的自有域名）。
// 用法：Kotlin 侧 QuickJSSpider.init(本文件文本) 后调用 home()/search()/detail()/play()。

// ---------- 基础配置（换成你的自有/授权源）----------
var BASE = "https://your-own-api.example.com";  // 自有 API，不是 <competitor-endpoint>
var TOKEN_HEADER = "X-Auth";
var TOKEN = "dev-test-token";                     // MVP；真实走 M3 的 ECDH 会话密钥

// ---------- 内置 get() 的包装：加 auth header + 超时 ----------
function apiGet(path, opts) {
  // opts: {params:{}, headers:{}}
  var url = BASE + path;
  if (opts && opts.params) {
    var qs = [];
    for (var k in opts.params) qs.push(encodeURIComponent(k) + "=" + encodeURIComponent(opts.params[k]));
    if (qs.length) url += (url.indexOf("?") < 0 ? "?" : "&") + qs.join("&");
  }
  var headers = { "Accept": "application/json" };
  if (TOKEN) headers[TOKEN_HEADER] = TOKEN;
  if (opts && opts.headers) for (var k2 in opts.headers) headers[k2] = opts.headers[k2];
  // 内置 get 目前只收 URL；header 走 M3 的 EncryptedApiInterceptor（请求层统一加）
  return get(url);
}

// ---------- 13 接口 ----------
function home() {
  // 真实：GET /home 返回 {"class":[...]}
  var raw = apiGet("/home", {});
  return raw;
}

function homeVod(data) {
  // 真实：GET /home/list?types=1,2,3&page=1
  var types = (data.types || []).join(",");
  var page = data.page || 1;
  var raw = apiGet("/home/list", { params: { types: types, page: page } });
  return raw;
}

function category(type, page, filter, extend) {
  var p = { type: type, page: page || 1 };
  if (filter && filter.kw) p.kw = filter.kw;
  // extend 里的 area/year 等筛选
  if (extend) for (var k in extend) p[k] = extend[k];
  return apiGet("/category", { params: p });
}

function detail(id) {
  return apiGet("/detail/" + id, {});
}

function search(kw, page, filter) {
  return apiGet("/search", { params: { kw: kw, page: page || 1 } });
}

function play(id, flag, ep, alias) {
  // 真实：GET /play?id=&flag=&ep=  → 返回可播 m3u8（M3 下带时效 token）
  return apiGet("/play", { params: { id: id, flag: flag, ep: ep, alias: alias } });
}

function proxy(url) {
  // 流代理：把真实 m3u8 走服务端代理/加鉴权头
  return JSON.stringify({ url: url, headers: {} });
}

function classify() {
  return apiGet("/classify", {});
}

function test() {
  // 自检：打 home 看是否 200 + 有 class
  var raw = home();
  var ok = (raw || "").indexOf("\"class\"") >= 0;
  return JSON.stringify({ node: { ok: ok, sample: (raw || "").substring(0, 80) } });
}

// ---------- 内置方法（Kotlin 注入）----------
// get(url) / post(url, body) / json2object(str) / request(headers, url, options)
//   这些由 QuickJSSpiderApi 在 Kotlin 侧实现，本 JS 不重写。

// ---------- 主入口（供开发期一次性跑 home+search+detail）----------
function main() {
  var result = {
    home: home(),
    search_a: search("a", 1, {}),
    detail_1002: detail("1002")
  };
  __result__ = JSON.stringify(result);
  return __result__;
}
