# Fuzzel

Fuzzel is an Android client for [Fizzy](https://fizzy.io), the card-based project management tool from 37signals.

## Building

```bash
./gradlew assembleDebug
```

## Architecture

The app follows Clean Architecture with the following layers:

- **Domain** - Business logic, models, and repository interfaces
- **Data** - API services, DTOs, and repository implementations
- **Presentation** - ViewModels and Compose UI

## API Integration

The app integrates with the Fizzy API using the following key endpoints:

### Authentication
- `POST /session` - Request magic link
- `POST /session/magic_link` - Verify magic link code
- Personal Access Token support via `GET /my/identity.json`

### Resources
- **Boards**: CRUD operations at `/boards`
- **Cards**: Operations use card `number` (not ID) at `/cards/{cardNumber}`
- **Card Actions**: Separate endpoints for close (`/closure`), triage (`/triage`), priority (`/goldness`), watch (`/watch`)
- **Tags**: Account-level tags at `/tags`, card taggings at `/cards/{cardNumber}/taggings`
- **Comments**: Nested under cards at `/cards/{cardNumber}/comments`
- **Steps**: Nested under cards at `/cards/{cardNumber}/steps`
- **Notifications**: Mark read via `POST /notifications/{id}/reading`

## Tech Stack

- Kotlin
- Jetpack Compose
- Hilt (DI)
- Retrofit + Moshi
- Coroutines + Flow
