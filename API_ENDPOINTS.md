# Atlas API Endpoints

> Protected endpoints expect an `Authorization: Bearer <token>` header. Tokens are issued by `/api/v1/auth/login` or
> `/api/v1/auth/register` and appear under `AuthResponse.token`. JSON payloads require `Content-Type: application/json`;
> file uploads must use `multipart/form-data`. Most responses are wrapped inside `ApiResponse`, while download endpoints
> stream binary payloads and `/agent-overlay/checksums` returns raw JSON.
>
> **Common enums & hints:**
>
> - `UserStatus`: `ONLINE`, `OFFLINE`.
> - `SocialPlatform`: `DISCORD`, `TELEGRAM`, `GITHUB`, `YOUTUBE`.
> - `Version`: `1.21.11`, `1.21.8`, `1.21.4`, `1.16.5`, `1.8.9` (strings or enum names like `v_1_21_11`).
> - `ClientType` API values: `default`, `forge`, `fabric`.
> - Friend request `type` query accepts `incoming`, `outgoing`, or `all` (defaults to `incoming`).
> - Metadata and preference values accept any JSON (objects, arrays, primitives).

## Authentication

| Endpoint                        | Method | Auth   | Payload / Notes                                                                    |
| ------------------------------- | ------ | ------ | ---------------------------------------------------------------------------------- |
| `POST /api/v1/auth/register`    | POST   | None   | JSON `{"username": string, "password": string, "email": string}`. Returns `token`. |
| `POST /api/v1/auth/login`       | POST   | None   | Same body as register. Returns `token`.                                            |
| `POST /api/v1/auth/setPassword` | POST   | Bearer | `{"newPassword": string, "currentPassword": string}`. Returns refreshed `token`.   |

## Public resources

### Status & news

| Endpoint       | Method | Auth | Payload / Notes                                                                                                                                               |
| -------------- | ------ | ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/`            | GET    | None | Health check returning `ApiResponse` with `{ "status": 200 }`.                                                                                                |
| `/api/v1/news` | GET    | None | Optional header `Accept-Language: <lang>` (defaults to `en`). Returns an array of news DTOs (`id`, `title`, `content`, `language`, `createdAt`, `updatedAt`). |

### Preset marketplace

| Endpoint                                 | Method | Auth     | Payload / Notes                                                                                                                                                                                                                                                                                                                                                                                                              |
| ---------------------------------------- | ------ | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/api/presets`                           | GET    | None     | Query params: `q` (search name), `owner` (owner id), `mine` (bool, requires Bearer), `limit` (max 100, defaults 50). Returns presets with counts and theme summary.                                                                                                                                                                                                                                                          |
| `/api/presets/{id}`                      | GET    | None     | Returns preset detail (`name`/`title`, `is_public`, `likes_count`, `downloads_count`, `comments_count`, `liked`, `author`, `theme`). Private presets are visible only to owner or admins.                                                                                                                                                                                                                                    |
| `/api/presets`                           | POST   | Bearer   | Create preset. Body: `name` (or `title`), optional `description`, `is_public` (default true), theme fields (`customCSS`, `enableCustomCSS`, `base100`, `base200`, `base300`, `baseContent`, `primary`, `primaryContent`, `secondary`, `secondaryContent`, `accent`, `accentContent`, `neutral`, `neutralContent`, `info`, `infoContent`, `success`, `successContent`, `warning`, `warningContent`, `error`, `errorContent`). |
| `/api/presets/{id}`                      | PATCH  | Bearer   | Update preset (owner or admin). Same fields as create; omitting a field keeps its current value.                                                                                                                                                                                                                                                                                                                             |
| `/api/presets/{id}`                      | DELETE | Bearer   | Delete preset (owner or admin).                                                                                                                                                                                                                                                                                                                                                                                              |
| `/api/presets/{id}/like`                 | POST   | Bearer   | Likes the preset; idempotent. Returns updated preset with `liked=true`.                                                                                                                                                                                                                                                                                                                                                      |
| `/api/presets/{id}/unlike`               | POST   | Bearer   | Removes your like; idempotent. Returns updated preset.                                                                                                                                                                                                                                                                                                                                                                       |
| `/api/presets/{id}/download`             | POST   | Optional | Increments download counter (requires Bearer to access private presets). Returns updated preset.                                                                                                                                                                                                                                                                                                                             |
| `/api/presets/{id}/comments`             | GET    | None     | Lists comments for the preset (blocked for private presets if you are not allowed).                                                                                                                                                                                                                                                                                                                                          |
| `/api/presets/{id}/comments`             | POST   | Bearer   | Adds comment; body `{ "text": string }`, trims and caps at 2000 chars.                                                                                                                                                                                                                                                                                                                                                       |
| `/api/presets/{id}/comments/{commentId}` | DELETE | Bearer   | Deletes comment if you are the author, preset owner, or admin.                                                                                                                                                                                                                                                                                                                                                               |

### Client catalog and downloads

| Endpoint                        | Method | Auth | Payload / Notes                                                                                                          |
| ------------------------------- | ------ | ---- | ------------------------------------------------------------------------------------------------------------------------ |
| `/api/v1/clients`               | GET    | None | Lists all clients (`ClientResponse`).                                                                                    |
| `/api/v1/clients/{id}/detailed` | GET    | None | Detailed info plus dependencies, ratings, and screenshots.                                                               |
| `/api/v1/clients/download/{id}` | POST   | None | Increments download counter; no body needed.                                                                             |
| `/api/v1/clients/launch/{id}`   | POST   | None | Increments launch counter; no body needed.                                                                               |
| `/api/v1/fabric-clients`        | GET    | None | All fabric clients.                                                                                                      |
| `/api/v1/fabric-clients/{id}`   | GET    | None | Fabric client detail.                                                                                                    |
| `/api/v1/forge-clients`         | GET    | None | All Forge clients.                                                                                                       |
| `/api/v1/forge-clients/{id}`    | GET    | None | Forge client detail.                                                                                                     |
| `/api/v1/loader/launch`         | POST   | None | Increments loader launch counter; no body. Returns `ApiResponse` with `total_loader_launches`.                           |
| `/api/v1/statistics`            | GET    | None | Returns totals `{ "total_client_launches": number, "total_client_downloads": number, "total_loader_launches": number }`. |

### Agent & overlay downloads

| Endpoint                                     | Method | Auth | Payload / Notes                                                                                                                          |
| -------------------------------------------- | ------ | ---- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `/agent/download`                            | GET    | None | Streams universal `CollapseAgent.jar` (legacy `/agent/download/windows` and `/agent/download/linux` still work).                         |
| `/overlay/download/{os}`                     | GET    | None | `os` must be `windows` or `linux`. Streams `CollapseOverlay.dll` (Windows) or `libCollapseOverlay.so` (Linux).                           |
| `/agent-overlay/checksums?os=windows\|linux` | GET    | None | `os` must be `windows` or `linux`. Returns MD5s `{ "agent_hash": "...", "overlay_hash": "..." }`; agent hash is shared across platforms. |

## Client ratings (requires Bearer)

| Endpoint                      | Method | Auth   | Payload / Notes                                                                                 |
| ----------------------------- | ------ | ------ | ----------------------------------------------------------------------------------------------- |
| `/api/v1/clients/{id}/rating` | POST   | Bearer | `{"rating": number}` (short). UI expects 1-5 but backend accepts any short. Returns rating DTO. |
| `/api/v1/clients/{id}/rating` | GET    | Bearer | Returns your rating for that client.                                                            |
| `/api/v1/clients/{id}/rating` | DELETE | Bearer | Deletes your rating.                                                                            |

## User self-service (requires Bearer)

| Endpoint                                                                                                          | Method                                         | Auth   | Payload / Notes                                      |
| ----------------------------------------------------------------------------------------------------------------- | ---------------------------------------------- | ------ | ---------------------------------------------------- |
| `/api/v1/users/me`                                                                                                | GET                                            | Bearer | Returns `UserMeResponse` (account, profile, status). |
| `/api/v1/users/{userId}`                                                                                          | GET                                            | Bearer | Public profile and friendship status versus you.     |
| `/api/v1/users/search?q=...&limit=...`                                                                            | GET                                            | Bearer | `q` is required; `limit` defaults to 20.             |
| `/api/v1/users/me/profile`                                                                                        | PATCH                                          | Bearer | `{"nickname": string}`.                              |
| `/api/v1/users/me/avatar`                                                                                         | POST                                           | Bearer | Multipart `avatar` file (image).                     |
| `/api/v1/users/me/avatar/reset`                                                                                   | POST                                           | Bearer | Without body; resets avatar.                         |
| `/api/v1/users/me/social-links`                                                                                   | PUT                                            | Bearer |
| `{"links": [{"platform": <SocialPlatform>, "url": "https://..."}, ...]}`. Replaces all social links.              |
| `/api/v1/users/me/social-links`                                                                                   | GET                                            | Bearer | Returns saved social links.                          |
| `/api/v1/users/me/status`                                                                                         | GET                                            | Bearer | Returns your `UserStatusResponse`.                   |
| `/api/v1/users/me/status`                                                                                         | PUT                                            | Bearer |
| `{"status": "ONLINE"                                                                                              | "OFFLINE", "client_name": string (optional)}`. |
| `/api/v1/users/{userId}/status`                                                                                   | GET                                            | None   | Public status for another user.                      |
| `/api/v1/users/me/accounts`                                                                                       | GET                                            | Bearer | Lists linked external accounts.                      |
| `/api/v1/users/me/accounts`                                                                                       | POST                                           | Bearer |
| `{"provider": string, "external_id": string, "display_name": string, "metadata": {...}}`. Metadata optional JSON. |
| `/api/v1/users/me/accounts/{accountId}`                                                                           | DELETE                                         | Bearer | Deletes an external account.                         |
| `/api/v1/users/me/favorites`                                                                                      | GET                                            | Bearer | Lists favorites.                                     |
| `/api/v1/users/me/favorites`                                                                                      | POST                                           | Bearer |
| `{"type": string, "reference": string, "metadata": {...}}`. Metadata optional JSON.                               |
| `/api/v1/users/me/favorites/{favoriteId}`                                                                         | DELETE                                         | Bearer | Removes favorite.                                    |
| `/api/v1/users/me/preferences`                                                                                    | GET                                            | Bearer | Lists preferences.                                   |
| `/api/v1/users/me/preferences/{key}`                                                                              | PUT                                            | Bearer | Body is raw JSON (true, 123, "special", {"foo":      |
| 1}, [1,2]).                                                                                                       |
| `/api/v1/users/me/preferences/{key}`                                                                              | DELETE                                         | Bearer | Removes the preference.                              |

## Friends (requires Bearer)

| Endpoint                                       | Method | Auth   | Payload / Notes                                                      |
| ---------------------------------------------- | ------ | ------ | -------------------------------------------------------------------- |
| `/api/v1/friends`                              | GET    | Bearer | Lists your friends.                                                  |
| `/api/v1/friends/batch`                        | GET    | Bearer | Returns friends plus sent/received requests.                         |
| `/api/v1/friends/requests?type=`               | GET    | Bearer | `type` can be `incoming`, `outgoing`, or `all` (default `incoming`). |
| `/api/v1/friends/requests`                     | POST   | Bearer | `{"user_id": <id>}` to send a request.                               |
| `/api/v1/friends/requests/{requestId}/accept`  | POST   | Bearer | Accepts the request.                                                 |
| `/api/v1/friends/requests/{requestId}/decline` | POST   | Bearer | Declines the request.                                                |
| `/api/v1/friends/block`                        | POST   | Bearer | `{"user_id": <id>}` to block.                                        |
| `/api/v1/friends/unblock`                      | POST   | Bearer | `{"user_id": <id>}` to unblock.                                      |
| `/api/v1/friends/{userId}`                     | DELETE | Bearer | Removes friend.                                                      |
| ---------------------------------------------- | ------ | ------ | -------------------------------------------------------------------- |

## Achievements (requires Bearer)

| Endpoint                          | Method | Auth   | Payload / Notes                                  |
| --------------------------------- | ------ | ------ | ------------------------------------------------ |
| `/api/v1/achievements`            | GET    | Bearer | Lists all available achievements.                |
| `/api/v1/achievements/users/{id}` | GET    | Bearer | Lists unlocked achievements for a specific user. |

## Agent & overlay uploads (Admin)

| Endpoint               | Method | Auth           | Payload / Notes                                                                                               | -------- |
| ---------------------- | ------ | -------------- | ------------------------------------------------------------------------------------------------------------- | -------- |
| `/agent/upload`        | POST   | Bearer (ADMIN) | Multipart `file` (.jar). Returns storage path + `agent_hash` (also accepts legacy `/agent/upload/windows      | linux`). |
| `/overlay/upload/{os}` | POST   | Bearer (ADMIN) | `os` `windows` or `linux`. Multipart `file` (.dll for Windows, .so for Linux). Returns path + `overlay_hash`. |

## Admin operations

### Stats & users

| Endpoint                   | Method | Auth           | Payload / Notes                                               |
| -------------------------- | ------ | -------------- | ------------------------------------------------------------- |
| `/api/v1/admin/stats`      | GET    | Bearer (ADMIN) | Returns counts: `users`, `news`, `clients`.                   |
| `/api/v1/admin/news`       | GET    | Bearer (ADMIN) | Returns all news rows.                                        |
| `/api/v1/admin/users`      | GET    | Bearer (ADMIN) | Lists users (`UserAdminResponse`).                            |
| `/api/v1/admin/users/{id}` | GET    | Bearer (ADMIN) | Returns `AdminUserDetailResponse` with profile, social links, |
| preferences.               |
| `/api/v1/admin/users/{id}` | PUT    | Bearer (ADMIN) | Body: `username`, `enabled` (boolean), `role` (               |
| `USER`                     |

`ADMIN`), `nickname`, `avatarPath`, `profileRole` (`ProfileRole`), `socialLinks` (list of `{id?, platform, url}`),
`preferences` (list of `{key, value}` where `value` is JSON). |
| `/api/v1/admin/status` | GET | Bearer (ADMIN) | Returns `ApiResponse` with detailed server info: project name,
version, environment, current timestamp, `started_at`, `uptime_seconds`, and subsystem checks (database, redis,
storage). Each check exposes `status`, `detail`, and `info` (e.g., DB URL redacted + latency/version, Redis
host/port/latency/ping, storage path + space). |

### Clients

| Endpoint                     | Method | Auth           | Payload / Notes                                                                                                                                             |
| ---------------------------- | ------ | -------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `/api/v1/admin/clients`      | GET    | Bearer (ADMIN) | Lists clients (`ClientResponse`).                                                                                                                           |
| `/api/v1/admin/clients/{id}` | GET    | Bearer (ADMIN) | Returns `ClientResponse`.                                                                                                                                   |
| `/api/v1/admin/clients`      | POST   | Bearer (ADMIN) | `AdminClientRequest` with `name`, `version`, `filename`, `md5Hash`, `size`, `mainClass`, `show`, `working`, optional `launches`, `downloads`, `clientType`. |
| `/api/v1/admin/clients/{id}` | PUT    | Bearer (ADMIN) | Same request body as create.                                                                                                                                |
| `/api/v1/admin/clients/{id}` | DELETE | Bearer (ADMIN) | Deletes client (204 or 404).                                                                                                                                |

### Client screenshots

| Endpoint                                                      | Method | Auth           | Payload / Notes                                                |
| ------------------------------------------------------------- | ------ | -------------- | -------------------------------------------------------------- |
| `/api/v1/admin/clients/{clientId}/screenshots`                | GET    | Bearer (ADMIN) | Returns items `[{id, imageUrl, sortOrder}]`.                   |
| `/api/v1/admin/clients/{clientId}/screenshots`                | POST   | Bearer (ADMIN) | Multipart list `file`. Uploads screenshots, returns new items. |
| `/api/v1/admin/clients/{clientId}/screenshots/{screenshotId}` | DELETE | Bearer (ADMIN) | Deletes screenshot (verifies matching client).                 |
| `/api/v1/admin/clients/{clientId}/screenshots/order`          | PUT    | Bearer (ADMIN) | Body is JSON array of screenshot IDs in the desired order.     |

### Fabric dependencies

| Endpoint                                               | Method | Auth           | Payload / Notes                                                                                         |
| ------------------------------------------------------ | ------ | -------------- | ------------------------------------------------------------------------------------------------------- |
| `/api/v1/admin/clients/{clientId}/fabric-deps`         | GET    | Bearer (ADMIN) | Lists dependencies for fabric clients (client must be type `fabric`).                                   |
| `/api/v1/admin/clients/{clientId}/fabric-deps`         | POST   | Bearer (ADMIN) | `{"name": string, "md5Hash": string, "size": number}`. Adds new dependency; duplicates (name) rejected. |
| `/api/v1/admin/clients/{clientId}/fabric-deps/{depId}` | PUT    | Bearer (ADMIN) | Same body as add. Updates existing dependency.                                                          |
| `/api/v1/admin/clients/{clientId}/fabric-deps/{depId}` | DELETE | Bearer (ADMIN) | Removes dependency.                                                                                     |

### Forge dependencies

| Endpoint                                              | Method | Auth           | Payload / Notes                                                                                         |
| ----------------------------------------------------- | ------ | -------------- | ------------------------------------------------------------------------------------------------------- |
| `/api/v1/admin/clients/{clientId}/forge-deps`         | GET    | Bearer (ADMIN) | Lists dependencies for forge clients (client must be type `forge`).                                     |
| `/api/v1/admin/clients/{clientId}/forge-deps`         | POST   | Bearer (ADMIN) | `{"name": string, "md5Hash": string, "size": number}`. Adds new dependency; duplicates (name) rejected. |
| `/api/v1/admin/clients/{clientId}/forge-deps/{depId}` | PUT    | Bearer (ADMIN) | Same body as add. Updates existing dependency.                                                          |
| `/api/v1/admin/clients/{clientId}/forge-deps/{depId}` | DELETE | Bearer (ADMIN) | Removes dependency.                                                                                     |
