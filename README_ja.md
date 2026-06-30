# Chairs

**[English README](README.md)**

Spigot / Paper サーバーで、プレイヤーが階段や設定ブロックに座れるようにするプラグインです。

## 概要
Chairs は、右クリック着席とコマンド着席を提供する軽量な着席プラグインです。
1.15.2 から 26.1.2 までの互換性を重視し、既存ユーザー体験を崩さない運用を目的としています。

### 対象ユーザー
- サバイバル / ロビー / RPG 系サーバーで没入感を高めたい管理者
- 設定駆動で椅子挙動を運用したいサーバー運営者

## 特徴
- 右クリック着席 + コマンド着席の両対応
- `/chair` で自分を着席、`/chair <player>` で対象プレイヤーを強制着席（権限必須）
- `/chairs block disable|enable <MATERIAL|PATTERN>` による実行時ブロック制御
- `*_STAIRS` などのパターン指定に対応
- ワールド単位での着席無効化
- 着席中の制限・効果（コマンド制限 / 回復 / アイテム回収）
- スタックトレース付き詳細ログで障害解析を強化

## 必要環境
- Java 8 以上（プラグイン実行ターゲット）
- Spigot または Paper 1.15.2 以上
- Gradle 8.14.3（ビルド）

**動作確認済みバージョン（実サーバー起動）:**

| Minecraft | サーバー | JDK |
|---|---|---|
| 1.15.2 | Paper 1.15.2 (build 393) | JDK 11.0.31 |
| 1.16.5 | Paper 1.16.5 (build 794) | JDK 11.0.31 |
| 1.21.11 | Paper 1.21.11 (build 69) | JDK 21.0.11 |
| 26.1.2 | Paper 26.1.2 (build 72) | JDK 25.0.3 |

コード互換目標: 1.15.2 - 26.1.2

## ステータス
- README 最終更新日: 2026-06-30
- 安定運用方針: 異常時でも処理継続し、根本原因をログに残す
- 配布注記: 本リリースは Cyrne1_7208 バージョンとして公開

## インストール
1. プラグイン jar をビルドします。
2. 生成された jar をサーバーの `plugins/` に配置します。
3. サーバーを再起動します。

```bash
./gradlew.bat clean build
```

生成物: `target/Chairs.jar`

## クイックスタート
1. `target/Chairs.jar` を `plugins/` に配置します。
2. サーバーを起動（または再起動）します。
3. ログに `[Chairs] Enabling Chairs v1.2.0` が出ることを確認します。
4. 階段を右クリック、または `/chair` を実行して着席を確認します。

期待結果:

```text
サーバーログで Chairs の有効化を確認でき、プレイヤーが正常に着席できる。
```

## 使い方

### コマンド一覧
| コマンド | 権限 | 説明 |
|---|---|---|
| `/chairs reload` | `chairs.reload` | 設定を再読み込み |
| `/chairs on` | `chairs.sit` | 自分の着席機能を有効化 |
| `/chairs off` | `chairs.sit` | 自分の着席機能を無効化 |
| `/chairs block list` | `chairs.block.edit` | 無効化中ブロック ID/パターン一覧 |
| `/chairs block disable <MATERIAL\|PATTERN>` | `chairs.block.edit` | ブロック ID/パターンを無効化 |
| `/chairs block enable <MATERIAL\|PATTERN>` | `chairs.block.edit` | ブロック ID/パターンを有効化 |
| `/chair` | `chairs.sit` | 自分を着席 |
| `/chair <player>` | `chairs.sit.force` | 対象プレイヤーを強制着席 |

パターン指定例:

```text
/chairs block disable WARPED_STAIRS
/chairs block disable *_STAIRS
/chairs block enable *_STAIRS
```

## 設定
`config.yml` の主要項目:

| セクション | キー | 概要 |
|---|---|---|
| `sit-config` | `disabled-worlds` | 着席無効ワールド |
| `sit-config` | `max-distance` | 着席可能距離 |
| `sit-config.stairs` | `enabled` | 階段着席の有効/無効 |
| `sit-config.additional-blocks` | `MATERIAL` または `*_PATTERN : 数値` | 追加着席ブロックと高さ |
| `sit-config.disabled-blocks` | `MATERIAL` / `*_PATTERN` | 着席対象から除外 |
| `sit-effects.healing` | `enabled` など | 着席中回復設定 |
| `sit-effects.itempickup` | `enabled` | 着席中アイテム回収 |
| `sit-restrictions.commands` | `all`, `list` | 着席中のコマンド制限 |

## 安定性メモ
本バージョンでは以下を強化しています。
- コマンド実行経路での例外捕捉
- 着席 / 再着席 / 降車フローでの例外ログ化
- 効果タスクでのプレイヤー単位例外分離
- 不正なブロック ID / パターン設定の読み込み時バリデーション

代表ログタグ:

```text
[CMD]
[SIT-START]
[SIT-RESIT]
[SIT-UNSIT]
[CFG-SAVE]
[EFFECT-HEAL]
[EFFECT-PICKUP]
```

## テスト
Paper 1.15.2 / 1.16.5 / 1.21.11 / 26.1.2 の実サーバーで手動確認済みです。
各環境で `Enabling Chairs` ログ、コマンド実行、正常起動を確認しています。

## トラブルシューティング
- `InvalidDescriptionException: commands are of wrong type`
  - `plugin.yml` の `commands` 配下インデントを確認してください。
- `Minecraft 1.19 requires Java 17 or above`
  - 1.21.x 系の起動には Java 17 以上を使用してください。
- `Invalid or corrupt jarfile paper.jar`
  - 公式配布元から `paper.jar` を再取得してください。

## 謝辞
- Original contributors: spoothie, cnaude, _Shevchik_, Pugabyte

## 生成支援ツール利用
- 使用した AI: GPT-5.3-Codex, GLM-4.6, MiMo-V2.5
- 利用範囲: コードレビュー、バグ修正
- 人手レビュー: コマンド・設定・起動ログをソースと照合

## コントリビュート
Issue / Pull Request を歓迎します。
挙動変更を含む PR では README と設定説明の同時更新をお願いします。

## ライセンス
GPL-3.0。詳細は `LICENSE` を参照してください。
