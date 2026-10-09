// M1 · 最小 TVBox JS 源示例（QuickJS 侧可直接跑通 home/search）
// 说明：这是"自研协议"下源 JS 的最小可运行示例。
//       内置方法（get/post/json2object/request）由 Kotlin 侧注入到全局，
//       源 JS 直接调用它们拿数据，main() 返回 JSON 字符串。

// ===== 数据示例（实际由内置 get(url) 返回，这里内联便于开发期演示）=====
var CLASS = [
  { type: 1, name: "电影", style: 0 },
  { type: 2, name: "连续剧", style: 0 },
  { type: 3, name: "综艺", style: 0 }
];

var VODS = [
  {
    vod_id: 1001, vod_name: "示例电影A", vod_pic: "https://example.com/a.jpg",
    vod_year: "2024", vod_area: "内地", vod_cls: "动作",
    vod_remarks: "更新至24集", vod_content: "简介",
    vod_actor: "演员A 演员B", vod_director: "导演A", vod_score: "8.5",
    vod_play_from: "monday",
    vod_play_url: "第01集$http://cdn.example.com/a_01.m3u8#第02集$http://cdn.example.com/a_02.m3u8#第03集$http://cdn.example.com/a_03.m3u8"
  },
  {
    vod_id: 1002, vod_name: "示例剧集B", vod_pic: "https://example.com/b.jpg",
    vod_year: "2023", vod_area: "日本", vod_cls: "动漫",
    vod_remarks: "全12集", vod_content: "简介B",
    vod_actor: "声优A", vod_director: "导演B", vod_score: "9.0",
    vod_play_from: "lzm3u8",
    vod_play_url: "第01集$http://cdn.example.com/b_01.m3u8#第02集$http://cdn.example.com/b_02.m3u8"
  }
];

// ===== 13 接口实现（QuickJS 侧）=====
function home() {
  // 实际：return get("https://.../home"); 这里内联
  return json2object(JSON.stringify({ class: CLASS }));
}

function homeVod(data) {
  var page = data.page || 1, pages = 1, total = VODS.length;
  return JSON.stringify({ list: VODS, page: page, pages: pages, total: total });
}

function category(type, page, filter, extend) {
  var list = (type === "1") ? [VODS[0]] : (type === "2" ? [VODS[1]] : VODS);
  return JSON.stringify({ list: list, page: page || 1, pages: 1, total: list.length });
}

function detail(id) {
  var v = VODS.filter(function (x) { return String(x.vod_id) === String(id); })[0];
  return v ? JSON.stringify({ vod: v, player: "m3u8" }) : "{}";
}

function search(kw, page, filter) {
  var kw2 = (kw || "").toLowerCase();
  var list = VODS.filter(function (x) {
    return x.vod_name.toLowerCase().indexOf(kw2) >= 0;
  });
  return JSON.stringify({ list: list, page: page || 1, pages: 1, total: list.length });
}

function play(id, flag, ep, alias) {
  var v = VODS.filter(function (x) { return String(x.vod_id) === String(id); })[0];
  if (!v) return "";
  // ep 形如 "第01集"；在 vod_play_url 里找 "第01集$url"
  var parts = v.vod_play_url.split("#");
  for (var i = 0; i < parts.length; i++) {
    if (parts[i].indexOf(ep + "$") === 0) return parts[i].substring(ep.length + 1);
  }
  return parts[0];
}

function proxy(url) {
  return JSON.stringify({ url: url, headers: {} });
}

function classify() {
  return JSON.stringify({ type: [{ 1: ["全部", "动作"], 2: ["全部", "动漫"] }] });
}

function test() {
  return JSON.stringify({ node: { ok: true } });
}

// ===== 主入口：Kotlin 侧调用 main(action, arg) =====
function main() {
  // 演示：home + search("电影")
  var result = {
    home: home(),
    search_电影: search("电影", 1, {}),
    detail_1002: detail("1002")
  };
  __result__ = JSON.stringify(result);
  return __result__;
}
