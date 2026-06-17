/* ===================================================================
 *  EnhancedMobs サイトのレンダラ
 *  content.js の window.SITE を読み、ページを組み立てる。
 *  ★ 通常このファイルは編集不要。文言は content.js を編集する。
 * =================================================================== */
(function () {
  "use strict";

  var S = window.SITE;
  if (!S) { document.body.innerHTML = "<p style='padding:2rem'>content.js が読み込めませんでした。</p>"; return; }

  /* --- 小さなヘルパー --- */
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

  /* --- <head> 反映 --- */
  if (S.meta) {
    if (S.meta.pageTitle) document.title = S.meta.pageTitle;
    var md = document.querySelector('meta[name="description"]');
    if (md && S.meta.description) md.setAttribute("content", S.meta.description);
  }

  /* --- ヘッダー --- */
  function renderHeader() {
    var h = el("header", "site-header");
    var brand = add(h, el("a", "brand",
      (S.meta.brandPre || "") + '<span>' + (S.meta.brandAccent || "") + '</span>'));
    brand.href = "#top";
    var nav = add(h, el("nav", "nav"));
    (S.nav || []).forEach(function (n) {
      var a = add(nav, el("a", null, n.label));
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

  /* --- 3本柱 --- */
  function renderPillars() {
    var P = S.pillars; if (!P) return null;
    var sec = section("pillars");
    head(sec, P.heading, P.sub);
    var grid = add(sec, el("div", "cards-3"));
    (P.cards || []).forEach(function (c) {
      var card = add(grid, el("article", "card"));
      if (c.num) add(card, el("div", "card-num", c.num));
      add(card, el("h3", null, c.title || ""));
      add(card, el("p", null, c.body || ""));
    });
    return sec;
  }

  /* --- アイテム強化 --- */
  function renderEnhance() {
    var E = S.enhance; if (!E) return null;
    var sec = section("enhance", "section section-alt");
    head(sec, E.heading, E.sub);
    var ol = add(sec, el("ol", "progression"));
    (E.steps || []).forEach(function (s) {
      var li = add(ol, el("li"));
      add(li, el("span", "step-name", s.name || ""));
      add(li, el("span", "step-desc", s.desc || ""));
    });
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
        var head = el("span", "skill-name", it.name || "");
        if (it.off) head.innerHTML += ' <span class="off">既定OFF</span>';
        li.appendChild(head);
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
    var sec = section("traits", "section section-alt");
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

  /* --- カスタムディメンション --- */
  function renderDimension() {
    var D = S.dimension; if (!D) return null;
    var sec = section("dimension");
    head(sec, D.heading, D.sub);
    add(sec, el("div", "dim-box", D.body || ""));
    return sec;
  }

  /* --- コマンド --- */
  function renderCommands() {
    var C = S.commands; if (!C) return null;
    var sec = section("commands", "section section-alt");
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
    var sec = section("attributelib");
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
    var sec = section("install", "section section-alt");
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

  /* --- フッター --- */
  function renderFooter() {
    var F = S.footer || {};
    var f = el("footer", "site-footer");
    add(f, el("p", null, F.title || ""));
    if (F.sub) add(f, el("p", "muted", F.sub));
    return f;
  }

  /* --- 組み立て --- */
  var body = document.body;
  body.appendChild(renderHeader());

  var main = el("main");
  main.id = "top";
  [
    renderHero, renderPillars, renderEnhance, renderSkills, renderTraits,
    renderDimension, renderCommands, renderAttributelib, renderInstall,
  ].forEach(function (fn) {
    var node = fn();
    if (node) main.appendChild(node);
  });
  body.appendChild(main);

  body.appendChild(renderFooter());
})();
