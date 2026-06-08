# Planner - Android 시간 기록 앱

날짜별로 시간을 기록하고, 월간 통계와 목표를 관리할 수 있는 Android 앱입니다.

## 주요 기능

### 📝 기록 (Main)
- 날짜 선택하여 항목 추가/삭제
- 6개 카테고리: Health, Mind, Family, Language, Finance, Technology
- 시간(h) + 분(m) 단위 입력
- 메모 추가 가능

### 📊 통계 (Stats)
- 월별 네비게이션 (← →)
- 도넛 차트로 카테고리별 비율 시각화
- 카테고리별 상세 시간 및 퍼센티지
- 총 기록 시간 표시

### 🎯 목표 (Goals)
- 카테고리별 월 목표 시간 설정
- 실시간 진행률 바
- 목표 달성 시 표시

## 기술 스택

| 항목 | 사용 기술 |
|------|----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| Database | Room (SQLite) |
| DI | Hilt 미사용 (AndroidViewModel) |
| Navigation | Navigation Compose |
| Build | Gradle KTS + AGP 8.2.2 |
| CI/CD | GitHub Actions |

## 프로젝트 구조

```
app/src/main/java/com/planner/tracker/
├── data/                    # 데이터 계층
│   ├── Entry.kt            # 시간 기록 엔티티 + Category enum
│   ├── Goal.kt             # 목표 엔티티
│   ├── EntryDao.kt         # 기록 DAO
│   ├── GoalDao.kt          # 목표 DAO
│   ├── AppDatabase.kt      # Room Database
│   └── Repository.kt       # 리포지토리 + 날짜 유틸
├── viewmodel/
│   └── PlannerViewModel.kt # 상태 관리
├── ui/
│   ├── theme/              # 테마 (Color, Type, Theme)
│   ├── components/         # 공통 컴포넌트
│   └── screens/            # 화면 (Main, Stats, Goals)
├── MainActivity.kt         # Bottom Navigation
└── PlannerApp.kt           # Application
```

## 빌드 방법

### Android Studio
1. `File > Open` → 프로젝트 폴더 선택
2. Gradle Sync 완료 후 `Run` (▶)

### 명령줄
```bash
# Gradle Wrapper 생성 (최초 1회)
gradle wrapper

# Debug APK 빌드
./gradlew assembleDebug

# Release APK 빌드
./gradlew assembleRelease
```

### GitHub Actions
`main` 브랜치에 push 시 자동 빌드:
- Debug APK → artifact 업로드
- Release APK (main push 전용) → artifact 업로드

## 요구 사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34
- Gradle 8.5

## 라이선스

MIT
