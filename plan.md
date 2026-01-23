# Plan Projektu Fuzzel - Klient Android dla Fizzy

## 1. Aktualny Stan Projektu

### Architektura
- **Platforma**: Android Native (Kotlin + Jetpack Compose)
- **Wzorzec**: Clean Architecture (Domain, Data, Presentation)
- **DI**: Hilt
- **Networking**: Retrofit 2.9.0 + Moshi + OkHttp 4.12.0
- **UI**: Material Design 3, Jetpack Compose
- **Min SDK**: 26, Target SDK: 34

### Zaimplementowane Funkcjonalności

| Obszar | Status | Uwagi |
|--------|--------|-------|
| Autentykacja Magic Link | ✅ Gotowe | Pełny flow email → kod → token |
| Personal Access Token | ✅ Gotowe | Bezpośrednie logowanie tokenem |
| Multi-account | ✅ Gotowe | Encrypted storage, przełączanie kont |
| Lista boardów | ✅ Gotowe | CRUD, wyszukiwanie |
| Widok Kanban | ✅ Gotowe | Kolumny, karty, drag&drop |
| Swimlanes (Triage/Not Now/Done) | ✅ Gotowe | Wirtualne kolumny |
| Karty CRUD | ✅ Gotowe | Tworzenie, edycja, usuwanie |
| Akcje na kartach | ✅ Gotowe | Close/reopen, priority, watch, triage, defer |
| Kolumny CRUD | ✅ Gotowe | Dodawanie, edycja, usuwanie |
| Steps | ✅ Gotowe | CRUD + toggle completion |
| Comments | ✅ Gotowe | CRUD |
| Reactions | ⚠️ Częściowe | Add działa, remove ma bug (emoji vs reactionId) |
| Tags | ⚠️ Częściowe | Wyświetlanie OK, add/remove wymaga poprawy |
| Assignees | ⚠️ Częściowe | Add działa, remove niekompletne |
| Notifications | ✅ Gotowe | Lista, mark read/unread, bulk |
| Settings | ✅ Gotowe | Theme mode |

---

## 2. Brakujące Funkcjonalności vs API

### 2.1 Nieimplementowane Endpointy API

| Endpoint | Opis | Priorytet |
|----------|------|-----------|
| `GET /cards?tag_ids[]=...` | Filtrowanie kart po tagach | Wysoki |
| `GET /cards?assignee_ids[]=...` | Filtrowanie kart po assignees | Wysoki |
| `GET /cards?terms[]=...` | Wyszukiwanie kart tekstowe | Wysoki |
| `GET /cards?creation=...` | Filtrowanie po dacie utworzenia | Średni |
| `GET /cards?closure=...` | Filtrowanie po dacie zamknięcia | Średni |
| `DELETE /cards/{n}/image` | Usuwanie obrazka nagłówkowego karty | Niski |
| `GET /:account/users` | Lista użytkowników konta | Wysoki |
| `GET /:account/users/{id}` | Szczegóły użytkownika | Średni |
| `PUT /:account/users/{id}` | Aktualizacja profilu użytkownika | Niski |
| Paginacja (`Link` header) | Obsługa dużych zbiorów danych | Wysoki |
| ETag caching | Optymalizacja requestów (304 Not Modified) | Średni |
| File uploads (multipart) | Załączniki, avatary, obrazki | Wysoki |
| Rich text (HTML + ActionText) | Formatowany opis karty/komentarza | Średni |

### 2.2 Bugi i Niekompletne Implementacje

| Problem | Lokalizacja | Opis |
|---------|-------------|------|
| Reakcje - usuwanie | `CardDetailViewModel` | Używa emoji zamiast reactionId |
| Tagi - dodawanie | `CardDetailViewModel` | Używa tagId zamiast prawidłowego lookup |
| Assignees - usuwanie | `CardRepository` | Endpoint niejasny w dokumentacji, stub |
| Reakcje - GET | `FizzyApiService` | Brak endpointu do pobierania listy reakcji |

---

## 3. Wymagania dla Parytetu z Wersją Webową (1:1)

### 3.1 UI - Widok Kanban

| Element | Status | Do zrobienia |
|---------|--------|--------------|
| Kolumny z kartami | ✅ | - |
| Drag & drop kart | ✅ | - |
| Triage swimlane | ✅ | - |
| Not Now swimlane | ✅ | - |
| Done swimlane | ✅ | - |
| Kolory kolumn (user-defined) | ⚠️ | Dodać obsługę `color` z API |
| Limity kart w kolumnie (WIP) | ❌ | API może to wspierać |
| Reordering kolumn | ❌ | Brak w API (sprawdzić) |
| Bulk card actions | ❌ | Zaznaczanie wielu kart |

### 3.2 UI - Szczegóły Karty

| Element | Status | Do zrobienia |
|---------|--------|--------------|
| Tytuł + opis | ✅ | - |
| Status badge | ✅ | - |
| Priority (golden) | ✅ | - |
| Watch | ✅ | - |
| Tags | ⚠️ | Poprawić add/remove |
| Assignees + avatary | ⚠️ | Poprawić remove, dodać prawdziwe avatary |
| Steps z progress | ✅ | - |
| Comments | ✅ | - |
| Reactions na comments | ⚠️ | Naprawić usuwanie |
| Activity log | ❌ | Placeholder w UI |
| Obrazek nagłówkowy | ❌ | Brak obsługi |
| Załączniki | ❌ | Brak obsługi |
| Rich text rendering | ❌ | Tylko plain text |
| Due date | ❌ | Sprawdzić czy API wspiera |
| Triage date display | ⚠️ | API bierze column_id, nie date |

### 3.3 UI - Board List

| Element | Status | Do zrobienia |
|---------|--------|--------------|
| Lista boardów | ✅ | - |
| Wyszukiwanie | ✅ | - |
| Tworzenie/edycja/usuwanie | ✅ | - |
| Board stats | ⚠️ | Pokazuje tylko podstawowe info |
| Board members | ❌ | Nie wyświetla użytkowników |
| Sortowanie | ❌ | Brak opcji sortowania |

### 3.4 UI - Notifications

| Element | Status | Do zrobienia |
|---------|--------|--------------|
| Lista powiadomień | ✅ | - |
| Mark read/unread | ✅ | - |
| Mark all read | ✅ | - |
| Notification grouping | ❌ | Brak grupowania |
| Deep link do karty | ⚠️ | Podstawowa nawigacja |

---

## 4. Plan Implementacji - Priorytety

### Faza 1: Naprawy Krytyczne (Wysoki Priorytet)

1. **Naprawa reakcji**
   - Dodać `GET /comments/{id}/reactions` do pobierania listy z ID
   - Użyć `reaction.id` zamiast `emoji` przy usuwaniu
   - Pliki: `FizzyApiService.kt`, `CommentDto.kt`, `CardDetailViewModel.kt`

2. **Naprawa tagów**
   - Poprawić flow add/remove tag
   - Użyć prawidłowego `tagging_id` przy usuwaniu
   - Pliki: `CardRepository.kt`, `CardDetailViewModel.kt`

3. **Naprawa assignees**
   - Zbadać API dla remove assignment (toggle?)
   - Implementować prawidłowe usuwanie
   - Pliki: `FizzyApiService.kt`, `CardRepository.kt`

### Faza 2: Filtrowanie i Wyszukiwanie

4. **Zaawansowane filtrowanie kart**
   - Dodać parametry query: `tag_ids[]`, `assignee_ids[]`, `terms[]`
   - UI: FilterChips w Kanban toolbar
   - Pliki: `FizzyApiService.kt`, `CardRepository.kt`, `KanbanViewModel.kt`

5. **Wyszukiwanie globalne**
   - Wyszukiwanie po wszystkich boardach
   - UI: SearchBar w głównym ekranie
   - Nowy feature: `feature/search/`

### Faza 3: Paginacja i Caching

6. **Paginacja**
   - Parser dla `Link` header z `rel="next"`
   - Infinite scroll w listach
   - Pliki: `NetworkModule.kt`, wszystkie repozytoria

7. **ETag Caching**
   - OkHttp interceptor dla `If-None-Match`
   - Cache storage
   - Pliki: `NetworkModule.kt`, `AuthInterceptor.kt`

### Faza 4: Rich Content

8. **File Uploads**
   - Multipart request handling
   - Avatary użytkowników (zamiast inicjałów)
   - Obrazki nagłówkowe kart
   - Pliki: `FizzyApiService.kt`, nowy `FileUploadRepository.kt`

9. **Rich Text**
   - HTML rendering w opisach i komentarzach
   - Compose HTML renderer lub WebView
   - Pliki: `CardDetailScreen.kt`, nowy `RichTextContent.kt`

### Faza 5: Parytet UI

10. **Activity Log**
    - Nowy endpoint (jeśli dostępny w API)
    - Timeline UI w CardDetailScreen
    - Pliki: `CardDetailScreen.kt`, `CardDetailViewModel.kt`

11. **Board Members**
    - Wyświetlanie użytkowników boarda
    - Zarządzanie dostępem
    - Pliki: `BoardDetailScreen.kt` (nowy), `UserRepository.kt`

12. **Offline Support**
    - Room database dla cache
    - Sync queue dla operacji offline
    - Nowy moduł: `data/local/database/`

---

## 5. Szczegóły Techniczne API

### Struktura URL
```
{base_url}/{account_slug}/{resource}.json
```

### Autentykacja
- Header: `Authorization: Bearer {token}`
- Session token lub Personal Access Token

### Parametry Listowania Kart
```
GET /cards.json?
  board_ids[]=uuid1&board_ids[]=uuid2    # filtr boardów
  tag_ids[]=uuid1&tag_ids[]=uuid2        # filtr tagów
  assignee_ids[]=uuid1                    # filtr assignees
  indexed_by=all|closed|not_now           # status kart
  creation=today|this_week|last_week      # data utworzenia
  closure=today|this_week|last_week       # data zamknięcia
  terms[]=word1&terms[]=word2             # wyszukiwanie tekstowe
```

### Paginacja
- Response header: `Link: <url>; rel="next"`
- Parsować URL i fetchować następną stronę

### Caching
- Response header: `ETag: "abc123"`
- Request header: `If-None-Match: "abc123"`
- Response 304 = dane niezmienione

### Wrapping Request/Response
```json
// Create request
{ "card": { "title": "...", "description": "..." } }

// Response
{ "card": { "id": "...", "number": 123, ... } }
```

---

## 6. Pliki Kluczowe do Modyfikacji

| Plik | Zmiany |
|------|--------|
| `FizzyApiService.kt` | Nowe endpointy, parametry query |
| `CardDto.kt` | Pola dla attachments, obrazki |
| `CardRepository.kt` | Filtrowanie, paginacja |
| `KanbanViewModel.kt` | Filtry UI, bulk actions |
| `CardDetailViewModel.kt` | Naprawy reakcji/tagów |
| `NetworkModule.kt` | ETag interceptor, cache |
| `BoardListScreen.kt` | Sortowanie, search |

---

## 7. Weryfikacja Zmian

### Testy Manualne
1. Zaloguj się przez Magic Link
2. Utwórz board, dodaj kolumny i karty
3. Przetestuj drag & drop między kolumnami
4. Otwórz szczegóły karty - dodaj/usuń tagi, assignees
5. Dodaj komentarz, dodaj/usuń reakcję
6. Zamknij i otwórz ponownie kartę
7. Sprawdź powiadomienia

### Testy API
1. Porównaj response API z DTO mapping
2. Zweryfikuj error handling (401, 403, 404, 422)
3. Sprawdź paginację na dużych zbiorach

### Porównanie z Web
1. Otwórz tę samą tablicę w web i w aplikacji
2. Porównaj wyświetlanie kart 1:1
3. Sprawdź wszystkie akcje dostępne w web

---

## 8. Podsumowanie Priorytetów

| Priorytet | Zadanie | Effort |
|-----------|---------|--------|
| 🔴 Krytyczny | Naprawa reakcji, tagów, assignees | Mały |
| 🟠 Wysoki | Filtrowanie kart, paginacja | Średni |
| 🟡 Średni | ETag caching, rich text | Średni |
| 🟢 Niski | File uploads, activity log, offline | Duży |

---

*Plan utworzony: 2026-01-19*
*Wersja: 1.0*
