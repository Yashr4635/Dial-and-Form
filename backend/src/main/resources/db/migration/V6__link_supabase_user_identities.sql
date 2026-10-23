-- Additive identity link only. Existing users and every FK-backed customer
-- record retain the same users.id. Existing accounts are linked at first
-- Supabase sign-in only after the provider confirms the same email address.
ALTER TABLE users ADD COLUMN supabase_user_id VARCHAR(36);

CREATE UNIQUE INDEX uq_users_supabase_user_id ON users(supabase_user_id);
