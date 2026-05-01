-- Insert default roles if they don't exist
INSERT INTO roles (role_id, role_name)
VALUES (1, 'ROLE_ADMIN')
ON CONFLICT (role_id) DO NOTHING;

INSERT INTO roles (role_id, role_name)
VALUES (2, 'ROLE_USER')
ON CONFLICT (role_id) DO NOTHING;

-- Insert categories if they don't exist
INSERT INTO categories (category_id, name, meaning, display_order, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'IT基礎', 'Cơ bản về IT', 1, NOW(), NOW(), true),
  (gen_random_uuid(), 'プログラミング', 'Lập trình (Programming)', 2, NOW(), NOW(), true),
  (gen_random_uuid(), 'ウェブ開発', 'Phát triển Web', 3, NOW(), NOW(), true),
  (gen_random_uuid(), 'データベース', 'Cơ sở dữ liệu (Database)', 4, NOW(), NOW(), true),
  (gen_random_uuid(), '人工知能・データ', 'Trí tuệ nhân tạo / Dữ liệu', 5, NOW(), NOW(), true),
  (gen_random_uuid(), 'コミュニケーション', 'Giao tiếp & teamwork', 6, NOW(), NOW(), true),
  (gen_random_uuid(), '実務とキャリア', 'Dự án & thực tế', 7, NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;

-- Insert topics if they don't exist (IT Basics)
WITH category AS (SELECT category_id FROM categories WHERE name = 'IT基礎')
INSERT INTO topics (topic_id, name, meaning, display_order, category_id, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'コンピューターの構造', 'Kiến trúc máy tính', 1, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'オペレーティングシステム', 'Hệ điều hành', 2, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'ハードウェア', 'Phần cứng', 3, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'ソフトウェア', 'Phần mềm', 4, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'コンピューターネットワーク', 'Mạng máy tính', 5, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'IT共通用語', 'Thuật ngữ chung trong IT', 6, (SELECT category_id FROM category), NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;

-- Insert topics if they don't exist (Programming)
WITH category AS (SELECT category_id FROM categories WHERE name = 'プログラミング')
INSERT INTO topics (topic_id, name, meaning, display_order, category_id, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'プログラミング言語', 'Ngôn ngữ lập trình', 1, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'アルゴリズムとデータ構造', 'Thuật toán & cấu trúc dữ liệu', 2, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'デバッグとエラーハンドリング', 'Gỡ lỗi và xử lý lỗi', 3, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'オブジェクト指向', 'Hướng đối tượng', 4, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'コマンドライン・ターミナル', 'Command line & terminal', 5, (SELECT category_id FROM category), NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;

-- Insert topics if they don't exist (Web Development)
WITH category AS (SELECT category_id FROM categories WHERE name = 'ウェブ開発')
INSERT INTO topics (topic_id, name, meaning, display_order, category_id, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'フロントエンド', 'Frontend', 1, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'バックエンド', 'Backend', 2, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'システムアーキテクチャ', 'Kiến trúc hệ thống', 3, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'DevOps・CI/CD', 'DevOps, CI/CD', 4, (SELECT category_id FROM category), NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;

-- Insert topics if they don't exist (Database)
WITH category AS (SELECT category_id FROM categories WHERE name = 'データベース')
INSERT INTO topics (topic_id, name, meaning, display_order, category_id, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'SQLの基本', 'SQL cơ bản', 1, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'データベース設計', 'Thiết kế cơ sở dữ liệu', 2, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'NoSQL・新しい概念', 'NoSQL và các khái niệm mới', 3, (SELECT category_id FROM category), NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;

-- Insert topics if they don't exist (AI/Data)
WITH category AS (SELECT category_id FROM categories WHERE name = '人工知能・データ')
INSERT INTO topics (topic_id, name, meaning, display_order, category_id, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), '機械学習', 'Học máy', 1, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), '人工知能とディープラーニング', 'AI & Deep Learning', 2, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), '自然言語処理', 'Xử lý ngôn ngữ tự nhiên', 3, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'データサイエンス', 'Khoa học dữ liệu', 4, (SELECT category_id FROM category), NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;  

-- Insert topics if they don't exist (Communication)
WITH category AS (SELECT category_id FROM categories WHERE name = 'コミュニケーション')
INSERT INTO topics (topic_id, name, meaning, display_order, category_id, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'プロジェクトの会話用語', 'Thuật ngữ giao tiếp trong dự án', 1, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'アジャイル・スクラム開発', 'Quy trình làm việc agile/scrum', 2, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), 'ドキュメント・コメント', 'Đọc hiểu tài liệu, comment code', 3, (SELECT category_id FROM category), NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;

-- Insert topics if they don't exist (Work & Career)
WITH category AS (SELECT category_id FROM categories WHERE name = '実務とキャリア')
INSERT INTO topics (topic_id, name, meaning, display_order, category_id, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'IT面接', 'Phỏng vấn xin việc IT', 1, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), '自己紹介・履歴書', 'CV, giới thiệu bản thân', 2, (SELECT category_id FROM category), NOW(), NOW(), true),
  (gen_random_uuid(), '実務で使う単語', 'Từ vựng thực hành/thường dùng khi làm việc', 3, (SELECT category_id FROM category), NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;

-- Insert sample vocabulary items for programming topic
WITH topic AS (SELECT topic_id FROM topics WHERE name = 'プログラミング言語')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), '変数', 'Biến', 'へんすう', '変数を宣言する', 'Khai báo biến', 'N4', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '関数', 'Hàm', 'かんすう', '関数を呼び出す', 'Gọi hàm', 'N3', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;

-- Insert sample vocabulary items for web development topic
WITH topic AS (SELECT topic_id FROM topics WHERE name = 'フロントエンド')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), 'ウェブサイト', 'Trang web', NULL, 'ウェブサイトを構築する', 'Xây dựng trang web', 'N5', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'レスポンシブデザイン', 'Thiết kế đáp ứng', NULL, 'レスポンシブデザインを実装する', 'Triển khai thiết kế đáp ứng', 'N2', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;


-- Additional vocabulary entries for IT Japanese learning
-- This file adds 30 new vocabulary items across different topics

-- Programming Languages vocabulary
WITH topic AS (SELECT topic_id FROM topics WHERE name = 'プログラミング言語')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), 'コンパイル', 'Biên dịch', 'こんぱいる', 'ソースコードをコンパイルする', 'Biên dịch mã nguồn', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '実行', 'Thực thi', 'じっこう', 'プログラムを実行する', 'Chạy chương trình', 'N3', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'デバッグ', 'Gỡ lỗi', 'でばっぐ', 'コードをデバッグする', 'Gỡ lỗi code', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '配列', 'Mảng', 'はいれつ', '配列を初期化する', 'Khởi tạo mảng', 'N3', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'オブジェクト', 'Đối tượng', 'おぶじぇくと', 'オブジェクトを生成する', 'Tạo đối tượng', 'N3', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;

-- Algorithm and Data Structure vocabulary
WITH topic AS (SELECT topic_id FROM topics WHERE name = 'アルゴリズムとデータ構造')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), 'ソート', 'Sắp xếp', 'そーと', 'データをソートする', 'Sắp xếp dữ liệu', 'N3', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '二分探索', 'Tìm kiếm nhị phân', 'にぶんたんさく', '二分探索を実装する', 'Triển khai tìm kiếm nhị phân', 'N1', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'リンクリスト', 'Danh sách liên kết', 'りんくりすと', 'リンクリストを操作する', 'Thao tác với danh sách liên kết', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '再帰', 'Đệ quy', 'さいき', '再帰関数を書く', 'Viết hàm đệ quy', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '計算量', 'Độ phức tạp tính toán', 'けいさんりょう', 'アルゴリズムの計算量を分析する', 'Phân tích độ phức tạp của thuật toán', 'N1', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;

-- Frontend vocabulary
WITH topic AS (SELECT topic_id FROM topics WHERE name = 'フロントエンド')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), 'ユーザーインターフェース', 'Giao diện người dùng', 'ゆーざーいんたーふぇーす', 'ユーザーインターフェースを設計する', 'Thiết kế giao diện người dùng', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'レンダリング', 'Hiển thị', 'れんだりんぐ', 'コンポーネントをレンダリングする', 'Hiển thị component', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'イベントリスナー', 'Lắng nghe sự kiện', 'いべんとりすなー', 'イベントリスナーを追加する', 'Thêm trình lắng nghe sự kiện', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'スタイルシート', 'Bảng định kiểu', 'すたいるしーと', 'スタイルシートを適用する', 'Áp dụng bảng định kiểu', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '単一ページアプリケーション', 'Ứng dụng một trang', 'たんいつぺーじあぷりけーしょん', '単一ページアプリケーションを開発する', 'Phát triển ứng dụng một trang', 'N1', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;

-- Backend vocabulary
WITH topic AS (SELECT topic_id FROM topics WHERE name = 'バックエンド')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), 'サーバー', 'Máy chủ', 'さーばー', 'サーバーを構築する', 'Xây dựng máy chủ', 'N3', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'エンドポイント', 'Điểm cuối', 'えんどぽいんと', 'APIエンドポイントを作成する', 'Tạo điểm cuối API', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '認証', 'Xác thực', 'にんしょう', 'ユーザー認証を実装する', 'Triển khai xác thực người dùng', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'マイクロサービス', 'Vi dịch vụ', 'まいくろさーびす', 'マイクロサービスアーキテクチャを採用する', 'Áp dụng kiến trúc vi dịch vụ', 'N1', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'ミドルウェア', 'Phần mềm trung gian', 'みどるうぇあ', 'ミドルウェアを追加する', 'Thêm phần mềm trung gian', 'N2', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;

-- Database vocabulary
WITH topic AS (SELECT topic_id FROM topics WHERE name = 'SQLの基本')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), 'クエリ', 'Truy vấn', 'くえり', 'SQLクエリを実行する', 'Thực thi truy vấn SQL', 'N3', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '結合', 'Kết hợp', 'けつごう', 'テーブルを結合する', 'Kết hợp các bảng', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'インデックス', 'Chỉ mục', 'いんでっくす', 'インデックスを作成する', 'Tạo chỉ mục', 'N2', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'トランザクション', 'Giao dịch', 'とらんざくしょん', 'トランザクションを開始する', 'Bắt đầu giao dịch', 'N1', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '正規化', 'Chuẩn hóa', 'せいきか', 'データベースを正規化する', 'Chuẩn hóa cơ sở dữ liệu', 'N1', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;

-- AI and Machine Learning vocabulary
WITH topic AS (SELECT topic_id FROM topics WHERE name = '機械学習')
INSERT INTO vocabulary (vocab_id, term, meaning, pronunciation, example, example_meaning, jlpt_level, topic_id, created_at)
VALUES
  (gen_random_uuid(), '学習モデル', 'Mô hình học tập', 'がくしゅうもでる', '学習モデルを訓練する', 'Huấn luyện mô hình học tập', 'N1', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '予測', 'Dự đoán', 'よそく', 'データを予測する', 'Dự đoán dữ liệu', 'N3', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '特徴抽出', 'Trích xuất đặc trưng', 'とくちょうちゅうしゅつ', '特徴抽出を行う', 'Thực hiện trích xuất đặc trưng', 'N1', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), '過学習', 'Quá khớp', 'かがくしゅう', '過学習を防ぐ', 'Ngăn chặn quá khớp', 'N1', (SELECT topic_id FROM topic), NOW()),
  (gen_random_uuid(), 'ニューラルネットワーク', 'Mạng nơ-ron', 'にゅーらるねっとわーく', 'ニューラルネットワークを構築する', 'Xây dựng mạng nơ-ron', 'N1', (SELECT topic_id FROM topic), NOW())
ON CONFLICT DO NOTHING;

-- Add is_active column to topics table if it doesn't exist
ALTER TABLE topics ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

