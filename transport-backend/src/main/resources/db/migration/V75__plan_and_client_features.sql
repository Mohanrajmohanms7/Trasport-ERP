-- V75: subscription / client feature access.
-- plan_features: what a plan includes. company_features: per-client overrides decided in Platform Admin.
-- Effective access = client override, else plan setting, else allowed (so existing clients keep everything).
CREATE TABLE IF NOT EXISTS plan_features (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES saas_plans(id),
    feature_code VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_by VARCHAR(100),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_plan_feature UNIQUE (plan_id, feature_code)
);
CREATE TABLE IF NOT EXISTS company_features (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    feature_code VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_by VARCHAR(100),
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_company_feature UNIQUE (company_id, feature_code)
);
CREATE INDEX IF NOT EXISTS idx_company_features_company ON company_features (company_id);
