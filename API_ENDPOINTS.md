# Atlas API Endpoints

> Protected endpoints expect an `Authorization: Bearer <token>` header. Tokens are issued by `/api/v1/auth/login` or
> `/api/v1/auth/register` and appear under `AuthResponse.token`. JSON payloads require `Content-Type: application/json`;
> file uploads must use `multipart/form-data`. Most responses are wrapped inside `ApiResponse`.
>
> **Common enums & hints:**
>
> - `UserStatus`: `ONLINE`, `OFFLINE`.
> - `SocialPlatform`: `DISCORD`, `TELEGRAM`, `GITHUB`, `YOUTUBE`.
> - `Version`: `1.21.11`, `1.21.8`, `1.21.4`, `1.16.5`, `1.8.9` (strings or enum names like `v_1_21_11`).
> - `ClientType` API values: `default`, `forge`, `fabric`.
> - Friend request `type` query accepts `incoming`, `outgoing`, `blocked` (defaults to `incoming`).
> - Metadata and preference values accept any JSON (objects, arrays, primitives).

## Authentication

| Endpoint                                | Method | Auth   | Payload / Notes                                                                  |
| --------------------------------------- | ------ | ------ | -------------------------------------------------------------------------------- |
| `POST /api/v1/auth/register`            | POST   | None   | JSON `{"username": string, "password": string, "email": string}`.                |
| `POST /api/v1/auth/login`               | POST   | None   | Same body as register. Returns `token`.                                          |
| `POST /api/v1/auth/logout`              | POST   | Bearer | Invalidates current token.                                                       |
| `POST /api/v1/auth/setPassword`         | POST   | Bearer | `{"newPassword": string, "currentPassword": string}`. Returns refreshed `token`. |
| `GET /api/v1/auth/verify`               | GET    | None   | `?token=...` Verifies email.                                                     |
| `POST /api/v1/auth/resend-verification` | POST   | None   | `?email=...` Resends verification email.                                         |

## Public resources

### News

| Endpoint       | Method | Auth | Payload / Notes                                                                                                                                               |
| -------------- | ------ | ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/api/v1/news` | GET    | None | Optional header `Accept-Language: <lang>` (defaults to `en`). Returns an array of news DTOs (`id`, `title`, `content`, `language`, `createdAt`, `updatedAt`). |

### Preset marketplace

| Endpoint                                    | Method | Auth     | Payload / Notes                                                                                   |
| ------------------------------------------- | ------ | -------- | ------------------------------------------------------------------------------------------------- |
| `/api/v1/presets`                           | GET    | None     | Query params: `q` (query), `owner` (id), `sort` (e.g., `newest`), `limit` (max 100, defaults 50). |
| `/api/v1/presets/{id}`                      | GET    | None     | Returns preset detail.                                                                            |
| `/api/v1/presets`                           | POST   | Bearer   | Create preset. Body: `PresetUpsertRequest`.                                                       |
| `/api/v1/presets/{id}`                      | PATCH  | Bearer   | Update preset (owner).                                                                            |
| `/api/v1/presets/{id}`                      | DELETE | Bearer   | Delete preset (owner).                                                                            |
| `/api/v1/presets/{id}/like`                 | POST   | Bearer   | Likes the preset.                                                                                 |
| `/api/v1/presets/{id}/unlike`               | POST   | Bearer   | Removes your like.                                                                                |
| `/api/v1/presets/{id}/download`             | POST   | Optional | Increments download counter.                                                                      |
| `/api/v1/presets/{id}/comments`             | GET    | None     | Lists comments.                                                                                   |
| `/api/v1/presets/{id}/comments`             | POST   | Bearer   | Adds comment; body `{"content": string}`.                                                         |
| `/api/v1/presets/{id}/comments/{commentId}` | DELETE | Bearer   | Deletes comment (author/owner).                                                                   |

### Client catalog and downloads

| Endpoint                        | Method | Auth | Payload / Notes                                                                                                          |
| ------------------------------- | ------ | ---- | ------------------------------------------------------------------------------------------------------------------------ |
| `/api/v1/clients`               | GET    | None | Lists all clients (`ClientResponse`).                                                                                    |
| `/api/v1/clients/{id}/detailed` | GET    | None | Detailed info plus dependencies, ratings, and screenshots.                                                               |
| `/api/v1/clients/download/{id}` | POST   | None | Increments download counter.                                                                                             |
| `/api/v1/clients/launch/{id}`   | POST   | None | Increments launch counter.                                                                                               |
| `/api/v1/fabric-clients`        | GET    | None | All fabric clients.                                                                                                      |
| `/api/v1/fabric-clients/{id}`   | GET    | None | Fabric client detail.                                                                                                    |
| `/api/v1/forge-clients`         | GET    | None | All Forge clients.                                                                                                       |
| `/api/v1/forge-clients/{id}`    | GET    | None | Forge client detail.                                                                                                     |
| `/api/v1/loader/launch`         | POST   | None | Increments loader launch counter. Returns `{"total_loader_launches": number}`.                                           |
| `/api/v1/statistics`            | GET    | None | Returns totals `{ "total_client_launches": number, "total_client_downloads": number, "total_loader_launches": number }`. |

### Agent & overlay downloads

| Endpoint                          | Method | Auth | Payload / Notes                                                                                                                                    |
| --------------------------------- | ------ | ---- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/api/v1/agent/download`          | GET    | None | Streams universal `CollapseAgent.jar`.                                                                                                             |
| `/api/v1/overlay/download/{os}`   | GET    | None | `os` must be `windows` or `linux`. Streams native overlay file.                                                                                    |
| `/api/v1/agent-overlay/checksums` | GET    | None | Optional `os` query. Returns hashes: `agent_hash`, `overlay_hash` (if `os` provided) or `windows_overlay_hash`, `linux_overlay_hash` (if no `os`). |

### Achievements

| Endpoint                            | Method | Auth   | Payload / Notes                                               |
| ----------------------------------- | ------ | ------ | ------------------------------------------------------------- |
| `/api/v1/achievements`              | GET    | None   | Lists all available achievements.                             |
| `/api/v1/achievements/users/{id}`   | GET    | None   | Lists unlocked achievements for a specific user.              |
| `/api/v1/achievements/unlock/{key}` | POST   | Bearer | Unlocks achievement (currently only `SECRET_FINDER` allowed). |

## User services (requires Bearer)

### Profile & Accounts

| Endpoint                               | Method | Auth   | Payload / Notes                                                          |
| -------------------------------------- | ------ | ------ | ------------------------------------------------------------------------ |
| `/api/v1/users/init`                   | GET    | Bearer | Composite profile/friends/preferences/favorites bootstrap.               |
| `/api/v1/users/me`                     | GET    | Bearer | Returns `UserMeResponse`.                                                |
| `/api/v1/users/{userId}`               | GET    | Bearer | Public profile; `?include=achievements,presets` supported.               |
| `/api/v1/users/search?q=...&limit=...` | GET    | Bearer | Search users by username.                                                |
| `/api/v1/users/me/profile`             | PATCH  | Bearer | Update nickname.                                                         |
| `/api/v1/users/me/avatar`              | POST   | Bearer | Multipart `avatar` file upload.                                          |
| `/api/v1/users/me/avatar/reset`        | POST   | Bearer | Resets avatar to default.                                                |
| `/api/v1/users/me/social-links`        | PUT    | Bearer | Replace all social links.                                                |
| `/api/v1/users/me/social-links`        | GET    | Bearer | Returns social links.                                                    |
| `/api/v1/users/me/status`              | GET    | Bearer | Returns your status.                                                     |
| `/api/v1/users/me/status`              | PUT    | Bearer | Update status/client. JSON `{"status": "ONLINE", "client_name": "..."}`. |
| `/api/v1/users/{userId}/status`        | GET    | None   | Public status for user.                                                  |
| `/api/v1/users/me/accounts`            | GET    | Bearer | Lists linked accounts.                                                   |
| `/api/v1/users/me/accounts`            | POST   | Bearer | Link account. `{"display_name": "...", "metadata": {...}}`.              |
| `/api/v1/users/me/accounts/{id}`       | DELETE | Bearer | Unlink account.                                                          |
| `/api/v1/users/me/favorites`           | GET    | Bearer | Lists favorites.                                                         |
| `/api/v1/users/me/favorites`           | POST   | Bearer | Add favorite. `{"type": "...", "reference": "...", "metadata": {...}}`.  |
| `/api/v1/users/me/favorites/{id}`      | DELETE | Bearer | Remove favorite.                                                         |
| `/api/v1/users/me/preferences`         | GET    | Bearer | Lists preferences.                                                       |
| `/api/v1/users/me/preferences/{key}`   | PUT    | Bearer | Set preference value (raw JSON).                                         |
| `/api/v1/users/me/preferences/{key}`   | DELETE | Bearer | Remove preference.                                                       |

### Friends

| Endpoint                                | Method | Auth   | Payload / Notes                                                      |
| --------------------------------------- | ------ | ------ | -------------------------------------------------------------------- |
| `/api/v1/friends`                       | GET    | Bearer | Lists friends.                                                       |
| `/api/v1/friends/batch`                 | GET    | Bearer | Returns friends plus all request categories (sent/received/blocked). |
| `/api/v1/friends/requests?type=`        | GET    | Bearer | `type` can be `incoming`, `outgoing`, or `blocked`.                  |
| `/api/v1/friends/requests`              | POST   | Bearer | Send request: `{"user_id": number}`.                                 |
| `/api/v1/friends/requests/{id}/accept`  | POST   | Bearer | Accepts request.                                                     |
| `/api/v1/friends/requests/{id}/decline` | POST   | Bearer | Declines/cancels request.                                            |
| `/api/v1/friends/block`                 | POST   | Bearer | Block user: `{"user_id": number}`.                                   |
| `/api/v1/friends/unblock`               | POST   | Bearer | Unblock user: `{"user_id": number}`.                                 |
| `/api/v1/friends/{userId}`              | DELETE | Bearer | Removes friendship.                                                  |

### Reports

| Endpoint          | Method | Auth   | Payload / Notes                                                                          |
| ----------------- | ------ | ------ | ---------------------------------------------------------------------------------------- |
| `/api/v1/reports` | POST   | Bearer | Create report. `{"target_id": number, "type": "USER/PRESET/COMMENT", "reason": string}`. |

## Admin operations (requires Bearer + ADMIN role)

### Dashboard & System

| Endpoint                               | Method | Auth           | Payload / Notes                                          |
| -------------------------------------- | ------ | -------------- | -------------------------------------------------------- |
| `/api/v1/admin/stats`                  | GET    | Bearer (ADMIN) | Dashboard counters (users, news, reports, online users). |
| `/api/v1/admin/audit-logs`             | GET    | Bearer (ADMIN) | paged audit logs.                                        |
| `/api/v1/admin/status`                 | GET    | Bearer (ADMIN) | Subsystem health (DB, Redis, Storage).                   |
| `/api/v1/admin/clients/trigger-update` | POST   | Bearer (ADMIN) | Sends update command via WebSocket.                      |

### User Management

| Endpoint                                       | Method | Auth           | Payload / Notes                             |
| ---------------------------------------------- | ------ | -------------- | ------------------------------------------- |
| `/api/v1/admin/users`                          | GET    | Bearer (ADMIN) | Lists users with search/filter.             |
| `/api/v1/admin/users/{id}`                     | GET    | Bearer (ADMIN) | Full user details (profile, prefs, links).  |
| `/api/v1/admin/users/{id}`                     | PUT    | Bearer (ADMIN) | Update user (role, enabled, nickname, etc). |
| `/api/v1/admin/users/{id}`                     | DELETE | Bearer (ADMIN) | Full user deletion.                         |
| `/api/v1/admin/users/{id}/reset-password`      | POST   | Bearer (ADMIN) | Body `{"password": "..."}`.                 |
| `/api/v1/admin/users/{uId}/achievements/{key}` | POST   | Bearer (ADMIN) | Grant achievement.                          |
| `/api/v1/admin/users/{uId}/achievements/{key}` | DELETE | Bearer (ADMIN) | Revoke achievement.                         |

### Content Management

| Endpoint                             | Method | Auth           | Payload / Notes                  |
| ------------------------------------ | ------ | -------------- | -------------------------------- |
| `/api/v1/admin/news`                 | GET    | Bearer (ADMIN) | List all news.                   |
| `/api/v1/admin/news`                 | POST   | Bearer (ADMIN) | Create news article.             |
| `/api/v1/admin/news/{id}`            | PUT    | Bearer (ADMIN) | Update news.                     |
| `/api/v1/admin/news/{id}`            | DELETE | Bearer (ADMIN) | Delete news.                     |
| `/api/v1/admin/reports`              | GET    | Bearer (ADMIN) | List user reports.               |
| `/api/v1/admin/reports/{id}/resolve` | PUT    | Bearer (ADMIN) | Resolve report with note/action. |
| `/api/v1/admin/achievements`         | GET    | Bearer (ADMIN) | List achievements.               |
| `/api/v1/admin/achievements`         | POST   | Bearer (ADMIN) | Create achievement.              |
| `/api/v1/admin/achievements/{id}`    | PUT    | Bearer (ADMIN) | Update achievement.              |
| `/api/v1/admin/achievements/{id}`    | DELETE | Bearer (ADMIN) | Delete achievement.              |

### Client Registry

| Endpoint                         | Method | Auth           | Payload / Notes            |
| -------------------------------- | ------ | -------------- | -------------------------- |
| `/api/v1/admin/clients`          | GET    | Bearer (ADMIN) | List entries.              |
| `/api/v1/admin/clients/{id}`     | GET    | Bearer (ADMIN) | Details.                   |
| `/api/v1/admin/clients`          | POST   | Bearer (ADMIN) | Create entry.              |
| `/api/v1/admin/clients/{id}`     | PUT    | Bearer (ADMIN) | Update entry.              |
| `/api/v1/admin/clients/{id}`     | DELETE | Bearer (ADMIN) | Delete entry.              |
| `/api/v1/admin/clients/versions` | GET    | Bearer (ADMIN) | Supported version strings. |

### Assets & Artifacts

| Endpoint                                       | Method | Auth           | Payload / Notes                             |
| ---------------------------------------------- | ------ | -------------- | ------------------------------------------- |
| `/api/v1/admin/clients/{id}/screenshots`       | GET    | Bearer (ADMIN) | List screenshots.                           |
| `/api/v1/admin/clients/{id}/screenshots`       | POST   | Bearer (ADMIN) | Upload screenshots (Multipart).             |
| `/api/v1/admin/clients/{id}/screenshots/{sId}` | DELETE | Bearer (ADMIN) | Delete screenshot.                          |
| `/api/v1/admin/clients/{id}/screenshots/order` | PUT    | Bearer (ADMIN) | Set sort order (List of IDs).               |
| `/api/v1/admin/clients/{id}/fabric-deps`       | GET    | Bearer (ADMIN) | List Fabric deps.                           |
| `/api/v1/admin/clients/{id}/fabric-deps`       | POST   | Bearer (ADMIN) | Add Fabric dep manually.                    |
| `/api/v1/admin/clients/{id}/fabric-deps/{dId}` | PUT    | Bearer (ADMIN) | Update dep metadata.                        |
| `/api/v1/admin/clients/{id}/fabric-deps/{dId}` | DELETE | Bearer (ADMIN) | Remove dep.                                 |
| `/api/v1/admin/clients/{id}/forge-deps`        | GET    | Bearer (ADMIN) | List Forge deps.                            |
| `/api/v1/admin/clients/{id}/forge-deps`        | POST   | Bearer (ADMIN) | Add Forge dep manually.                     |
| `/api/v1/admin/clients/{id}/forge-deps/{dId}`  | PUT    | Bearer (ADMIN) | Update dep metadata.                        |
| `/api/v1/admin/clients/{id}/forge-deps/{dId}`  | DELETE | Bearer (ADMIN) | Remove dep.                                 |
| `/api/v1/admin/upload`                         | POST   | Bearer (ADMIN) | Direct file storage upload (`?target=...`). |
| `/api/v1/admin/upload/chunk`                   | POST   | Bearer (ADMIN) | Upload file chunk.                          |
| `/api/v1/admin/upload/merge`                   | POST   | Bearer (ADMIN) | Merge chunks into final file.               |
| `/api/v1/agent/upload`                         | POST   | Bearer (ADMIN) | Specialized agent artifact upload.          |
| `/api/v1/overlay/upload/{os}`                  | POST   | Bearer (ADMIN) | Specialized overlay artifact upload.        |

### File Manager (Internal)

| Endpoint                               | Method | Auth           | Payload / Notes                          |
| -------------------------------------- | ------ | -------------- | ---------------------------------------- |
| `/api/v1/admin/files`                  | GET    | Bearer (ADMIN) | Browse filesystem storage (`?path=...`). |
| `/api/v1/admin/files/sync`             | POST   | Bearer (ADMIN) | Verify storage integrity.                |
| `/api/v1/admin/files/trash`            | GET    | Bearer (ADMIN) | List deleted files (metadata).           |
| `/api/v1/admin/files/restore/{*name}`  | POST   | Bearer (ADMIN) | Restore file from trash.                 |
| `/api/v1/admin/files/create-directory` | POST   | Bearer (ADMIN) | New storage directory.                   |
| `/api/v1/admin/files/rename`           | POST   | Bearer (ADMIN) | Move/rename files.                       |
| `/api/v1/admin/files/{*name}`          | DELETE | Bearer (ADMIN) | Soft/hard delete storage file.           |

## Static Resources

| Endpoint      | Method | Auth | Payload / Notes                        |
| ------------- | ------ | ---- | -------------------------------------- |
| `/uploads/**` | GET    | None | Direct access to stored binary assets. |
