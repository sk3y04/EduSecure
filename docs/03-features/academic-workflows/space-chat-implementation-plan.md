# Space Chat Implementation Plan

## 0. Implementation status snapshot

Status as of 2026-04-11: the MVP described in this plan has now been implemented in the repository.

Implemented highlights:
- backend chat endpoints under `/api/spaces/{spaceId}/chat/messages`
- shared authorization reuse via `SpaceAccessService`
- MongoDB-backed `SpaceChatMessage` persistence gated behind `app.chat.enabled=true`
- metadata-only audit action `SPACE_CHAT_MESSAGE_CREATED`
- Vue `SpaceChatPanel` embedded in `frontend/src/pages/SpaceDetail/index.vue`
- optional local MongoDB development through the `chat` profile in `compose.yaml`

Current repository note:
- the default application and test startup path still works with chat disabled
- the backend test suite validates chat behavior through focused service-level tests and keeps the broader suite stable without requiring MongoDB for every test run

## 1. Feature goal

Introduce a per-space shared chat so users can communicate inside a `Space`.

Scope for the first delivery:
- one shared chat room per space
- authorized users can read existing chat history for that space
- authorized users can post new messages to that space unless the space is archived
- chat messages are stored in MongoDB
- existing users, spaces, memberships, auth, and audit infrastructure remain in PostgreSQL via JPA/Liquibase

This feature must integrate with the current backend-issued `HttpOnly` cookie authentication model and existing CSRF protections already used by the SPA.

## 2. Repository context and inspected anchors

This plan is based on the current repository structure and these implementation anchors:

### 2.1 Backend
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- `backend/src/main/java/edusecure/edusecure/controller/common/ApiExceptionHandler.java`
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceController.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
- `backend/src/main/java/edusecure/edusecure/repository/space/SpaceRepository.java`
- `backend/src/main/java/edusecure/edusecure/repository/space/SpaceMembershipRepository.java`
- `backend/src/main/java/edusecure/edusecure/entity/space/Space.java`
- `backend/src/main/java/edusecure/edusecure/entity/space/SpaceMembership.java`
- `backend/src/main/java/edusecure/edusecure/entity/auth/User.java`
- `backend/src/main/java/edusecure/edusecure/entity/audit/AuditActionType.java`
- `backend/src/test/java/edusecure/edusecure/SpaceFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

### 2.2 Frontend
- `frontend/package.json`
- `frontend/src/services/http.ts`
- `frontend/src/services/spaces.ts`
- `frontend/src/stores/auth.ts`
- `frontend/src/types/space.ts`
- `frontend/src/pages/SpaceDetail/index.vue`
- `frontend/src/pages/SpaceDetail/components/index.ts`
- `frontend/src/router/index.ts`

### 2.3 Runtime and local development
- `compose.yaml`
- existing docs in `docs/03-features/academic-workflows/space-management-technical-specification.md`

## 3. Current-state summary

### 3.1 Current space authorization rules

Current space access is implemented in `SpaceService` and should remain the source of truth for authorization behavior:
- `ADMIN` can list and manage any space
- `LECTURER` can list and manage spaces they created
- `STUDENT` can list and view only spaces where a membership exists for their user id
- student viewers do not receive the full roster in `SpaceDetailResponse`
- archived spaces are already a real lifecycle state and currently reject new membership additions

### 3.2 Current backend platform shape

The backend currently uses:
- Spring Boot
- Spring Web MVC
- Spring Security
- Spring Validation
- Spring Data JPA
- Spring Data MongoDB
- Liquibase
- PostgreSQL

MongoDB is now configured as an optional datastore for space chat only.

Authentication and CSRF behavior already exist and should be reused for chat:
- auth is cookie-based
- the SPA sends cookies with `withCredentials`
- unsafe requests use the existing `XSRF-TOKEN` / `X-XSRF-TOKEN` flow
- all non-public endpoints are already protected by Spring Security

### 3.3 Current frontend shape

The frontend currently uses:
- Vue 3
- Vite
- TypeScript
- Axios
- Pinia
- Vue Router

`SpaceDetail/index.vue` already acts as the main in-space screen. It loads the target space, displays metadata, embeds the assignment workspace, and conditionally shows management controls for staff.

This makes `SpaceDetail` the correct place to embed the MVP chat UI.

## 4. Product and architecture decisions

### 4.1 Shared chat model

Each `Space` has exactly one shared chat.

There is no need for a separate room-management feature in MVP because room identity already exists implicitly through `Space.id` in PostgreSQL.

### 4.2 MongoDB scope

MongoDB will store only chat messages.

PostgreSQL remains the source of truth for:
- users
- roles
- spaces
- memberships
- auth/session-related data
- audit logs

MongoDB must not duplicate space memberships or authorization state.

### 4.3 Archived-space behavior

Recommended MVP rule:
- archived spaces remain readable
- archived spaces are not writable

Rationale:
- this matches the current meaning of archive in `Space`
- it preserves historical context while preventing continued activity in a retired space
- it is simpler and safer than keeping archived spaces fully active

### 4.4 MVP transport choice

Recommended MVP transport:
- REST endpoints with client polling

Do not start with WebSocket/STOMP in MVP.

Rationale:
- the current application is strongly REST-oriented
- Spring Web MVC endpoints already fit the current architecture
- polling is lower risk than introducing a real-time broker protocol in the first iteration
- the existing auth and CSRF model already works naturally with REST

Possible later enhancement:
- add SSE for server-to-client updates while keeping message creation on REST

## 5. Data model proposal

### 5.1 MongoDB message document

Collection name:
- `space_chat_messages`

Suggested document fields:
- `id: UUID`
- `spaceId: UUID`
- `authorUserId: UUID`
- `authorDisplayName: String`
- `body: String`
- `createdAt: Instant`

Notes:
- `spaceId` references `Space.id` from PostgreSQL at the application level
- `authorDisplayName` is stored as a snapshot to avoid repeated live resolution for each message render
- no membership data is stored in MongoDB

### 5.2 Indexes

Required MVP compound index:
- `(spaceId ASC, createdAt DESC, id DESC)`

Purpose:
- efficient latest-message retrieval by space
- deterministic cursor pagination

### 5.3 Retention and mutation rules

MVP assumptions:
- append-only messages
- no edit
- no delete
- no attachments
- no reactions
- no moderation workflow yet

Future enhancements may add:
- moderator delete or soft-delete
- abuse reporting
- retention policies
- attachment support

## 6. Authorization and security model

### 6.1 Source of truth

Authorization must stay relational and must be enforced using existing PostgreSQL-backed repositories and service logic.

MongoDB must not become a second source of truth for:
- membership
- lecturer ownership
- archive state
- admin privileges

### 6.2 Read access rules

Chat read access should match current space read access:
- admin can read any space chat
- lecturer can read chat for spaces they own
- student can read chat only for spaces they belong to

### 6.3 Write access rules

Chat write access should follow the same space access rules as read, with one additional rule:
- posting is blocked when `space.archived == true`

### 6.4 Security and privacy requirements

The implementation must:
- reuse existing authentication and session handling
- reuse existing CSRF handling for unsafe requests
- validate message content server-side
- render message content as plain text in the frontend
- avoid `v-html` and any raw HTML rendering
- avoid leaking chat content into audit logs

## 7. Backend implementation plan

### 7.1 Packages and classes to add

#### Config
- `backend/src/main/java/edusecure/edusecure/config/chat/SpaceChatProperties.java`
- `backend/src/main/java/edusecure/edusecure/config/chat/SpaceChatMongoConfiguration.java`

#### Mongo document
- `backend/src/main/java/edusecure/edusecure/document/spacechat/SpaceChatMessage.java`

#### Repository
- `backend/src/main/java/edusecure/edusecure/repository/spacechat/SpaceChatMessageRepository.java`

#### DTOs
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/CreateSpaceChatMessageRequest.java`
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/SpaceChatMessageResponse.java`
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/SpaceChatMessagePageResponse.java`

#### Services
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceAccessContext.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceAccessService.java`
- `backend/src/main/java/edusecure/edusecure/service/spacechat/SpaceChatService.java`

#### Controller
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceChatController.java`

### 7.2 Existing files likely to change
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `backend/src/test/resources/application.properties`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
- `backend/src/main/java/edusecure/edusecure/entity/audit/AuditActionType.java`
- optionally `compose.yaml`

### 7.3 Backend responsibilities

#### `SpaceAccessService`
Purpose:
- centralize shared access rules already used by `SpaceService`
- prevent authorization drift between space endpoints and chat endpoints

Suggested methods:
- `requireReadableSpace(String currentUserEmail, UUID spaceId)`
- `requireManageableSpace(String currentUserEmail, UUID spaceId)`
- `requireWritableChatSpace(String currentUserEmail, UUID spaceId)`

`SpaceAccessContext` should include:
- `User currentUser`
- `Space space`
- `boolean canManage`
- `boolean isMember`

#### `SpaceChatService`
Purpose:
- authorize requests using relational state
- list paginated messages from MongoDB
- create new messages in MongoDB
- map document data into API responses
- translate Mongo exceptions into safe client-facing errors

#### `SpaceChatController`
Purpose:
- expose REST endpoints under `/api/spaces/{spaceId}/chat/messages`
- keep request handling thin and delegate business logic to `SpaceChatService`

### 7.4 Backend API design

All endpoints are under `/api/spaces/{spaceId}/chat` and require authentication.

#### 7.4.1 List messages
`GET /api/spaces/{spaceId}/chat/messages`

Query parameters:
- `limit` optional, default `30`, max `100`
- `beforeCreatedAt` optional cursor timestamp
- `beforeMessageId` optional cursor tie-breaker

Behavior:
- no cursor returns the latest page
- cursor returns older messages
- response should be deterministic even for equal timestamps
- response items should be returned in oldest-to-newest order within the batch to simplify UI rendering

Suggested response shape:

```json
{
  "items": [
    {
      "id": "uuid",
      "spaceId": "uuid",
      "authorUserId": "uuid",
      "authorDisplayName": "Student Example",
      "body": "Can someone confirm the deadline?",
      "createdAt": "2026-04-11T12:34:56.789Z"
    }
  ],
  "hasMore": true,
  "nextCursorBeforeCreatedAt": "2026-04-11T12:34:56.789Z",
  "nextCursorBeforeMessageId": "uuid"
}
```

Failure cases:
- `401` unauthenticated
- `403` not authorized for that space
- `404` space not found
- `400` invalid pagination parameters
- `503` chat backend temporarily unavailable

#### 7.4.2 Create message
`POST /api/spaces/{spaceId}/chat/messages`

Request body:

```json
{
  "body": "Can someone confirm the deadline?"
}
```

Rules:
- user must already be allowed to access the space
- archived spaces reject writes
- message is trimmed before persistence
- blank-after-trim messages are rejected
- sender display name is captured from the current user profile

Response `201 Created`:

```json
{
  "id": "uuid",
  "spaceId": "uuid",
  "authorUserId": "uuid",
  "authorDisplayName": "Student Example",
  "body": "Can someone confirm the deadline?",
  "createdAt": "2026-04-11T12:34:56.789Z"
}
```

Failure cases:
- `400` invalid request body
- `401` unauthenticated
- `403` not authorized for that space
- `404` space not found
- `409` space archived and chat is read-only
- `503` chat backend temporarily unavailable

### 7.5 Validation and error handling

Recommended message validation:
- required
- trimmed value must not be blank
- maximum length `2000`

Pagination validation:
- limit between `1` and `100`
- `beforeCreatedAt` and `beforeMessageId` should be used together for deterministic paging

Error handling should reuse the current `ApiExceptionHandler` response format where practical.

For MongoDB failures, the client message should be generic, such as:
- `Space chat is temporarily unavailable`

Do not leak internal Mongo connection details.

### 7.6 Audit design

Recommended new audit action:
- `SPACE_CHAT_MESSAGE_CREATED`

Audit payload should contain metadata only, for example:
- `spaceId=<UUID>,messageId=<UUID>,authorUserId=<UUID>,bodyLength=<N>`

Do not include:
- message body
- message excerpt
- sender email

MVP recommendation:
- audit message creation only
- do not audit chat reads

## 8. Frontend implementation plan

### 8.1 Placement in the existing UI

Embed chat directly in `frontend/src/pages/SpaceDetail/index.vue`.

Recommended placement order:
1. space metadata
2. chat panel
3. assignment workspace
4. management forms or read-only explanatory panel

This keeps chat inside the primary space context and avoids introducing a new route for MVP.

### 8.2 Frontend files to add

#### Types
- `frontend/src/types/spaceChat.ts`

#### Service
- `frontend/src/services/spaceChat.ts`

#### Components
- `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- `frontend/src/pages/SpaceDetail/components/SpaceChatMessageList.vue`
- `frontend/src/pages/SpaceDetail/components/SpaceChatComposer.vue`

### 8.3 Existing frontend files likely to change
- `frontend/src/pages/SpaceDetail/index.vue`
- `frontend/src/pages/SpaceDetail/components/index.ts`

No router changes are required for MVP.

### 8.4 Frontend state management

Use local component state for MVP rather than adding a new Pinia store.

State to manage in `SpaceChatPanel`:
- `messages`
- `isLoading`
- `loadError`
- `isLoadingOlder`
- `hasMore`
- `nextCursorBeforeCreatedAt`
- `nextCursorBeforeMessageId`
- `draftBody`
- `isSending`
- `sendError`

`useAuthStore()` may be used only for current-user presentation details, such as styling messages authored by the logged-in user.

### 8.5 Polling strategy

Recommended MVP behavior:
- load latest `30` messages on initial render
- poll every `15` seconds while the page is active
- merge new messages by id to avoid duplicates
- provide a `Load older messages` action for history pagination

### 8.6 UX requirements

#### Loading state
- show a loading state inside the chat panel only
- do not block the rest of `SpaceDetail`

#### Empty state
- display: `No messages yet. Start the conversation.`

#### Send message
- multiline textarea
- send button disabled when body is blank, request is in flight, or the space is archived
- keep the draft if send fails

#### Archived spaces
- show existing messages
- show a read-only notice explaining that new messages are disabled because the space is archived

#### Error states
- show retry action when message loading fails
- preserve already loaded messages during transient polling failures

#### Rendering safety
- render message body as plain text
- use Vue interpolation rather than HTML injection
- preserve line breaks through CSS rather than raw HTML

## 9. MongoDB and PostgreSQL coexistence plan

### 9.1 Configuration strategy

MongoDB must be added without breaking the current PostgreSQL/Liquibase setup.

Recommended properties:
- `app.chat.enabled=${APP_CHAT_ENABLED:false}`
- `spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:}`
- `spring.data.mongodb.database=${SPRING_DATA_MONGODB_DATABASE:edusecure}`
- `app.chat.message-max-length=${APP_CHAT_MESSAGE_MAX_LENGTH:2000}`
- `app.chat.page-size-default=${APP_CHAT_PAGE_SIZE_DEFAULT:30}`
- `app.chat.page-size-max=${APP_CHAT_PAGE_SIZE_MAX:100}`

Recommended startup behavior:
- chat beans should be conditional on `app.chat.enabled=true`
- when chat is disabled, the app should continue to boot exactly as it does today with PostgreSQL and Liquibase only

### 9.2 Local development

When local chat development is needed, `compose.yaml` can be extended with a MongoDB service and backend environment variables for:
- `SPRING_DATA_MONGODB_URI`
- `SPRING_DATA_MONGODB_DATABASE`
- `APP_CHAT_ENABLED=true`

The Mongo service should be additive rather than required for every non-chat workflow.

Current implemented local path:
- `compose.yaml` now includes a `mongodb` service behind `profiles: ["chat"]`
- set `APP_CHAT_ENABLED=true` when you actually want the backend to expose live Mongo-backed chat behavior
- keep `APP_CHAT_ENABLED=false` for existing non-chat workflows that do not need MongoDB

Example local run:

```powershell
$env:APP_CHAT_ENABLED="true"
$env:SPRING_DATA_MONGODB_URI="mongodb://mongodb:27017/edusecure"
$env:SPRING_DATA_MONGODB_DATABASE="edusecure"
docker compose --profile chat up
```

## 10. Step-by-step implementation order

### Phase 0: lock key decisions
- confirm archived spaces are readable but not writable
- confirm REST polling is the MVP transport
- confirm audit metadata excludes message content
- confirm message max length and display-name behavior

### Phase 1: backend dependency and configuration scaffolding
Modify:
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `backend/src/test/resources/application.properties`

Create:
- `backend/src/main/java/edusecure/edusecure/config/chat/SpaceChatProperties.java`
- `backend/src/main/java/edusecure/edusecure/config/chat/SpaceChatMongoConfiguration.java`

Acceptance criteria:
- existing app boots with `app.chat.enabled=false`
- existing tests remain unaffected
- PostgreSQL/Liquibase startup is unchanged

### Phase 2: shared authorization extraction
Create:
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceAccessContext.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceAccessService.java`

Modify:
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`

Acceptance criteria:
- existing space behavior remains unchanged
- `SpaceFlowIntegrationTests` still pass
- chat can reuse the exact same access rules

### Phase 3: message document, repository, and DTOs
Create:
- `backend/src/main/java/edusecure/edusecure/document/spacechat/SpaceChatMessage.java`
- `backend/src/main/java/edusecure/edusecure/repository/spacechat/SpaceChatMessageRepository.java`
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/CreateSpaceChatMessageRequest.java`
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/SpaceChatMessageResponse.java`
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/SpaceChatMessagePageResponse.java`

Acceptance criteria:
- message schema is defined in Mongo only
- index strategy supports deterministic pagination
- no room collection is introduced

### Phase 4: backend service and controller
Create:
- `backend/src/main/java/edusecure/edusecure/service/spacechat/SpaceChatService.java`
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceChatController.java`

Modify:
- `backend/src/main/java/edusecure/edusecure/entity/audit/AuditActionType.java`

Acceptance criteria:
- `GET /api/spaces/{spaceId}/chat/messages` works with pagination
- `POST /api/spaces/{spaceId}/chat/messages` creates messages
- archived spaces reject writes
- unauthorized users cannot read or write
- audit logging does not leak content

### Phase 5: backend integration tests
Create:
- `backend/src/test/java/edusecure/edusecure/SpaceChatIntegrationTests.java`

Acceptance criteria:
- tests cover member access, non-member rejection, archived behavior, validation, and pagination
- tests validate metadata-only auditing
- tests verify PostgreSQL and MongoDB can coexist

### Phase 6: frontend chat API and types
Create:
- `frontend/src/types/spaceChat.ts`
- `frontend/src/services/spaceChat.ts`

Acceptance criteria:
- chat requests reuse the existing shared Axios client
- GET requests use current cookie auth
- POST requests naturally reuse CSRF behavior from `http.ts`

### Phase 7: frontend chat components and page integration
Create:
- `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- `frontend/src/pages/SpaceDetail/components/SpaceChatMessageList.vue`
- `frontend/src/pages/SpaceDetail/components/SpaceChatComposer.vue`

Modify:
- `frontend/src/pages/SpaceDetail/components/index.ts`
- `frontend/src/pages/SpaceDetail/index.vue`

Acceptance criteria:
- chat is visible within the existing space detail screen
- users can load, send, and paginate messages
- archived spaces are clearly read-only
- chat failures do not break the rest of the space detail view

### Phase 8: UX and ops polish
Modify as needed:
- `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- `compose.yaml`
- optional operational docs

Acceptance criteria:
- polling avoids duplicate messages
- load-more behavior is stable
- local development can enable Mongo when needed

## 11. File-by-file coding order

### 11.1 Backend
1. `backend/build.gradle`
2. `backend/src/main/resources/application.properties`
3. `backend/src/test/resources/application.properties`
4. `backend/src/main/java/edusecure/edusecure/config/chat/SpaceChatProperties.java`
5. `backend/src/main/java/edusecure/edusecure/config/chat/SpaceChatMongoConfiguration.java`
6. `backend/src/main/java/edusecure/edusecure/service/space/SpaceAccessContext.java`
7. `backend/src/main/java/edusecure/edusecure/service/space/SpaceAccessService.java`
8. `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
9. `backend/src/main/java/edusecure/edusecure/document/spacechat/SpaceChatMessage.java`
10. `backend/src/main/java/edusecure/edusecure/repository/spacechat/SpaceChatMessageRepository.java`
11. `backend/src/main/java/edusecure/edusecure/dto/spacechat/CreateSpaceChatMessageRequest.java`
12. `backend/src/main/java/edusecure/edusecure/dto/spacechat/SpaceChatMessageResponse.java`
13. `backend/src/main/java/edusecure/edusecure/dto/spacechat/SpaceChatMessagePageResponse.java`
14. `backend/src/main/java/edusecure/edusecure/service/spacechat/SpaceChatService.java`
15. `backend/src/main/java/edusecure/edusecure/entity/audit/AuditActionType.java`
16. `backend/src/main/java/edusecure/edusecure/controller/space/SpaceChatController.java`
17. `backend/src/test/java/edusecure/edusecure/SpaceChatIntegrationTests.java`
18. `compose.yaml` if local Mongo enablement is added in the same PR

### 11.2 Frontend
1. `frontend/src/types/spaceChat.ts`
2. `frontend/src/services/spaceChat.ts`
3. `frontend/src/pages/SpaceDetail/components/SpaceChatComposer.vue`
4. `frontend/src/pages/SpaceDetail/components/SpaceChatMessageList.vue`
5. `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
6. `frontend/src/pages/SpaceDetail/components/index.ts`
7. `frontend/src/pages/SpaceDetail/index.vue`
8. optional UX refinement pass in the new chat components

## 12. Testing plan

### 12.1 Backend integration tests

Add scenarios for:
- authorized student member can read empty history
- authorized student member can post and then read history
- lecturer owner can post and read
- admin can post and read
- non-member student receives `403` for read
- non-member student receives `403` for write
- archived space remains readable
- archived space rejects post
- blank message fails validation
- too-long message fails validation
- pagination returns correct order and no duplicates
- membership revoked after messages exist blocks future access
- audit logs contain metadata but not message content
- Mongo outage degrades chat with a controlled error, where practical to test

### 12.2 Frontend verification

At minimum, run:
- type checking
- manual verification of key chat scenarios

Manual scenarios:
- empty chat renders clearly
- user can send a message and see it appear
- archived space shows messages but disables sending
- load older messages works
- polling merges in new content without duplicates
- session expiry during chat requests follows existing auth handling
- chat load failures do not break assignment or space-metadata views

## 13. Risks and tradeoffs

### 13.1 Main risks
- introducing a second datastore into a mainly relational application
- lack of cross-database transactions
- startup/configuration drift if Mongo becomes implicitly required
- pagination edge cases around equal timestamps
- authorization drift if chat re-implements access logic instead of reusing `SpaceService` rules

### 13.2 Key tradeoffs

#### Polling vs real-time transport
- polling is simpler and fits the current architecture
- WebSocket/STOMP is more complex and not justified for MVP
- SSE is a reasonable future enhancement if real-time updates become necessary

#### Sender snapshot vs live user lookup
Recommended MVP choice:
- store `authorDisplayName` snapshot and `authorUserId`

Pros:
- simple rendering
- stable history
- no extra read-time joins

Cons:
- older messages may show an older display name after a profile rename

## 14. Acceptance summary

The feature is considered complete for MVP when:
- a shared chat exists for every space implicitly through `spaceId`
- chat messages are stored in MongoDB only
- space and membership authorization remain in PostgreSQL only
- authorized users can read and post in non-archived spaces
- archived spaces remain readable but not writable
- backend endpoints support pagination and validation
- audit logs do not leak message content
- frontend chat is integrated into `SpaceDetail`
- polling, empty state, error state, and load-more all behave predictably
- existing space behavior and existing PostgreSQL/Liquibase setup remain intact

Implementation note:
- this acceptance summary is now satisfied at MVP level in the repository, with MongoDB remaining optional behind the chat runtime flag/profile

## 15. Open decisions to confirm before coding

- confirm the final message max length
- confirm whether metadata-only auditing of message creation is required in MVP or can be deferred
- confirm whether local Docker Compose should include Mongo immediately or only when the feature branch needs it
- confirm whether current-user visual styling is desired in the first frontend iteration
- confirm whether later work should target SSE as the first real-time enhancement

