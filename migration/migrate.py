#!/usr/bin/env python3
import argparse
import json
import os
from datetime import datetime, timezone

import psycopg2
from psycopg2 import sql
from psycopg2.extras import execute_batch

DEFAULT_PASSWORD_HASH = "$2b$12$XLYH1iXVxLR/xRHTBVa3QeLIp0W2vnidFGgO.z7vLuSTRs47fJdZm"


def parse_args():
    parser = argparse.ArgumentParser(
        description=(
            "Migrate clients.json, fabric-clients.json, forge-clients.json, analytics.json, users.json, "
            "user_data.json, social_links.json, and friendships.json into Postgres."
        )
    )
    parser.add_argument(
        "--json",
        default="migration/clients.json",
        help="Path to clients.json (default: migration/clients.json)",
    )
    parser.add_argument(
        "--fabric-json",
        default="migration/fabric-clients.json",
        help="Path to fabric-clients.json (default: migration/fabric-clients.json)",
    )
    parser.add_argument(
        "--forge-json",
        default="migration/forge-clients.json",
        help="Path to forge-clients.json (default: migration/forge-clients.json)",
    )
    parser.add_argument(
        "--analytics-json",
        default="migration/analytics.json",
        help="Path to analytics.json (default: migration/analytics.json)",
    )
    parser.add_argument(
        "--users-json",
        default="migration/users.json",
        help="Path to users.json (default: migration/users.json)",
    )
    parser.add_argument(
        "--user-data-json",
        default="migration/user_data.json",
        help="Path to user_data.json (default: migration/user_data.json)",
    )
    parser.add_argument(
        "--social-links-json",
        default="migration/social_links.json",
        help="Path to social_links.json (default: migration/social_links.json)",
    )
    parser.add_argument(
        "--friendships-json",
        default="migration/friendships.json",
        help="Path to friendships.json (default: migration/friendships.json)",
    )
    parser.add_argument(
        "--clients",
        action="store_true",
        help="Only migrate clients.json",
    )
    parser.add_argument(
        "--fabric",
        action="store_true",
        help="Only migrate fabric-clients.json",
    )
    parser.add_argument(
        "--forge",
        action="store_true",
        help="Only migrate forge-clients.json",
    )
    parser.add_argument(
        "--analytics",
        action="store_true",
        help="Only migrate analytics.json",
    )
    parser.add_argument(
        "--users",
        action="store_true",
        help="Only migrate users.json",
    )
    parser.add_argument(
        "--user-profiles",
        action="store_true",
        help="Only migrate user_data.json",
    )
    parser.add_argument(
        "--social-links",
        action="store_true",
        help="Only migrate social_links.json",
    )
    parser.add_argument(
        "--friendship",
        "--friendships",
        action="store_true",
        help="Only migrate friendships.json",
    )
    parser.add_argument(
        "--skip-sequence-reset",
        action="store_true",
        help="Skip resetting the clients.id sequence after insert.",
    )
    return parser.parse_args()


def get_db_config():
    load_dotenv()
    database_url = os.getenv("DATABASE_URL")
    if database_url:
        return {"dsn": database_url}

    return {
        "host": os.getenv("POSTGRES_HOST", "localhost"),
        "port": int(os.getenv("POSTGRES_PORT", "5433")),
        "dbname": os.getenv("POSTGRES_DB", "atlas"),
        "user": os.getenv("POSTGRES_USER", "atlas_user"),
        "password": os.getenv("POSTGRES_PASSWORD", "atlas_password"),
    }


def load_dotenv():
    candidates = [
        os.path.join(os.getcwd(), ".env"),
        os.path.join(os.path.dirname(__file__), "..", ".env"),
    ]
    for path in candidates:
        if os.path.exists(path):
            _load_env_file(path)
            break


def _load_env_file(path):
    with open(path, "r", encoding="utf-8") as handle:
        for raw_line in handle:
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip("'\"")
            if key and key not in os.environ:
                os.environ[key] = value


VERSION_CANONICAL = [
    "V_1_21_11",
    "V_1_21_8",
    "V_1_21_1",
    "V_1_21_4",
    "V_1_16_5",
    "V_1_12_2",
    "V_1_8_9",
]


def normalize_version(value):
    if not value:
        raise ValueError("Missing version value.")
    raw = value.strip()
    raw_lower = raw.lower()
    candidate_lower = raw.replace(".", "_").replace("-", "_").lower()
    for canonical in VERSION_CANONICAL:
        canonical_lower = canonical.lower()
        if raw_lower == canonical_lower:
            return canonical
        if candidate_lower == canonical_lower:
            return canonical
        base_lower = (
            canonical_lower[1:] if canonical_lower.startswith("v") else canonical_lower
        )
        base_stripped = base_lower.lstrip("_")
        if candidate_lower == base_lower or candidate_lower == base_stripped:
            return canonical
        if (
            candidate_lower == f"v{base_lower}"
            or candidate_lower == f"v{base_stripped}"
        ):
            return canonical
        if raw_lower == base_lower.replace(
            "_", "."
        ) or raw_lower == base_stripped.replace("_", "."):
            return canonical
    raise ValueError(f"Unsupported version value: {value}")


def normalize_type(value):
    if not value:
        raise ValueError("Missing client_type value.")
    lowered = value.strip().lower()
    if lowered == "default":
        return "Vanilla"
    if lowered == "forge":
        return "FORGE"
    if lowered == "fabric":
        return "FABRIC"
    raise ValueError(f"Unsupported client_type value: {value}")


def parse_timestamp(value):
    if not value:
        return None
    if value.endswith("Z"):
        value = value.replace("Z", "+00:00")
    return datetime.fromisoformat(value)


def load_clients(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as handle:
        return json.load(handle)


def load_analytics(path):
    if not path or not os.path.exists(path):
        return {}
    with open(path, "r", encoding="utf-8") as handle:
        content = json.load(handle)
        if not isinstance(content, dict):
            raise ValueError("analytics.json must contain a JSON object.")
        return content


def normalize_user_item(item):
    if not isinstance(item, dict):
        return item

    fields = item.get("fields")
    if isinstance(fields, dict):
        normalized = dict(fields)
        if item.get("pk") is not None:
            normalized["id"] = item["pk"]
        elif item.get("id") is not None:
            normalized["id"] = item["id"]
        if "enabled" not in normalized and "is_active" in fields:
            normalized["enabled"] = bool(fields.get("is_active"))
        if "created_at" not in normalized and "date_joined" in fields:
            normalized["created_at"] = fields.get("date_joined")
        if "last_login_at" not in normalized and "last_login" in fields:
            normalized["last_login_at"] = fields.get("last_login")
        if "role" not in normalized and (
            "is_superuser" in fields or "is_staff" in fields
        ):
            normalized["role"] = (
                "ADMIN"
                if (fields.get("is_superuser") or fields.get("is_staff"))
                else "USER"
            )
        return normalized

    return item


def load_users(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as handle:
        content = json.load(handle)
        if not isinstance(content, list):
            raise ValueError("users.json must contain a JSON array.")
        return [normalize_user_item(item) for item in content]


def normalize_user_profile_item(item):
    if not isinstance(item, dict):
        return None

    fields = item.get("fields")
    normalized = {}
    if isinstance(fields, dict):
        user_id = fields.get("user")
        if user_id is not None:
            normalized["user_id"] = user_id
        nickname = fields.get("nickname")
        if nickname is not None:
            normalized["nickname"] = nickname
        role = fields.get("role")
        if role is not None:
            normalized["role"] = role
        if fields.get("created_at") is not None:
            normalized["created_at"] = fields.get("created_at")
        if fields.get("updated_at") is not None:
            normalized["updated_at"] = fields.get("updated_at")
    else:
        user_id = item.get("user") or item.get("user_id")
        if user_id is not None:
            normalized["user_id"] = user_id
        if item.get("nickname") is not None:
            normalized["nickname"] = item.get("nickname")
        if item.get("role") is not None:
            normalized["role"] = item.get("role")
        if item.get("created_at") is not None:
            normalized["created_at"] = item.get("created_at")
        if item.get("updated_at") is not None:
            normalized["updated_at"] = item.get("updated_at")

    profile_pk = item.get("pk") or item.get("id")
    if profile_pk is not None:
        normalized["profile_pk"] = profile_pk

    return normalized if normalized.get("user_id") is not None else None


def load_user_profiles(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as handle:
        content = json.load(handle)
        if not isinstance(content, list):
            raise ValueError("user_data.json must contain a JSON array.")
        profiles = []
        for item in content:
            model = (item.get("model") or "").strip().lower()
            if model != "users.userprofile":
                continue
            normalized = normalize_user_profile_item(item)
            if normalized:
                profiles.append(normalized)
        return profiles


SOCIAL_PLATFORM_VALUES = {"DISCORD", "TELEGRAM", "GITHUB", "YOUTUBE"}


def normalize_social_platform(value):
    if not value:
        return None
    normalized = value.strip().upper()
    return normalized if normalized in SOCIAL_PLATFORM_VALUES else None


def normalize_social_link_item(item):
    if not isinstance(item, dict):
        return None
    model = (item.get("model") or "").strip().lower()
    if model != "users.sociallink":
        return None
    fields = item.get("fields") or {}
    profile_pk = fields.get("user_profile") or fields.get("profile")
    url = fields.get("url")
    platform = fields.get("platform")
    if profile_pk is None or not url or not platform:
        return None
    return {"profile_pk": profile_pk, "platform": platform, "url": url}


def load_social_links(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as handle:
        content = json.load(handle)
        if not isinstance(content, list):
            raise ValueError("social_links.json must contain a JSON array.")
        links = []
        for item in content:
            normalized = normalize_social_link_item(item)
            if normalized:
                links.append(normalized)
        return links


FRIENDSHIP_STATUS_VALUES = {"PENDING", "ACCEPTED", "BLOCKED"}


def normalize_friendship_status(value):
    if not value:
        return None
    normalized = value.strip().upper()
    return normalized if normalized in FRIENDSHIP_STATUS_VALUES else None


def normalize_friendship_item(item):
    if not isinstance(item, dict):
        return None
    model = (item.get("model") or "").strip().lower()
    if model != "users.friendship":
        return None
    fields = item.get("fields") or {}
    requester_id = fields.get("requester")
    addressee_id = fields.get("addressee")
    status = fields.get("status")
    if requester_id is None or addressee_id is None or status is None:
        return None
    normalized = {
        "requester_id": requester_id,
        "addressee_id": addressee_id,
        "status": status,
        "created_at": fields.get("created_at"),
        "updated_at": fields.get("updated_at"),
    }
    pk = item.get("pk")
    if pk is not None:
        normalized["id"] = pk
    return normalized


def load_friendships(path):
    if not path or not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as handle:
        content = json.load(handle)
        if not isinstance(content, list):
            raise ValueError("friendships.json must contain a JSON array.")
        friendships = []
        for item in content:
            normalized = normalize_friendship_item(item)
            if normalized:
                friendships.append(normalized)
        return friendships


def build_social_links_insert_statement(columns):
    required_columns = ["profile_id", "platform", "url"]
    missing = [col for col in required_columns if col not in columns]
    if missing:
        raise RuntimeError(
            f"Missing columns in social_links table: {', '.join(missing)}"
        )
    column_list = ", ".join(required_columns)
    value_list = ", ".join([f"%({col})s" for col in required_columns])
    updates = ", ".join(
        [f"{col} = EXCLUDED.{col}" for col in required_columns if col != "profile_id"]
    )
    return (
        f"""
        INSERT INTO social_links ({column_list})
        VALUES ({value_list})
        ON CONFLICT (profile_id, platform) DO UPDATE
        SET {updates}
    """,
        required_columns,
    )


def build_social_link_row(item, profile_pk_map, column_order):
    profile_pk = item.get("profile_pk")
    profile_id = profile_pk_map.get(profile_pk)
    if profile_id is None:
        print(f"Skipping social link for profile pk={profile_pk}; profile not migrated")
        return None
    platform = normalize_social_platform(item.get("platform"))
    if platform is None:
        print(f"Skipping social link with unknown platform: {item.get('platform')}")
        return None
    url = item.get("url")
    if not url:
        print(f"Skipping social link for profile pk={profile_pk} because url is empty")
        return None
    row = {"profile_id": profile_id, "platform": platform, "url": url}
    return {key: row[key] for key in column_order}


def build_friend_requests_insert_statement(columns):
    required_columns = ["requester_id", "addressee_id", "status"]
    missing = [col for col in required_columns if col not in columns]
    if missing:
        raise RuntimeError(
            f"Missing columns in friend_requests table: {', '.join(missing)}"
        )

    column_order = []
    if "id" in columns:
        column_order.append("id")
    column_order.extend(required_columns)
    for col in ("blocked_by_id", "created_at", "updated_at"):
        if col in columns:
            column_order.append(col)

    column_list = ", ".join(column_order)
    value_list = ", ".join([f"%({col})s" for col in column_order])
    updates = ", ".join(
        [f"{col} = EXCLUDED.{col}" for col in column_order if col != "id"]
    )

    conflict_target = "id" if "id" in columns else "requester_id, addressee_id"

    sql = f"""
        INSERT INTO friend_requests ({column_list})
        VALUES ({value_list})
        ON CONFLICT ({conflict_target}) DO UPDATE
        SET {updates}
    """
    return sql, column_order


def build_friend_request_row(item, column_order, valid_user_ids):
    requester_id = item.get("requester_id")
    addressee_id = item.get("addressee_id")
    if requester_id is None or addressee_id is None:
        return None
    if requester_id not in valid_user_ids or addressee_id not in valid_user_ids:
        print(
            "Skipping friendship because users were not migrated: "
            f"requester_id={requester_id}, addressee_id={addressee_id}"
        )
        return None

    status = normalize_friendship_status(item.get("status"))
    if status is None:
        print(f"Skipping friendship with unknown status: {item.get('status')}")
        return None

    row = {
        "requester_id": requester_id,
        "addressee_id": addressee_id,
        "status": status,
    }

    if "id" in column_order:
        row["id"] = item.get("id")
    if "created_at" in column_order:
        row["created_at"] = parse_timestamp(item.get("created_at"))
    if "updated_at" in column_order:
        row["updated_at"] = parse_timestamp(item.get("updated_at"))
    if "blocked_by_id" in column_order:
        row["blocked_by_id"] = requester_id if status == "BLOCKED" else None

    return {key: row.get(key) for key in column_order}


def get_table_columns(cursor, table_name, schema=None):
    if schema is None:
        cursor.execute(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = ANY(current_schemas(true))
              AND table_name = %s
            """,
            (table_name,),
        )
    else:
        cursor.execute(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = %s
              AND table_name = %s
            """,
            (schema, table_name),
        )
    return {row[0] for row in cursor.fetchall()}


def table_exists(cursor, table_name):
    cursor.execute(
        """
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = ANY(current_schemas(true))
          AND table_name = %s
        """,
        (table_name,),
    )
    return cursor.fetchone() is not None


def resolve_table_schema(cursor, table_name):
    cursor.execute(
        """
        SELECT table_schema
        FROM information_schema.tables
        WHERE table_name = %s
        ORDER BY
            CASE WHEN table_schema = ANY(current_schemas(true)) THEN 0 ELSE 1 END,
            table_schema
        """,
        (table_name,),
    )
    rows = [row[0] for row in cursor.fetchall()]
    return rows[0] if rows else None


def build_insert_statement(columns, type_column, include_created_at):
    required_columns = [
        "id",
        "name",
        "version",
        type_column,
        "filename",
        "md5_hash",
        "size",
        "main_class",
        "show",
        "working",
        "launches",
        "downloads",
    ]
    if include_created_at:
        required_columns.append("created_at")

    missing = [col for col in required_columns if col not in columns]
    if missing:
        raise RuntimeError(f"Missing columns in clients table: {', '.join(missing)}")

    column_list = ", ".join(required_columns)
    value_list = ", ".join([f"%({col})s" for col in required_columns])
    updates = ", ".join(
        [f"{col} = EXCLUDED.{col}" for col in required_columns if col != "id"]
    )

    return f"""
        INSERT INTO clients ({column_list})
        VALUES ({value_list})
        ON CONFLICT (id) DO UPDATE
        SET {updates}
    """


def build_dependency_insert_statement(columns):
    required_columns = ["client_id", "name", "md5_hash", "size"]
    missing = [col for col in required_columns if col not in columns]
    if missing:
        raise RuntimeError(
            f"Missing columns in fabric_dependences table: {', '.join(missing)}"
        )

    column_list = ", ".join(required_columns)
    value_list = ", ".join([f"%({col})s" for col in required_columns])
    updates = ", ".join(
        [f"{col} = EXCLUDED.{col}" for col in required_columns if col != "client_id"]
    )

    return f"""
        INSERT INTO fabric_dependences ({column_list})
        VALUES ({value_list})
        ON CONFLICT (client_id, name) DO UPDATE
        SET {updates}
    """


def build_client_row(item, type_column, include_created_at):
    row = {
        "id": item["id"],
        "name": item["name"],
        "version": normalize_version(item["version"]),
        type_column: item.get("client_type", "Vanilla"),
        "filename": item.get("filename"),
        "md5_hash": item.get("md5_hash"),
        "size": item.get("size", 0),
        "main_class": item.get("main_class", "net.minecraft.client.main.Main"),
        "show": item.get("show", True),
        "working": item.get("working", True),
        "launches": item.get("launches", 0),
        "downloads": item.get("downloads", 0),
    }
    if include_created_at:
        row["created_at"] = parse_timestamp(item.get("created_at"))

    return row


def normalize_user_role(value):
    if not value:
        return "USER"
    lowered = value.strip().lower()
    admin_roles = {"admin", "owner", "developer"}
    return "ADMIN" if lowered in admin_roles else "USER"


def normalize_profile_role(value):
    allowed_roles = {"USER", "TESTER", "ADMIN", "DEVELOPER", "OWNER"}
    if not value:
        return "USER"
    candidate = value.strip().upper()
    return candidate if candidate in allowed_roles else "USER"


def build_users_insert_statement(columns):
    base_columns = ["id", "username", "password", "email", "enabled", "role"]
    optional_columns = [
        col for col in ("created_at", "updated_at", "last_login_at") if col in columns
    ]
    required_columns = base_columns + optional_columns

    missing = [col for col in base_columns if col not in columns]
    if missing:
        raise RuntimeError(f"Missing columns in users table: {', '.join(missing)}")

    column_list = ", ".join(required_columns)
    value_list = ", ".join([f"%({col})s" for col in required_columns])
    updates = ", ".join(
        [f"{col} = EXCLUDED.{col}" for col in required_columns if col != "id"]
    )

    sql = f"""
        INSERT INTO users ({column_list})
        VALUES ({value_list})
        ON CONFLICT (id) DO UPDATE
        SET {updates}
    """
    return sql, required_columns


def build_user_row(item, required_columns):
    missing_keys = [
        key
        for key in ("id", "username", "email")
        if key not in item or item[key] is None
    ]
    if missing_keys:
        raise ValueError(
            f"User item missing required fields: {', '.join(missing_keys)}"
        )

    now = datetime.now(timezone.utc)

    password = item.get("password")
    if password is None:
        password = DEFAULT_PASSWORD_HASH

    row = {
        "id": item["id"],
        "username": item["username"],
        "password": password,
        "email": item["email"],
        "enabled": item.get("enabled", True),
        "role": normalize_user_role(item.get("role")),
    }
    if "created_at" in required_columns:
        row["created_at"] = parse_timestamp(item.get("created_at")) or now
    if "updated_at" in required_columns:
        row["updated_at"] = (
            parse_timestamp(item.get("updated_at")) or row.get("created_at") or now
        )
    if "last_login_at" in required_columns:
        row["last_login_at"] = parse_timestamp(item.get("last_login_at"))

    return {key: row[key] for key in required_columns}


def ensure_unique_username(user_id, desired, seen_usernames):
    if desired and desired not in seen_usernames:
        seen_usernames.add(desired)
        return desired, False
    counter = 0
    while True:
        candidate = f"user_{user_id}" if counter == 0 else f"user_{user_id}_{counter}"
        if candidate not in seen_usernames:
            seen_usernames.add(candidate)
            return candidate, True
        counter += 1


def ensure_unique_email(user_id, desired, seen_emails):
    if desired and desired not in seen_emails:
        seen_emails.add(desired)
        return desired, False
    counter = 0
    while True:
        candidate = (
            f"user_{user_id}@invalid.local"
            if counter == 0
            else f"user_{user_id}_{counter}@invalid.local"
        )
        if candidate not in seen_emails:
            seen_emails.add(candidate)
            return candidate, True
        counter += 1


def get_required_user_ids(user_profiles, friendships):
    required_ids = set()
    for item in user_profiles:
        user_id = item.get("user_id")
        if user_id is not None:
            required_ids.add(user_id)
    for item in friendships:
        requester_id = item.get("requester_id")
        addressee_id = item.get("addressee_id")
        if requester_id is not None:
            required_ids.add(requester_id)
        if addressee_id is not None:
            required_ids.add(addressee_id)
    return required_ids


def parse_created_at_for_sort(value):
    if not value:
        return None
    try:
        return parse_timestamp(value)
    except (TypeError, ValueError):
        return None


def build_user_profile_insert_statement(columns, include_avatars):
    required_columns = ["user_id", "role", "launches_count", "total_playtime_seconds"]

    optional_columns = [
        col for col in ("nickname", "created_at", "updated_at") if col in columns
    ]

    if include_avatars:
        optional_columns.extend(
            col
            for col in ("avatar_path", "avatar_updated_at")
            if col in columns and col not in optional_columns
        )

    missing = [col for col in required_columns if col not in columns]
    if missing:
        raise RuntimeError(
            f"Missing columns in user_profiles table: {', '.join(missing)}"
        )

    column_order = required_columns + optional_columns
    column_list = ", ".join(column_order)
    value_list = ", ".join([f"%({col})s" for col in column_order])
    updates = ", ".join(
        [f"{col} = EXCLUDED.{col}" for col in column_order if col != "user_id"]
    )

    sql = f"""
        INSERT INTO user_profiles ({column_list})
        VALUES ({value_list})
        ON CONFLICT (user_id) DO UPDATE
        SET {updates}
    """
    return sql, column_order


def build_user_profile_row(item, column_order):
    first = (item.get("first_name") or "").strip()
    last = (item.get("last_name") or "").strip()
    nickname = " ".join(part for part in (first, last) if part).strip()
    if not nickname:
        nickname = item.get("username")

    now = datetime.now(timezone.utc)

    base = {
        "user_id": item["id"],
        "role": normalize_profile_role(item.get("role")),
        "nickname": nickname,
        "avatar_path": None,
        "avatar_updated_at": None,
        "launches_count": 0,
        "total_playtime_seconds": 0,
    }
    if "created_at" in column_order:
        base["created_at"] = parse_timestamp(item.get("created_at")) or now
    if "updated_at" in column_order:
        base["updated_at"] = (
            parse_timestamp(item.get("updated_at")) or base.get("created_at") or now
        )

    return {key: base[key] for key in column_order}


def build_user_profile_row_from_profile(item, column_order):
    user_id = item.get("user_id")
    if user_id is None:
        raise ValueError("User profile item missing required field: user_id")

    nickname = item.get("nickname")
    if isinstance(nickname, str):
        nickname = nickname.strip() or None

    now = datetime.now(timezone.utc)
    base = {
        "user_id": user_id,
        "role": normalize_profile_role(item.get("role")),
        "nickname": nickname,
        "launches_count": 0,
        "total_playtime_seconds": 0,
    }
    if "created_at" in column_order:
        base["created_at"] = parse_timestamp(item.get("created_at")) or now
    if "updated_at" in column_order:
        base["updated_at"] = (
            parse_timestamp(item.get("updated_at")) or base.get("created_at") or now
        )

    return {key: base[key] for key in column_order}


def migrate(
    conn,
    clients,
    fabric_clients,
    forge_clients,
    analytics,
    users,
    user_profiles,
    social_links,
    friendships,
    skip_sequence_reset,
):
    with conn.cursor() as cursor:
        clients_schema = resolve_table_schema(cursor, "clients")
        if not clients_schema:
            raise RuntimeError("clients table not found in any schema.")

        cursor.execute("SELECT current_schemas(true)")
        current_schemas = cursor.fetchone()[0] or []
        if clients_schema not in current_schemas:
            print(f"Using schema '{clients_schema}' for migration.")
            cursor.execute(
                sql.SQL("SET search_path TO {}").format(sql.Identifier(clients_schema))
            )

        columns = get_table_columns(cursor, "clients", schema=clients_schema)

        type_column = "type"
        if type_column not in columns:
            raise RuntimeError(
                f"Required column '{type_column}' not found. "
                f"Available: {sorted(columns)}"
            )

        include_created_at = "created_at" in columns

        print(f"Using type column: {type_column}")
        print(f"Created_at present: {include_created_at}")
        print(f"Detected columns: {sorted(columns)}")

        insert_sql = build_insert_statement(columns, type_column, include_created_at)

        rows = []
        for item in clients + fabric_clients + forge_clients:
            rows.append(build_client_row(item, type_column, include_created_at))

        execute_batch(cursor, insert_sql, rows, page_size=200)

        dep_table_name = "fabric_dependences"

        if table_exists(cursor, dep_table_name):
            print(
                f"Found table {dep_table_name}, starting migration of dependencies..."
            )

            dep_insert_sql = f"""
                INSERT INTO {dep_table_name} (client_id, name, md5_hash, size)
                VALUES (%(client_id)s, %(name)s, %(md5_hash)s, %(size)s)
                ON CONFLICT (client_id, name) DO UPDATE
                SET md5_hash = EXCLUDED.md5_hash, size = EXCLUDED.size
            """

            dependency_rows = []
            for item in clients:
                deps = item.get("dependencies", [])
                if deps:
                    for d in deps:
                        dependency_rows.append(
                            {
                                "client_id": item["id"],
                                "name": d.get("name"),
                                "md5_hash": d.get("md5_hash"),
                                "size": d.get("size", 0),
                            }
                        )

            if dependency_rows:
                print(
                    f"Found {len(dependency_rows)} dependencies. Importing into Atlas..."
                )
                execute_batch(cursor, dep_insert_sql, dependency_rows, page_size=200)
            else:
                print("No nested dependencies found in clients.json.")
        else:
            print(f"Error: Table {dep_table_name} not found in the database.")

        if not skip_sequence_reset:
            cursor.execute("SELECT pg_get_serial_sequence('clients', 'id')")
            result = cursor.fetchone()
            if result and result[0]:
                cursor.execute(
                    "SELECT setval(%s, (SELECT MAX(id) FROM clients))", (result[0],)
                )

        loader_launches_raw = (analytics or {}).get("total_loader_launches")
        if loader_launches_raw is not None:
            try:
                loader_launches = int(loader_launches_raw)
            except (TypeError, ValueError) as exc:
                raise ValueError("total_loader_launches must be an integer.") from exc
            if table_exists(cursor, "analytics_counters"):
                analytics_columns = get_table_columns(cursor, "analytics_counters")
                missing_columns = {"counter_key", "value"} - analytics_columns
                if missing_columns:
                    raise RuntimeError(
                        f"Missing columns in analytics_counters table: {', '.join(sorted(missing_columns))}"
                    )

                cursor.execute(
                    """
                    INSERT INTO analytics_counters (counter_key, value)
                    VALUES (%s, %s)
                    ON CONFLICT (counter_key) DO UPDATE
                    SET value = EXCLUDED.value
                    """,
                    ("loader_launches", loader_launches),
                )
            else:
                raise RuntimeError(
                    "analytics_counters table not found; cannot migrate loader launches."
                )

        accepted_users = []
        if users:
            if not table_exists(cursor, "users"):
                raise RuntimeError("users table not found; cannot migrate users.")

            user_columns = get_table_columns(cursor, "users")
            users_insert_sql, required_user_columns = build_users_insert_statement(
                user_columns
            )
            next_user_id = None
            required_user_ids = get_required_user_ids(user_profiles, friendships)
            explicit_user_ids = set()
            for item in users:
                raw_id = item.get("id")
                if raw_id is None:
                    continue
                try:
                    explicit_user_ids.add(int(raw_id))
                except (TypeError, ValueError):
                    continue
            users_missing_ids = [item for item in users if item.get("id") is None]
            if users_missing_ids:
                if required_user_ids:
                    print(
                        "Assigning missing user ids using profile/friendship ids, "
                        "ordered by date_joined when available."
                    )
                cursor.execute("SELECT COALESCE(MAX(id), 0) FROM users")
                result = cursor.fetchone()
                next_user_id = (result[0] or 0) + 1
            used_ids = set()
            required_id_queue = (
                [
                    user_id
                    for user_id in sorted(required_user_ids)
                    if user_id not in explicit_user_ids
                ]
                if required_user_ids
                else []
            )

            user_rows = []
            seen_emails = set()
            seen_usernames = set()
            users_with_index = list(enumerate(users))
            if users_missing_ids:
                users_with_index.sort(
                    key=lambda entry: (
                        parse_created_at_for_sort(entry[1].get("created_at"))
                        or datetime.max.replace(tzinfo=timezone.utc),
                        entry[0],
                    )
                )

            for index, item in users_with_index:
                user_id = item.get("id")
                if user_id is None:
                    while required_id_queue and required_id_queue[0] in used_ids:
                        required_id_queue.pop(0)
                    if required_id_queue:
                        user_id = required_id_queue.pop(0)
                    if user_id is None:
                        if next_user_id is None:
                            raise ValueError(
                                "users.json entries are missing ids and no fallback id could be generated."
                            )
                        while next_user_id in used_ids:
                            next_user_id += 1
                        user_id = next_user_id
                        next_user_id += 1
                else:
                    try:
                        user_id = int(user_id)
                    except (TypeError, ValueError) as exc:
                        print(f"Invalid user id={user_id}; using fallback id.")
                        if next_user_id is None:
                            cursor.execute("SELECT COALESCE(MAX(id), 0) FROM users")
                            result = cursor.fetchone()
                            next_user_id = (result[0] or 0) + 1
                        while next_user_id in used_ids:
                            next_user_id += 1
                        user_id = next_user_id
                        next_user_id += 1
                used_ids.add(user_id)

                normalized_item = dict(item)
                normalized_item["id"] = user_id
                email, email_placeholder = ensure_unique_email(
                    user_id,
                    normalized_item.get("email"),
                    seen_emails,
                )
                username, username_placeholder = ensure_unique_username(
                    user_id,
                    normalized_item.get("username"),
                    seen_usernames,
                )
                if email_placeholder:
                    print(f"Using placeholder email for user id={user_id}")
                if username_placeholder:
                    print(f"Using placeholder username for user id={user_id}")
                normalized_item["email"] = email
                normalized_item["username"] = username
                if email_placeholder or username_placeholder:
                    normalized_item["enabled"] = False

                try:
                    user_row = build_user_row(normalized_item, required_user_columns)
                except ValueError as exc:
                    print(f"Using placeholder user for id={user_id} because: {exc}")
                    placeholder_username, _ = ensure_unique_username(
                        user_id, None, seen_usernames
                    )
                    placeholder_email, _ = ensure_unique_email(
                        user_id, None, seen_emails
                    )
                    placeholder_item = {
                        "id": user_id,
                        "username": placeholder_username,
                        "email": placeholder_email,
                        "password": DEFAULT_PASSWORD_HASH,
                        "enabled": False,
                        "role": "USER",
                    }
                    user_row = build_user_row(placeholder_item, required_user_columns)
                    normalized_item = dict(placeholder_item)

                user_rows.append(user_row)
                accepted_users.append(normalized_item)

            if user_rows:
                execute_batch(cursor, users_insert_sql, user_rows, page_size=200)

            if user_rows and not skip_sequence_reset:
                cursor.execute("SELECT pg_get_serial_sequence('users', 'id')")
                result = cursor.fetchone()
                if result and result[0]:
                    cursor.execute(
                        "SELECT setval(%s, (SELECT MAX(id) FROM users))", (result[0],)
                    )
            if required_user_ids:
                missing_required_ids = sorted(required_user_ids - used_ids)
                if missing_required_ids:
                    print(
                        f"Creating {len(missing_required_ids)} placeholder users to satisfy profile/friendship ids."
                    )
                placeholder_rows = []
                for user_id in missing_required_ids:
                    placeholder_username, _ = ensure_unique_username(
                        user_id, None, seen_usernames
                    )
                    placeholder_email, _ = ensure_unique_email(
                        user_id, None, seen_emails
                    )
                    placeholder_item = {
                        "id": user_id,
                        "username": placeholder_username,
                        "email": placeholder_email,
                        "password": DEFAULT_PASSWORD_HASH,
                        "enabled": False,
                        "role": "USER",
                    }
                    user_row = build_user_row(placeholder_item, required_user_columns)
                    placeholder_rows.append(user_row)
                    accepted_users.append(dict(placeholder_item))
                if placeholder_rows:
                    execute_batch(
                        cursor, users_insert_sql, placeholder_rows, page_size=200
                    )

        inserted_user_ids = {user["id"] for user in accepted_users}
        valid_user_ids = set(inserted_user_ids)
        if (user_profiles or friendships) and not valid_user_ids:
            if table_exists(cursor, "users"):
                cursor.execute("SELECT id FROM users")
                valid_user_ids = {row[0] for row in cursor.fetchall()}
            else:
                print(
                    "users table not found; skipping profile and friendship migration."
                )
        profile_pk_to_profile_id = {}
        if user_profiles or accepted_users:
            if table_exists(cursor, "user_profiles"):
                profile_columns = get_table_columns(cursor, "user_profiles")
                profiles_insert_sql, profile_column_order = (
                    build_user_profile_insert_statement(
                        profile_columns,
                        include_avatars=False,
                    )
                )

                profile_rows = []
                profile_user_ids_with_profile_data = set()
                profile_pk_to_user_id = {}

                for item in user_profiles:
                    user_id = item.get("user_id")
                    if user_id not in valid_user_ids:
                        print(f"Skipping profile for unknown user id={user_id}")
                        continue
                    profile_rows.append(
                        build_user_profile_row_from_profile(item, profile_column_order)
                    )
                    profile_user_ids_with_profile_data.add(user_id)
                    pk = item.get("profile_pk")
                    if pk is not None:
                        profile_pk_to_user_id[pk] = user_id

                for item in accepted_users:
                    user_id = item["id"]
                    if user_id in profile_user_ids_with_profile_data:
                        continue
                    profile_rows.append(
                        build_user_profile_row(item, profile_column_order)
                    )

                if profile_rows:
                    execute_batch(
                        cursor, profiles_insert_sql, profile_rows, page_size=200
                    )

                if profile_pk_to_user_id:
                    user_ids = list(set(profile_pk_to_user_id.values()))
                    cursor.execute(
                        "SELECT id, user_id FROM user_profiles WHERE user_id = ANY(%s)",
                        (user_ids,),
                    )
                    user_id_to_profile_id = {
                        row[1]: row[0] for row in cursor.fetchall()
                    }
                    for pk, user_id in profile_pk_to_user_id.items():
                        profile_id = user_id_to_profile_id.get(user_id)
                        if profile_id is not None:
                            profile_pk_to_profile_id[pk] = profile_id
            else:
                print("user_profiles table not found; skipping profile migration.")

        if social_links:
            if table_exists(cursor, "social_links"):
                social_columns = get_table_columns(cursor, "social_links")
                social_insert_sql, social_column_order = (
                    build_social_links_insert_statement(social_columns)
                )
                social_rows = []
                for item in social_links:
                    row = build_social_link_row(
                        item, profile_pk_to_profile_id, social_column_order
                    )
                    if row:
                        social_rows.append(row)
                if social_rows:
                    execute_batch(cursor, social_insert_sql, social_rows, page_size=200)
            else:
                print("social_links table not found; skipping social link migration.")

        if friendships:
            if table_exists(cursor, "friend_requests"):
                friendship_columns = get_table_columns(cursor, "friend_requests")
                friendship_insert_sql, friendship_column_order = (
                    build_friend_requests_insert_statement(friendship_columns)
                )
                friendship_rows = []
                for item in friendships:
                    row = build_friend_request_row(
                        item,
                        friendship_column_order,
                        valid_user_ids,
                    )
                    if row:
                        friendship_rows.append(row)
                if friendship_rows:
                    execute_batch(
                        cursor, friendship_insert_sql, friendship_rows, page_size=200
                    )
            else:
                print("friend_requests table not found; skipping friendship migration.")

    conn.commit()


def main():
    args = parse_args()
    selective = any(
        (
            args.clients,
            args.fabric,
            args.forge,
            args.analytics,
            args.users,
            args.user_profiles,
            args.social_links,
            args.friendship,
        )
    )
    should_run = lambda flag: (not selective) or flag

    clients = load_clients(args.json) if should_run(args.clients) else []
    fabric_clients = load_clients(args.fabric_json) if should_run(args.fabric) else []
    forge_clients = load_clients(args.forge_json) if should_run(args.forge) else []
    analytics = (
        load_analytics(args.analytics_json) if should_run(args.analytics) else {}
    )
    users = load_users(args.users_json) if should_run(args.users) else []
    user_profiles = (
        load_user_profiles(args.user_data_json)
        if should_run(args.user_profiles)
        else []
    )
    social_links = (
        load_social_links(args.social_links_json)
        if should_run(args.social_links)
        else []
    )
    friendships = (
        load_friendships(args.friendships_json) if should_run(args.friendship) else []
    )

    db_config = get_db_config()

    conn = psycopg2.connect(
        host=db_config["host"],
        port=db_config["port"],
        database=db_config["dbname"],
        user=db_config["user"],
        password=db_config["password"],
    )

    try:
        migrate(
            conn,
            clients,
            fabric_clients,
            forge_clients,
            analytics,
            users,
            user_profiles,
            social_links,
            friendships,
            args.skip_sequence_reset,
        )
        total_clients = len(clients)
        fabric_count = sum(1 for c in clients if c.get("client_type") == "FABRIC")
        forge_count = sum(1 for c in clients if c.get("client_type") == "FORGE")

        total_deps = 0
        for c in clients:
            total_deps += len(c.get("dependencies", []))

        print(
            f"Migration complete: {total_clients} clients "
            f"({fabric_count} fabric, {forge_count} forge) "
            f"and {total_deps} fabric dependencies migrated."
        )
    finally:
        conn.close()


if __name__ == "__main__":
    main()
