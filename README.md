# Тишина

Простой и надёжный таймер для медитации.

- Длительность сессии: от 5 минут до 8 часов.
- Период звука: от 1 минуты до 4 часов (или без звука).
- Сессия продолжает идти при заблокированном экране и в фоне.
- Управление из уведомления: пауза, продолжение, стоп.

## Сборка

Требуется JDK 17+ (проект фиксирует JDK 25 через Gradle toolchain) и Android SDK.

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Установка на подключённое устройство или эмулятор:

```bash
./gradlew :app:installDebug
```

## Тесты

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

## Статус

В разработке. Звук — заменяемый плейсхолдер.
