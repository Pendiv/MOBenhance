# EnhancedMobs

**モンスターにレベルと特性を。**

出現するモンスターは成長し、特性を持つ。プレイヤーの動きに応じて、その場の危険度が変わる。
既存ワールドの緊張感を作り変える、Minecraft サーバー（Paper）用プラグインです。

![version](https://img.shields.io/badge/version-1.0.1-6ee779)
![paper](https://img.shields.io/badge/Paper-26.1.2-57c2ff)
![depends](https://img.shields.io/badge/depends-attributelib-ffd54a)

🌐 **紹介サイト → https://pendiv.github.io/MOBenhance/**

---

## 特徴

### 3 本の柱

- **レベリング** — スポーン地点からの距離や次元に応じて、湧くモンスターに `Lv.1〜500` が付く。体力・攻撃力・防御・移動速度がレベルでスケールし、頭上に「Lv.X」が表示される。
- **特性 (Traits)** — レベルの高いモンスターは最大 4 つの特性を持つ。オーラ・牽引・反射などの常時効果から、ステータス倍率まで多種多様。**全 165 種**。
- **難易度 (危険度)** — その場の危険度は固定ではない。撃破数やネザー／エンドへの到達状況から算出され、場所ごとに変わる。撃破を重ねた者の周りほど危険になる。

### アイテム強化

戦って得た武器・防具を、自分の手で育てる。撃破や被弾で経験値がたまり、
`強化 Lv → 精錬 → 鍛造 → 鋳造 → 神格化` と段階的に強くなる。金床では耐久回復・付加スキルの付与・スキルの再抽選・育成もできる。

### レベリングスキル

武器・防具の種類ごとに固有のスキルが宿る（**全 46 種**）。config で 1 つずつ ON / OFF できる。

### カスタムディメンション「Ameijia」

専用バイオーム（荒野・深層）を持つ独自次元をデータパックとして同梱。
古代都市の中心に鎮座するゲートを、リカバリーコンパスで開いて到達する。

---

## 必要環境

- **Paper** サーバー（API `26.1.2` 系）
- 依存プラグイン **[attributelib](https://github.com/)**（必須）
  - カスタム属性・ダメージパイプラインの基盤。回復倍率・与/被ダメージ倍率・魔法ダメージがこれに依存する。**未導入では起動しません。**

## 導入

1. **attributelib** を `plugins/` に入れる
2. **EnhancedMobs** の jar を `plugins/` に入れる
3. サーバーを起動すると `config.yml` が生成される（カスタムダメージタイプの初回適用に再起動が 1 回必要）
4. 必要に応じて設定を編集し、再起動またはリロード

## コマンド

すべて `/emob`（別名 `/em`）に統合されている。

| コマンド | 内容 |
|---|---|
| `/emob difficulty` | 自分の難易度を確認・調整する |
| `/emob trait` | 特性を参照・操作する |
| `/emob skill` | 所持アイテムの強化 Lv・スキルを操作する |
| `/emob dimension` | カスタム次元のロードとテレポート |
| `/emob gamemode` | ゲームモードを変更する |
| `/emob debug` | デバッグ表示を切り替える |

## 設定

`config.yml` はサーバーの起動／リロード時に一度だけ読み込まれる。主な項目:

- `leveling` — レベリングの ON/OFF・レベル上限・距離係数 など
- `enhancement` — アイテム強化の ON/OFF・必要経験値倍率 など
- `traits` — 特性抽選の ON/OFF・最大数・ランク上限 など
- `skills` — レベリングスキルの個別 ON/OFF
- `default-difficulty` / `danger` — 基礎危険度・次元到達・個人の重力 など
- `mob-bonus` — ボス（エンダードラゴン・ウィザー・ウォーデン）のレベル補正
- `display` — 頭上の Lv 表示・強敵の発光

詳細は[紹介サイトの「設定」ページ](https://pendiv.github.io/MOBenhance/config.html)を参照。

## ビルド

```bash
./gradlew build      # ビルド（attributelib との composite build）
./gradlew runServer  # 検証用サーバーを起動（Paper 26.1.2）
```

`settings.gradle.kts` で `includeBuild("../attributelib")` しているため、
隣に `attributelib` リポジトリがクローンされている必要がある。
