/* ===================================================================
 *  EnhancedMobs サイトのレンダラ（マルチページ対応）
 *  content.js の window.SITE を読み、<body data-page="..."> を見て
 *  そのページに必要なセクションだけを組み立てる。
 *  ★ 通常このファイルは編集不要。文言は content.js を編集する。
 * =================================================================== */
(function () {
  "use strict";

  var S = window.SITE;
  if (!S) { document.body.innerHTML = "<p style='padding:2rem'>content.js が読み込めませんでした。</p>"; return; }

  var PAGE = document.body.getAttribute("data-page") || "home";

  /* --- ヘルパー --- */
  function el(tag, cls, html) {
    var e = document.createElement(tag);
    if (cls) e.className = cls;
    if (html != null) e.innerHTML = html;
    return e;
  }
  function add(parent, child) { parent.appendChild(child); return child; }
  function section(id, cls) {
    var s = el("section", cls || "section");
    if (id) s.id = id;
    return s;
  }
  function head(parent, heading, sub) {
    var h = add(parent, el("div", "section-head"));
    add(h, el("h2", null, heading || ""));
    if (sub) add(h, el("p", null, sub));
  }

  if (S.meta) {
    var navItem = (S.nav || []).filter(function (n) { return n.page === PAGE; })[0];
    var t = S.meta.pageTitle || "EnhancedMobs";
    if (navItem && PAGE !== "home") t = navItem.label + " — EnhancedMobs";
    document.title = t;
    var md = document.querySelector('meta[name="description"]');
    if (md && S.meta.description) md.setAttribute("content", S.meta.description);
  }

  function renderHeader() {
    var h = el("header", "site-header");
    var brand = add(h, el("a", "brand",
      (S.meta.brandPre || "") + '<span>' + (S.meta.brandAccent || "") + '</span>'));
    brand.href = "index.html";
    var nav = add(h, el("nav", "nav"));
    (S.nav || []).forEach(function (n) {
      var a = add(nav, el("a", n.page === PAGE ? "active" : null, n.label));
      a.href = n.href;
    });
    return h;
  }

  /* --- ヒーロー --- */
  function renderHero() {
    var sec = section("top", "hero");
    add(sec, el("div", "hero-glow"));
    var H = S.hero || {};
    if (H.eyebrow) add(sec, el("p", "eyebrow", H.eyebrow));
    add(sec, el("h1", null, H.title || ""));
    if (H.lede) add(sec, el("p", "lede", H.lede));
    if (H.ctas && H.ctas.length) {
      var row = add(sec, el("div", "cta-row"));
      H.ctas.forEach(function (c) {
        var a = add(row, el("a", "btn " + (c.primary ? "btn-primary" : "btn-ghost"), c.label));
        a.href = c.href;
        if (/^https?:/.test(c.href)) { a.target = "_blank"; a.rel = "noopener"; }
      });
    }
    if (H.badges && H.badges.length) {
      var ul = add(sec, el("ul", "hero-badges"));
      H.badges.forEach(function (b) {
        add(ul, el("li", null, '<strong>' + b.value + '</strong> ' + b.label));
      });
    }
    return sec;
  }

  /* --- 3本柱（カードは href があれば詳細ページへのリンク） --- */
  function renderPillars() {
    var P = S.pillars; if (!P) return null;
    var sec = section("pillars");
    head(sec, P.heading, P.sub);
    var grid = add(sec, el("div", "cards-3"));
    (P.cards || []).forEach(function (c) {
      var card = el(c.href ? "a" : "article", "card" + (c.href ? " card-link" : ""));
      if (c.href) card.href = c.href;
      if (c.num) add(card, el("div", "card-num", c.num));
      add(card, el("h3", null, c.title || ""));
      add(card, el("p", null, c.body || ""));
      if (c.href) add(card, el("span", "card-more", "詳しく →"));
      grid.appendChild(card);
    });
    return sec;
  }

  /* --- レベリング詳細 --- */
  function renderLeveling() {
    var L = S.leveling; if (!L) return null;
    var sec = section("leveling");
    head(sec, L.heading, L.sub);
    if (L.intro) add(sec, el("p", "lead-intro", L.intro));
    if (L.mechanics && L.mechanics.length) {
      var grid = add(sec, el("div", "cards-3"));
      L.mechanics.forEach(function (m) {
        var card = add(grid, el("article", "card"));
        add(card, el("h3", null, m.title || ""));
        add(card, el("p", null, m.body || ""));
      });
    }
    if (L.bosses) {
      add(sec, el("h3", "subhead", L.bosses.heading || ""));
      if (L.bosses.sub) add(sec, el("p", "note", L.bosses.sub));
      var ul = add(sec, el("ul", "kv-list"));
      (L.bosses.items || []).forEach(function (it) {
        var li = add(ul, el("li"));
        add(li, el("span", "kv-key", it.name || ""));
        add(li, el("span", "kv-val", it.body || ""));
      });
    }
    return sec;
  }

  /* --- アイテム強化（詳細） --- */
  function renderEnhance() {
    var E = S.enhance; if (!E) return null;
    var sec = section("enhance");
    head(sec, E.heading, E.sub);
    if (E.intro) add(sec, el("p", "lead-intro", E.intro));

    // 工程フロー（チップ）
    if (E.steps && E.steps.length) {
      var ol = add(sec, el("ol", "progression"));
      E.steps.forEach(function (s) {
        var li = add(ol, el("li"));
        add(li, el("span", "step-name", s.name || ""));
        add(li, el("span", "step-desc", s.desc || ""));
      });
    }

    // 各工程の詳細
    (E.methods || []).forEach(function (m) {
      var box = add(sec, el("article", "method"));
      var hd = add(box, el("div", "method-head"));
      add(hd, el("h3", "method-name", m.name || ""));
      if (m.tool) add(hd, el("span", "method-tool", m.tool));
      if (m.body) add(box, el("p", "method-body", m.body));
      if (m.tips && m.tips.length) {
        var ul = add(box, el("ul", "method-tips"));
        m.tips.forEach(function (t) { add(ul, el("li", null, t)); });
      }
    });

    // 金床でできること
    if (E.anvil) {
      add(sec, el("h3", "subhead", E.anvil.heading || ""));
      var aul = add(sec, el("ul", "kv-list"));
      (E.anvil.items || []).forEach(function (it) {
        var li = add(aul, el("li"));
        add(li, el("span", "kv-key", it.action || ""));
        add(li, el("span", "kv-note", it.how || ""));
      });
    }

    // 破壊寸前
    if (E.broken) {
      add(sec, el("h3", "subhead", E.broken.heading || ""));
      add(sec, el("p", "note", E.broken.body || ""));
    }

    if (E.note) add(sec, el("p", "note", E.note));
    return sec;
  }

  /* --- スキル --- */
  function renderSkills() {
    var SK = S.skills; if (!SK) return null;
    var sec = section("skills");
    head(sec, SK.heading, SK.sub);
    var grid = add(sec, el("div", "skill-grid"));
    (SK.groups || []).forEach(function (g) {
      var box = add(grid, el("div", "skill-group"));
      add(box, el("h4", null, g.category || ""));
      var ul = add(box, el("ul"));
      (g.items || []).forEach(function (it) {
        var li = add(ul, el("li"));
        var nm = el("span", "skill-name", it.name || "");
        if (it.off) nm.innerHTML += ' <span class="off">既定OFF</span>';
        li.appendChild(nm);
        if (it.desc) add(li, el("span", "skill-desc", it.desc));
      });
    });
    return sec;
  }

  /* --- 特性 --- */
  function traitCard(grid, it) {
    var card = add(grid, el("div", "trait-card"));
    add(card, el("span", "trait-name", it.name || ""));
    if (it.desc) add(card, el("span", "trait-desc", it.desc));
  }
  function renderTraits() {
    var T = S.traits; if (!T) return null;
    var sec = section("traits");
    head(sec, T.heading, T.sub);
    if (T.groups && T.groups.length) {
      T.groups.forEach(function (g) {
        add(sec, el("h3", "trait-group-title", g.category || ""));
        var grid = add(sec, el("div", "trait-grid"));
        (g.items || []).forEach(function (it) { traitCard(grid, it); });
      });
    } else if (T.items && T.items.length) {
      var grid = add(sec, el("div", "trait-grid"));
      T.items.forEach(function (it) { traitCard(grid, it); });
    } else if (T.note) {
      add(sec, el("p", "note", T.note));
    }
    return sec;
  }

  /* --- 難易度詳細 --- */
  function renderDifficulty() {
    var D = S.difficulty; if (!D) return null;
    var sec = section("difficulty");
    head(sec, D.heading, D.sub);
    if (D.intro) add(sec, el("p", "lead-intro", D.intro));
    if (D.factors && D.factors.length) {
      var grid = add(sec, el("div", "cards-3"));
      D.factors.forEach(function (f) {
        var card = add(grid, el("article", "card"));
        add(card, el("h3", null, f.title || ""));
        add(card, el("p", null, f.body || ""));
      });
    }
    return sec;
  }

  /* --- 設定 (config.yml) --- */
  function renderConfig() {
    var C = S.config; if (!C) return null;
    var sec = section("config");
    head(sec, C.heading, C.sub);
    if (C.note) add(sec, el("p", "note", C.note));
    (C.groups || []).forEach(function (g) {
      add(sec, el("h3", "cfg-group-title", g.name || ""));
      var table = add(sec, el("table", "cfg-table"));
      var thead = add(add(table, el("thead")), el("tr"));
      ["キー", "既定値", "説明"].forEach(function (h) { add(thead, el("th", null, h)); });
      var tbody = add(table, el("tbody"));
      (g.items || []).forEach(function (it) {
        var tr = add(tbody, el("tr"));
        add(tr, el("td", "cfg-key")).appendChild(el("code", null, it.key || ""));
        add(tr, el("td", "cfg-def", it.def || ""));
        add(tr, el("td", "cfg-desc", it.desc || ""));
      });
    });
    return sec;
  }

  /* --- カスタムディメンション --- */
  function renderDimension() {
    var D = S.dimension; if (!D) return null;
    var sec = section("dimension", "section section-alt");
    head(sec, D.heading, D.sub);
    add(sec, el("div", "dim-box", D.body || ""));
    return sec;
  }

  /* --- コマンド --- */
  function renderCommands() {
    var C = S.commands; if (!C) return null;
    var sec = section("commands");
    head(sec, C.heading, C.sub);
    var ul = add(sec, el("ul", "cmd-list"));
    (C.items || []).forEach(function (it) {
      var li = add(ul, el("li"));
      add(li, el("code", null, it.cmd || ""));
      add(li, el("span", null, it.desc || ""));
    });
    return sec;
  }

  /* --- attributelib --- */
  function renderAttributelib() {
    var A = S.attributelib; if (!A) return null;
    var sec = section("attributelib", "section section-alt");
    head(sec, A.heading, A.sub);
    if (A.intro) add(sec, el("p", "alib-intro", A.intro));
    if (A.layers && A.layers.length) {
      var grid = add(sec, el("div", "cards-3 alib-layers"));
      A.layers.forEach(function (l) {
        var card = add(grid, el("article", "card"));
        add(card, el("h3", null, l.title || ""));
        add(card, el("p", null, l.body || ""));
      });
    }
    if (A.points && A.points.length) {
      var ul = add(sec, el("ul", "alib-points"));
      A.points.forEach(function (p) { add(ul, el("li", null, p)); });
    }
    if (A.commands && A.commands.length) {
      var cul = add(sec, el("ul", "cmd-list"));
      A.commands.forEach(function (it) {
        var li = add(cul, el("li"));
        add(li, el("code", null, it.cmd || ""));
        add(li, el("span", null, it.desc || ""));
      });
    }
    return sec;
  }

  /* --- 導入 --- */
  function renderInstall() {
    var I = S.install; if (!I) return null;
    var sec = section("install");
    head(sec, I.heading, I.sub);
    var grid = add(sec, el("div", "install-grid"));
    if (I.requirements) {
      var r = add(grid, el("div", "req"));
      add(r, el("h4", null, I.requirements.title || ""));
      var rul = add(r, el("ul"));
      (I.requirements.items || []).forEach(function (x) { add(rul, el("li", null, x)); });
    }
    if (I.steps) {
      var st = add(grid, el("div", "steps"));
      add(st, el("h4", null, I.steps.title || ""));
      var sol = add(st, el("ol"));
      (I.steps.items || []).forEach(function (x) { add(sol, el("li", null, x)); });
    }
    if (I.note) add(sec, el("p", "note", I.note));
    return sec;
  }

  /* --- 更新履歴：1エントリを描画 --- */
  var TYPE_LABEL = { "new": "新規", "fix": "修正", "change": "変更" };
  function changelogEntry(parent, e) {
    var box = add(parent, el("div", "cl-entry"));
    var hd = add(box, el("div", "cl-head"));
    add(hd, el("span", "cl-version", e.version || ""));
    add(hd, el("span", "cl-date", e.date || ""));
    var ul = add(box, el("ul", "cl-changes"));
    (e.changes || []).forEach(function (c) {
      var li = add(ul, el("li"));
      var t = c.type || "change";
      add(li, el("span", "cl-tag cl-" + t, TYPE_LABEL[t] || t));
      add(li, el("span", "cl-text", c.text || ""));
    });
  }

  /* --- 更新履歴：全件（changelog ページ） --- */
  function renderChangelog() {
    var C = S.changelog; if (!C) return null;
    var sec = section("changelog");
    head(sec, C.heading, C.sub);
    if (C.note) add(sec, el("p", "note", C.note));
    (C.entries || []).forEach(function (e) { changelogEntry(sec, e); });
    return sec;
  }

  /* --- 更新履歴：最新数件（home） --- */
  function renderLatestUpdates() {
    var C = S.changelog; if (!C || !C.entries || !C.entries.length) return null;
    var sec = section("updates", "section section-alt");
    head(sec, "最新の更新", null);
    var n = C.latestOnHome || 3;
    C.entries.slice(0, n).forEach(function (e) { changelogEntry(sec, e); });
    var more = add(sec, el("div", "more-row"));
    var a = add(more, el("a", "btn btn-ghost", "更新履歴をすべて見る →"));
    a.href = "changelog.html";
    return sec;
  }

  /* --- フッター --- */
  function renderFooter() {
    var F = S.footer || {};
    var f = el("footer", "site-footer");
    add(f, el("p", null, F.title || ""));
    if (F.sub) add(f, el("p", "muted", F.sub));
    return f;
  }

  /* --- ページ別の組み立て --- */
  var LAYOUT = {
    home:       [renderHero, renderPillars, renderDimension, renderCommands, renderAttributelib, renderInstall, renderLatestUpdates],
    leveling:   [renderLeveling],
    enhance:    [renderEnhance],
    skills:     [renderSkills],
    traits:     [renderTraits],
    difficulty: [renderDifficulty],
    config:     [renderConfig],
    changelog:  [renderChangelog],
  };

  var body = document.body;
  body.appendChild(renderHeader());
  var main = el("main");
  (LAYOUT[PAGE] || LAYOUT.home).forEach(function (fn) {
    var node = fn();
    if (node) main.appendChild(node);
  });
  body.appendChild(main);
  body.appendChild(renderFooter());
})();
