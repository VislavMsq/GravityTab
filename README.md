# GravityTap 🎮🐾

Мини-аркада с простой физикой: мяч падает под гравитацией, игрок ловит тайминг, зарабатывает очки и соревнуется с собой. Проект создан как **учебный пример современной Android-разработки** на **Kotlin + Jetpack Compose** с архитектурой **MVVM** и разделением на слои **UI / Domain / Data**.

<p align="center">
  <img src="docs/screen_menu.png" alt="Menu" width="32%">
  <img src="docs/screen_game.png" alt="Game" width="32%">
  <img src="docs/screen_result.png" alt="Result" width="32%">
</p>

---

## 📚 Оглавление

* [Демо](#-демо)
* [Функциональность](#-функциональность)
* [Геймдизайн и механики](#-геймдизайн-и-механики)
* [Технологии](#-технологии)
* [Почему Jetpack Compose](#-почему-jetpack-compose)
* [Архитектура](#-архитектура)
* [Структура проекта](#-структура-проекта)
* [Быстрый старт](#-быстрый-старт)
* [Конфигурация и версии](#-конфигурация-и-версии)
* [Качество кода](#-качество-кода)
* [Доступность и i18n](#-доступность-и-i18n)
* [Как мы закрыли замечания из прошлого ревью](#-как-мы-закрыли-замечания-из-прошлого-ревью)
* [Дорожная карта](#-дорожная-карта)
* [Лицензия](#-лицензия)

---

## 🎥 Демо

* **Splash → Menu → Game → Result** — полный игровой цикл с анимированными переходами.
* Папка `docs/` зарезервирована под скриншоты/GIF (замени плейсхолдеры своими изображениями).

---

## ✨ Функциональность

* 4 экрана: **Splash**, **Menu**, **Game**, **Result**
* **Сложности**: Easy / Normal / Hard (разные параметры скорости/жизней)
* **Физика**: гравитация, ограничение `dt`, столкновение с «землёй»
* **Счёт** и **комбо-множитель**
* **High Score** локально (Room)
* **Настройки** (DataStore): выбранная сложность, звук
* **Адаптивный UI** для телефонов и планшетов (**WindowSizeClass**)
* **Анимации**: появление/исчезновение, scale/alpha в геймплее
* **Защита от дабл-кликов** на навигационных кнопках

---

## 🧩 Геймдизайн и механики

* Игровой цикл: **Start → Progress → Game Over → Result**
* **Падение шара** под гравитацией → успей «поймать» тайминг кликом
* **Усложнение**: с ростом уровня уменьшаются интервалы, возрастает динамика
* **3 промаха** подряд → **Game Over**
* **Результат**: очки, максимальное комбо, сложность → кнопки «Сыграть снова» / «В меню»

---

## 🔧 Технологии

* **Язык**: Kotlin
* **UI**: Jetpack Compose + Material 3
* **Навигация**: Navigation for Compose
* **DI**: Hilt (Dagger)
* **Асинхронность**: Coroutines / Flow
* **Хранение**: Room (High Scores), DataStore (Preferences)
* **Минимальная версия Android**: 24+

---

## 🧱 Почему Jetpack Compose

* Декларативный UI упрощает **управление состоянием** и **анимации**
* **Меньше шаблонного кода**, легче поддерживать и рефакторить
* Хорошо сочетается с **MVVM** и **Unidirectional Data Flow** (состояние → UI)

---

## 🏗 Архитектура

**MVVM + слои:**

```
UI (Compose Screens, ViewModel)
        ↓
DOMAIN (UseCases, Reducer, Game Loop, Models)
        ↓
DATA (Repositories + Sources: Room, DataStore)
```

* **UI** — только отображение и события пользователя. Состояние хранится во **ViewModel** (переживает поворот/убийство процесса).
* **Domain** — «мозг» игры: чистая логика, юзкейсы, редьюсер, модели (без Android API).
* **Data** — реализация репозиториев (Room, DataStore). Инфраструктура изолирована от бизнес-логики.
* **DI** — Hilt модули: `DataModule` (Room, DataStore), `RepoModule`, `UseCaseModule`.

---

## 🗂 Структура проекта

```
app/
 └─ src/main/java/com/mosiuk/gravitytap/
    ├─ App.kt                         // @HiltAndroidApp
    ├─ MainActivity.kt                // setContent + AppNavHost
    ├─ core/
    │   ├─ nav/                       // AppNavHost, Route helpers
    │   ├─ di/                        // Hilt модули: Data/Repo/UseCase/App
    │   └─ util/                      // ClickThrottle, DispatchersProvider
    ├─ data/
    │   ├─ db/                        // Room: AppDatabase, Dao, Entities
    │   ├─ datastore/                 // SettingsDataStore (через DI)
    │   └─ repo/                      // *RepositoryImpl
    ├─ domain/
    │   ├─ game/                      // Reducer, Loop, Spawner, Actions/Effects
    │   ├─ model/                     // Difficulty, GameState, ScoreEntry
    │   └─ usecase/                   // TickPhysics, Save/GetHighScores, Settings
    └─ ui/
        ├─ splash/                    // SplashScreen + VM
        ├─ menu/                      // MenuScreen + VM
        ├─ game/                      // GameScreen + VM
        ├─ result/                    // ResultScreen + VM
        ├─ theme/                     // Material 3 тема
        └─ responsive/                // UiSizes, WindowSizeClass utils

res/
 └─ values/strings.xml                // ВСЕ строки + labels для сложностей
```

---

## 🚀 Быстрый старт

1. **Открыть** в Android Studio (Arctic/Narwhal и новее)
2. **Собрать**:

```bash
./gradlew :app:assembleDebug
```

3. **Запустить** на эмуляторе/устройстве (Android 7.0+, API 24+)
4. **Линтеры и статика**:

```bash
./gradlew ktlintCheck
./gradlew detekt
```

5. **Тесты** (если добавите модульные/инструментальные):

```bash
./gradlew test
./gradlew connectedAndroidTest
```

---

## ⚙️ Конфигурация и версии

* Все версии и координаты библиотек вынесены в **`gradle/libs.versions.toml`** (**Version Catalog**).
* **DataStore** поставляется через **Hilt** (один источник правды), без локальных делегатов.
* **Room** — локальное хранилище рекордов.
* **Многократные клики** на навигационных кнопках «душатся» `ClickThrottle`.

---

## ✅ Качество кода

* **Строки**: все UI-строки в `res/values/strings.xml` (готово для локализаций)
* **Стейт-менеджмент**: ViewModel + SavedStateHandle → **нет потери состояния** при повороте
* **Разделение ответственности**: бизнес-логика в `domain`, инфраструктура в `data`, UI в `ui`
* **CI-friendly**: детерминируемые задачи `ktlint`, `detekt`, `assemble`

---

## ♿ Доступность и i18n

* Тексты из ресурсов, поддержка динамического размера шрифтов Material 3
* Готовность к локализации (`values-ru/`, `values-uk/` можно добавить при необходимости)
* Контраст и размеры контролов учитывают **WindowSizeClass** (планшеты/телефоны)

---
## 🗺 Дорожная карта

* 🔊 Включить реальные звуки (SoundPool/MediaPlayer) по свитчу «Sound»
* 🌍 Добавить локализации `ru`/`uk` для ключевых строк
* 🧪 Покрыть логические части unit-тестами (Reducer, UseCases)
* 📈 Экран High Scores с сортировкой/фильтрами, пустыми состояниями и превью
* 🧾 Небольшой **in-app** tutorial (первый запуск)

---

## 📜 Лицензия

MIT / Apache-2.0 — на выбор. Добавьте файл `LICENSE` при публикации.
