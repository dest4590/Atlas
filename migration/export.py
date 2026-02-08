import json
import os
from django.apps import apps
from django.contrib.auth.models import User


def format_date(dt):
    if not dt:
        return None
    return dt.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"


print("Starting full data export for Atlas...")

if not os.path.exists("migration"):
    os.makedirs("migration")
    print("Directory 'migration/' created.")

try:
    Client = apps.get_model("clients", "Client")
    FabricClient = apps.get_model("clients", "FabricClient")
    ForgeClient = apps.get_model("clients", "ForgeClient")
    FabricRequirement = apps.get_model("clients", "FabricRequirement")

    fabric_ids = set(FabricClient.objects.values_list("id", flat=True))
    forge_ids = set(ForgeClient.objects.values_list("id", flat=True))

    all_clients = []
    client_count = 0
    dep_count = 0

    for c in Client.objects.all():
        client_type = "Vanilla"
        if c.id in fabric_ids:
            client_type = "FABRIC"
        elif c.id in forge_ids:
            client_type = "FORGE"

        data = {
            "id": c.id,
            "name": c.name,
            "version": c.version,
            "client_type": client_type,
            "filename": c.filename,
            "md5_hash": c.md5_hash,
            "size": c.size,
            "main_class": getattr(c, "main_class", "net.minecraft.client.main.Main"),
            "show": getattr(c, "show", True),
            "working": getattr(c, "working", True),
            "created_at": (
                format_date(c.created_at) if hasattr(c, "created_at") else None
            ),
            "launches": 0,
            "downloads": 0,
            "dependencies": [],
        }

        if client_type == "FABRIC":
            deps = FabricRequirement.objects.filter(client_id=c.id)
            for d in deps:
                data["dependencies"].append(
                    {"name": d.name, "md5_hash": d.md5_hash, "size": d.size}
                )
                dep_count += 1

        try:
            from client_statistics.models import ClientLaunchStats, ClientDownloadStats

            data["launches"] = ClientLaunchStats.objects.get(client_id=c.id).launches
            data["downloads"] = ClientDownloadStats.objects.get(
                client_id=c.id
            ).downloads
        except:
            pass

        all_clients.append(data)
        client_count += 1

    with open("migration/clients.json", "w", encoding="utf-8") as f:
        json.dump(all_clients, f, indent=4)
    print(f"clients.json exported: {client_count} clients, {dep_count} dependencies.")
except Exception as e:
    print(f"Error exporting clients: {e}")

try:
    user_list = []
    for u in User.objects.all():
        user_list.append(
            {
                "model": "auth.user",
                "pk": u.id,
                "fields": {
                    "password": u.password,
                    "last_login": format_date(u.last_login),
                    "is_superuser": u.is_superuser,
                    "username": u.username,
                    "first_name": u.first_name,
                    "last_name": u.last_name,
                    "email": u.email,
                    "is_staff": u.is_staff,
                    "is_active": u.is_active,
                    "date_joined": format_date(u.date_joined),
                },
            }
        )
    with open("migration/users.json", "w", encoding="utf-8") as f:
        json.dump(user_list, f, indent=4)
    print(f"users.json exported: {len(user_list)} users.")
except Exception as e:
    print(f"Error exporting users: {e}")


try:
    UserProfile = apps.get_model("users", "UserProfile")
    profile_list = []
    for p in UserProfile.objects.all():
        profile_list.append(
            {
                "model": "users.userprofile",
                "pk": p.id,
                "fields": {
                    "user": p.user_id,
                    "nickname": p.nickname,
                    "avatar": str(p.avatar) if p.avatar else "",
                    "avatar_updated_at": format_date(p.avatar_updated_at),
                    "role": p.role,
                    "created_at": format_date(p.created_at),
                    "updated_at": format_date(p.updated_at),
                },
            }
        )
    with open("migration/user_data.json", "w", encoding="utf-8") as f:
        json.dump(profile_list, f, indent=4)
    print(f"user_data.json exported: {len(profile_list)} user profiles.")
except Exception as e:
    print(f"Error exporting user profiles: {e}")


try:
    SocialLink = apps.get_model("users", "SocialLink")
    link_list = []
    for sl in SocialLink.objects.all():
        link_list.append(
            {
                "model": "users.sociallink",
                "pk": sl.id,
                "fields": {
                    "user_profile": sl.user_profile_id,
                    "platform": sl.platform,
                    "url": sl.url,
                },
            }
        )
    with open("migration/social_links.json", "w", encoding="utf-8") as f:
        json.dump(link_list, f, indent=4)
    print(f"social_links.json exported: {len(link_list)} social links.")
except Exception as e:
    print(f"Error exporting social links: {e}")


try:
    Friendship = apps.get_model("users", "Friendship")
    friend_list = []
    for fr in Friendship.objects.all():
        friend_list.append(
            {
                "model": "users.friendship",
                "pk": fr.id,
                "fields": {
                    "requester": fr.requester_id,
                    "addressee": fr.addressee_id,
                    "status": fr.status,
                    "created_at": format_date(fr.created_at),
                    "updated_at": format_date(fr.updated_at),
                },
            }
        )
    with open("migration/friendships.json", "w", encoding="utf-8") as f:
        json.dump(friend_list, f, indent=4)
    print(f"friendships.json exported: {len(friend_list)} friendships.")
except Exception as e:
    print(f"Error exporting friendships: {e}")

print("\nExport completed. All files are ready in the 'migration/' directory.")
