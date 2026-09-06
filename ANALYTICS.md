# События аналитики

<!-- Файл собран автоматически: `./gradlew generateAnalyticsRegistry`.
     Руками не правят — источник правды в коде. -->

Имя события целиком — это префикс модуля плюс имя из `track(...)`.


## app · `app/src/main/kotlin/com/slimly/app/AppAnalytics.kt`

| Событие | Параметры | Что значит |
| --- | --- | --- |
| `app_deeplink_opened` | `source`, `target` | Ссылка привела на экран. |
| `app_deeplink_unknown` | `target` | Ссылка есть, а маршрута под неё нет: обычно это ярлык, оставшийся от прошлой версии, — такое видно только по аналитике. |

## compress · `feature/compress/impl/src/main/kotlin/com/slimly/feature/compress/CompressAnalytics.kt`

| Событие | Параметры | Что значит |
| --- | --- | --- |
| `compress_level_picked` | `delta_percent`, `preset` | Степень выбрана, сжатие запущено. |

## flow · `mediaflow/src/main/kotlin/com/slimly/mediaflow/FlowAnalytics.kt`

| Событие | Параметры | Что значит |
| --- | --- | --- |
| `flow_access_answered` | `status`, `tool` | Система ответила на запрос доступа к медиатеке — первая развилка любого инструмента: отказ обрывает флоу на самом входе, и без этого события такой уход выглядел бы как обычный отвал с экрана выбора. |
| `flow_files_picked` | `count`, `input_mb`, `tool` | Файлы выбраны, инструмент пошёл к настройкам. |
| `flow_originals_deleted` | `count`, `tool` | Оригиналы удалены после сохранения: место освобождено по-настоящему. |
| `flow_result_previewed` | `tool` | Открыли просмотр готового файла — результату не поверили на слово. |
| `flow_result_renamed` | `tool` | Готовому файлу поменяли имя. |
| `flow_result_saved` | `count`, `destination`, `output_mb`, `tool` | Готовые файлы сохранены — ради этого затевался весь флоу. |
| `flow_run_cancelled` | `percent`, `tool` | С экрана обработки ушли, не дождавшись конца: уход рвёт поток, и готовых файлов после него не остаётся. |
| `flow_run_finished` | `count`, `duration_ms`, `failed`, `input_mb`, `output_mb`, `tool` | Обработка дошла до конца. |
| `flow_run_started` | `count`, `format`, `input_mb`, `tool` | Обработка началась — настройки приняты, дальше работает устройство. |
| `flow_save_failed` | `count`, `tool` | Часть файлов сохранить не удалось. |

## home · `feature/home/impl/src/main/kotlin/com/slimly/feature/home/HomeAnalytics.kt`

| Событие | Параметры | Что значит |
| --- | --- | --- |
| `home_profile_opened` | — | Открыт профиль — экран статистики и настроек. |
| `home_tool_opened` | `tool` | Инструмент открыт с главной. |

## onboarding · `feature/onboarding/impl/src/main/kotlin/com/slimly/feature/onboarding/OnboardingAnalytics.kt`

| Событие | Параметры | Что значит |
| --- | --- | --- |
| `onboarding_finished` | `page`, `skipped` | Тур закончен. |
| `onboarding_page_opened` | `page` | Открыта следующая страница тура. |

## profile · `feature/profile/impl/src/main/kotlin/com/slimly/feature/profile/ProfileAnalytics.kt`

| Событие | Параметры | Что значит |
| --- | --- | --- |
| `profile_language_settings_opened` | — | Ушли выбирать язык в системные настройки. |
| `profile_statistics_opened` | `source` | Открыт разбор по инструментам. |
| `profile_statistics_range_changed` | `range` | Сменили срез времени. |

## scan · `feature/scan/impl/src/main/kotlin/com/slimly/feature/scan/ScanAnalytics.kt`

| Событие | Параметры | Что значит |
| --- | --- | --- |
| `scan_bounds_confirmed` | `count`, `edited`, `missed` | Границы страниц приняты. |
| `scan_camera_cancelled` | `count` | Из камеры вышли, снятое выброшено. |
| `scan_pages_captured` | `count`, `mode` | Съёмка закончена, страницы уходят дальше. |
| `scan_source_picked` | `source` | Откуда берут страницы. |

## Расхождения с iOS

Не ошибка сама по себе: инструменты приезжают по фазам, а часть
экранов у платформ разная. Но каждая строка здесь — повод
проверить, что событие потеряно намеренно.

- `profile_language_changed` — только на iOS
- `profile_language_settings_opened` — только на Android
