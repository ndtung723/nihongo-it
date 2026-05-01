-- Insert conversations if they don't exist
INSERT INTO conversations (conv_id, title, description, jlpt_level, unit, created_at, updated_at)
VALUES
  (gen_random_uuid(), '職場での自己紹介', 'Tự giới thiệu ở nơi làm việc', 'N4', 1, NOW(), NOW()),
  (gen_random_uuid(), 'チーム会議', 'Cuộc họp nhóm phát triển', 'N3', 2, NOW(), NOW()),
  (gen_random_uuid(), '技術面接', 'Phỏng vấn kỹ thuật', 'N2', 3, NOW(), NOW()),
  (gen_random_uuid(), 'プロジェクトの進捗報告', 'Báo cáo tiến độ dự án', 'N3', 4, NOW(), NOW()),
  (gen_random_uuid(), '顧客との打ち合わせ', 'Cuộc họp với khách hàng', 'N2', 5, NOW(), NOW()),
  (gen_random_uuid(), 'トラブルシューティング', 'Xử lý sự cố kỹ thuật', 'N2', 6, NOW(), NOW())
ON CONFLICT (conv_id) DO NOTHING;

-- Insert conversation lines for "職場での自己紹介" (Self-introduction at workplace)
WITH conv AS (SELECT conv_id FROM conversations WHERE title = '職場での自己紹介' LIMIT 1)
INSERT INTO conversation_lines (line_id, conversation_id, speaker, japanese_text, vietnamese_translation, notes, important_vocab, order_index)
VALUES
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'みなさん、こちらは新しいITエンジニアの田中さんです。今日から我々のチームに加わります。', 'Các bạn, đây là Tanaka, kỹ sư IT mới. Từ hôm nay sẽ tham gia vào đội của chúng ta.', NULL, 'チーム (team)', 1),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', 'はじめまして、田中と申します。ベトナムから来ました。フロントエンド開発を担当します。どうぞよろしくお願いします。', 'Xin chào, tôi là Tanaka. Tôi đến từ Việt Nam. Tôi phụ trách phát triển frontend. Rất mong được làm việc cùng mọi người.', NULL, 'フロントエンド開発 (frontend development), 担当する (đảm nhận)', 2),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'はじめまして、佐藤です。バックエンド担当です。一緒に働けることを楽しみにしています。', 'Xin chào, tôi là Sato. Tôi phụ trách backend. Rất mong được làm việc cùng bạn.', NULL, 'バックエンド (backend), 一緒に働く (làm việc cùng nhau)', 3),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', '鈴木です。プロジェクトマネージャーをしています。何か困ったことがあれば、いつでも声をかけてください。', 'Tôi là Suzuki. Tôi là quản lý dự án. Nếu có vấn đề gì khó khăn, hãy gọi cho tôi bất cứ lúc nào.', NULL, 'プロジェクトマネージャー (project manager), 困ったこと (vấn đề khó khăn)', 4),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'ありがとうございます。頑張ります。', 'Cảm ơn mọi người. Tôi sẽ cố gắng.', NULL, '頑張る (cố gắng)', 5)
ON CONFLICT DO NOTHING;

-- Insert conversation lines for "チーム会議" (Team meeting)
WITH conv AS (SELECT conv_id FROM conversations WHERE title = 'チーム会議' LIMIT 1)
INSERT INTO conversation_lines (line_id, conversation_id, speaker, japanese_text, vietnamese_translation, notes, important_vocab, order_index)
VALUES
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', '今日は新機能の実装について話し合いましょう。まず、現在の進捗状況を確認したいと思います。', 'Hôm nay chúng ta sẽ thảo luận về việc triển khai tính năng mới. Trước tiên, tôi muốn kiểm tra tình trạng tiến độ hiện tại.', NULL, '新機能 (tính năng mới), 実装 (triển khai), 進捗状況 (tình trạng tiến độ)', 1),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', 'UIデザインは80%完了しています。あと数日で終わる予定です。', 'Thiết kế UI đã hoàn thành 80%. Dự kiến sẽ hoàn thành trong vài ngày tới.', NULL, 'UIデザイン (thiết kế UI), 完了 (hoàn thành), 予定 (dự kiến)', 2),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'APIの開発は順調に進んでいます。ただ、データベースの最適化にもう少し時間がかかりそうです。', 'Việc phát triển API đang tiến triển tốt. Tuy nhiên, có vẻ như việc tối ưu hóa cơ sở dữ liệu sẽ cần thêm một chút thời gian.', NULL, 'API開発 (phát triển API), 順調 (suôn sẻ), データベースの最適化 (tối ưu hóa cơ sở dữ liệu)', 3),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', 'テスト計画は準備できています。フロントエンドとバックエンドが連携できたら、すぐにテストを開始できます。', 'Kế hoạch kiểm thử đã sẵn sàng. Ngay khi frontend và backend có thể liên kết được với nhau, chúng tôi có thể bắt đầu kiểm thử ngay lập tức.', NULL, 'テスト計画 (kế hoạch kiểm thử), 準備 (chuẩn bị), 連携 (liên kết)', 4),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'ありがとうございます。では、次のスプリントでは、フロントエンドとバックエンドの統合を優先しましょう。質問はありますか？', 'Cảm ơn các bạn. Vậy, trong sprint tiếp theo, chúng ta sẽ ưu tiên tích hợp frontend và backend. Có câu hỏi nào không?', NULL, 'スプリント (sprint), 統合 (tích hợp), 優先 (ưu tiên)', 5)
ON CONFLICT DO NOTHING;

-- Insert conversation lines for "技術面接" (Technical interview)
WITH conv AS (SELECT conv_id FROM conversations WHERE title = '技術面接' LIMIT 1)
INSERT INTO conversation_lines (line_id, conversation_id, speaker, japanese_text, vietnamese_translation, notes, important_vocab, order_index)
VALUES
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'それでは、技術的な質問をいくつかさせていただきます。オブジェクト指向プログラミングの基本原則について説明してください。', 'Vậy, tôi sẽ hỏi một vài câu hỏi kỹ thuật. Hãy giải thích về các nguyên tắc cơ bản của lập trình hướng đối tượng.', NULL, 'オブジェクト指向プログラミング (lập trình hướng đối tượng), 基本原則 (nguyên tắc cơ bản)', 1),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', 'オブジェクト指向プログラミングの基本原則は、カプセル化、継承、ポリモーフィズム、抽象化です。カプセル化はデータと操作をまとめ、内部詳細を隠すことです。', 'Các nguyên tắc cơ bản của lập trình hướng đối tượng là đóng gói, kế thừa, đa hình và trừu tượng hóa. Đóng gói là việc gom dữ liệu và thao tác lại với nhau, và ẩn các chi tiết bên trong.', NULL, 'カプセル化 (đóng gói), 継承 (kế thừa), ポリモーフィズム (đa hình), 抽象化 (trừu tượng hóa)', 2),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', '具体的なプロジェクトでどのように適用したか例を挙げてください。', 'Hãy đưa ra ví dụ về cách bạn đã áp dụng những nguyên tắc này trong một dự án cụ thể.', NULL, '具体的 (cụ thể), 適用 (áp dụng), 例を挙げる (đưa ra ví dụ)', 3),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', '前のプロジェクトでは、ユーザー認証システムを開発しました。ユーザークラスをカプセル化し、異なる種類のユーザー（一般ユーザー、管理者）のために基本クラスを継承しました。', 'Trong dự án trước, tôi đã phát triển hệ thống xác thực người dùng. Tôi đã đóng gói lớp User và kế thừa lớp cơ bản cho các loại người dùng khác nhau (người dùng thông thường, quản trị viên).', NULL, 'ユーザー認証システム (hệ thống xác thực người dùng), ユーザークラス (lớp User), 管理者 (quản trị viên)', 4),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'なるほど。では、RESTful APIについての理解を教えてください。', 'Tôi hiểu rồi. Vậy hãy cho tôi biết hiểu biết của bạn về RESTful API.', NULL, 'RESTful API (RESTful API), 理解 (hiểu biết)', 5)
ON CONFLICT DO NOTHING;

-- Insert conversation lines for "プロジェクトの進捗報告" (Project progress report)
WITH conv AS (SELECT conv_id FROM conversations WHERE title = 'プロジェクトの進捗報告' LIMIT 1)
INSERT INTO conversation_lines (line_id, conversation_id, speaker, japanese_text, vietnamese_translation, notes, important_vocab, order_index)
VALUES
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', '今週のプロジェクト進捗を報告します。予定通り、ユーザー管理機能の実装が完了しました。', 'Tôi xin báo cáo tiến độ dự án tuần này. Như đã dự kiến, việc triển khai tính năng quản lý người dùng đã hoàn thành.', NULL, 'プロジェクト進捗 (tiến độ dự án), ユーザー管理機能 (tính năng quản lý người dùng)', 1),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', 'テスト結果はどうでしたか？何か問題点はありましたか？', 'Kết quả kiểm thử thế nào? Có vấn đề gì không?', NULL, 'テスト結果 (kết quả kiểm thử), 問題点 (vấn đề)', 2),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'ユニットテストは全て通過しましたが、統合テストで一部のAPIがタイムアウトする問題が見つかりました。現在、原因を調査中です。', 'Tất cả unit test đều đã vượt qua, nhưng trong quá trình integration test, chúng tôi đã phát hiện vấn đề một số API bị timeout. Hiện tại, chúng tôi đang điều tra nguyên nhân.', NULL, 'ユニットテスト (unit test), 統合テスト (integration test), タイムアウト (timeout), 原因 (nguyên nhân), 調査 (điều tra)', 3),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', 'データベースのクエリ最適化が必要かもしれません。明日までに修正案を提出します。', 'Có thể cần tối ưu hóa truy vấn cơ sở dữ liệu. Tôi sẽ đệ trình đề xuất sửa chữa trước ngày mai.', NULL, 'データベースのクエリ最適化 (tối ưu hóa truy vấn cơ sở dữ liệu), 修正案 (đề xuất sửa chữa), 提出 (đệ trình)', 4),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'ありがとう。次に、来週の計画ですが、決済機能の開発を開始する予定です。必要なAPIドキュメントは既に共有されています。', 'Cảm ơn. Tiếp theo, kế hoạch cho tuần tới, chúng ta dự định sẽ bắt đầu phát triển tính năng thanh toán. Tài liệu API cần thiết đã được chia sẻ.', NULL, '決済機能 (tính năng thanh toán), APIドキュメント (tài liệu API), 共有 (chia sẻ)', 5)
ON CONFLICT DO NOTHING;

-- Inserfurit conversation lines for "顧客との打ち合わせ" (Meeting with client)
WITH conv AS (SELECT conv_id FROM conversations WHERE title = '顧客との打ち合わせ' LIMIT 1)
INSERT INTO conversation_lines (line_id, conversation_id, speaker, japanese_text, vietnamese_translation, notes, important_vocab, order_index)
VALUES
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'お忙しいところお時間いただき、ありがとうございます。本日は新システムの要件について詳しくお伺いしたいと思います。', 'Cảm ơn bạn đã dành thời gian trong lúc bận rộn. Hôm nay, tôi muốn tìm hiểu chi tiết về các yêu cầu của hệ thống mới.', NULL, '営業担当 (nhân viên kinh doanh), 要件 (yêu cầu), お伺いする (hỏi, tìm hiểu)', 1),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', '私たちは顧客管理システムのアップデートを検討しています。現在のシステムは古く、モバイル対応していません。', 'Chúng tôi đang xem xét việc cập nhật hệ thống quản lý khách hàng. Hệ thống hiện tại đã cũ và không tương thích với thiết bị di động.', NULL, '顧客管理システム (hệ thống quản lý khách hàng), アップデート (cập nhật), モバイル対応 (tương thích với thiết bị di động)', 2),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', '具体的にどのような機能が必要でしょうか？また、セキュリティ要件についてもお聞かせください。', 'Cụ thể là bạn cần những tính năng nào? Và xin vui lòng cho tôi biết về các yêu cầu bảo mật.', NULL, '具体的 (cụ thể), セキュリティ要件 (yêu cầu bảo mật)', 3),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', '顧客データの検索・編集機能、売上レポートの生成、そして多層認証が必要です。また、APIを通じて他のシステムと連携できるようにしたいです。', 'Chúng tôi cần các chức năng tìm kiếm và chỉnh sửa dữ liệu khách hàng, tạo báo cáo doanh số, và xác thực nhiều lớp. Ngoài ra, chúng tôi muốn có thể liên kết với các hệ thống khác thông qua API.', NULL, '顧客データ (dữ liệu khách hàng), 売上レポート (báo cáo doanh số), 多層認証 (xác thực nhiều lớp), 連携 (liên kết)', 4),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'ありがとうございます。これらの要件を基に、来週までに提案書と見積もりを作成いたします。', 'Cảm ơn bạn. Dựa trên những yêu cầu này, chúng tôi sẽ tạo đề xuất và báo giá trước tuần sau.', NULL, '提案書 (đề xuất), 見積もり (báo giá), 作成 (tạo)', 5)
ON CONFLICT DO NOTHING;

-- Insert conversation lines for "トラブルシューティング" (Troubleshooting)
WITH conv AS (SELECT conv_id FROM conversations WHERE title = 'トラブルシューティング' LIMIT 1)
INSERT INTO conversation_lines (line_id, conversation_id, speaker, japanese_text, vietnamese_translation, notes, important_vocab, order_index)
VALUES
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', 'アプリケーションがクラッシュする問題が発生しています。エラーログを確認したところ、メモリ不足のエラーが記録されていました。', 'Chúng tôi đang gặp vấn đề ứng dụng bị crash. Khi kiểm tra nhật ký lỗi, tôi thấy có ghi nhận lỗi thiếu bộ nhớ.', NULL, 'クラッシュ (crash), エラーログ (nhật ký lỗi), メモリ不足 (thiếu bộ nhớ)', 1),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', '具体的にどのような操作をしたときに発生しますか？また、どのくらいの頻度で起きていますか？', 'Cụ thể là nó xảy ra khi bạn thực hiện thao tác nào? Và nó xảy ra thường xuyên như thế nào?', NULL, '操作 (thao tác), 頻度 (tần suất)', 2),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', '大量のデータをインポートしようとすると必ず発生します。最近、データ量が増えてから頻繁に起きるようになりました。', 'Nó luôn xảy ra khi tôi cố gắng nhập một lượng lớn dữ liệu. Gần đây, nó bắt đầu xảy ra thường xuyên hơn kể từ khi lượng dữ liệu tăng lên.', NULL, '大量 (lượng lớn), データ (dữ liệu), インポート (nhập), 頻繁 (thường xuyên)', 3),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'user', 'バッチ処理の最適化が必要かもしれません。一時的な解決策として、データを小さなバッチに分けてインポートすることをお勧めします。', 'Có thể cần tối ưu hóa xử lý hàng loạt. Như một giải pháp tạm thời, tôi đề xuất bạn chia dữ liệu thành các lô nhỏ để nhập.', NULL, 'バッチ処理 (xử lý hàng loạt), 最適化 (tối ưu hóa), 一時的な解決策 (giải pháp tạm thời)', 4),
  (gen_random_uuid(), (SELECT conv_id FROM conv), 'bot', '今週中にパフォーマンス改善のアップデートをリリースする予定です。それまでの間、メモリ使用量を監視してください。', 'Chúng tôi dự định phát hành bản cập nhật cải thiện hiệu suất trong tuần này. Trong thời gian chờ đợi, vui lòng theo dõi mức sử dụng bộ nhớ.', NULL, 'パフォーマンス改善 (cải thiện hiệu suất), アップデート (cập nhật), リリース (phát hành), メモリ使用量 (mức sử dụng bộ nhớ), 監視 (theo dõi)', 5)
ON CONFLICT DO NOTHING;
