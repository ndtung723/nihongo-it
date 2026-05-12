-- V2: Seed initial reference data

INSERT INTO roles (role_id, role_name)
VALUES (1, 'ROLE_ADMIN'), (2, 'ROLE_USER')
ON CONFLICT (role_id) DO NOTHING;

INSERT INTO categories (category_id, name, meaning, display_order, created_at, updated_at, is_active)
VALUES
  (gen_random_uuid(), 'IT基礎',              'Cơ bản về IT',                     1, NOW(), NOW(), true),
  (gen_random_uuid(), 'プログラミング',         'Lập trình (Programming)',           2, NOW(), NOW(), true),
  (gen_random_uuid(), 'ウェブ開発',            'Phát triển Web',                    3, NOW(), NOW(), true),
  (gen_random_uuid(), 'データベース',           'Cơ sở dữ liệu (Database)',          4, NOW(), NOW(), true),
  (gen_random_uuid(), '人工知能・データ',        'Trí tuệ nhân tạo / Dữ liệu',        5, NOW(), NOW(), true),
  (gen_random_uuid(), 'コミュニケーション',      'Giao tiếp & teamwork',              6, NOW(), NOW(), true),
  (gen_random_uuid(), '実務とキャリア',          'Dự án & thực tế',                   7, NOW(), NOW(), true)
ON CONFLICT (name) DO NOTHING;
