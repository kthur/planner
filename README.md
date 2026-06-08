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

## UX / 화면 구성

### 하단 네비게이션
3개 탭으로 구성되어 있으며, 선택한 탭이 강조 표시됩니다.

### 1. 기록 화면 (MainScreen)

```
┌──────────────────────────────┐
│  [2026년 6월 8일 (월)]  [📅] │  ← 날짜 표시 + 달력 아이콘
├──────────────────────────────┤
│  ┌─ 기록 추가 ─────────────┐ │
│  │ Health Mind Family …    │ │  ← 카테고리 선택 (토글 칩)
│  │ [2]시간 [30]분          │ │  ← 숫자 입력 필드
│  │ [메모 (선택사항)]        │ │  ← 텍스트 입력
│  └──────────────────────────┘ │
│                              │
│  기록된 항목                  │
│  ● Health             30분 🗑│  ← 색상 인디케이터 + 삭제 버튼
│  ● Mind               120분 🗑│
│                              │
│                          [➕] │  ← FAB (추가 버튼)
└──────────────────────────────┘
```

**상호작용 흐름:**
1. 상단 날짜를 탭 → DatePicker 다이얼로그 열림
2. 카테고리 칩을 탭하여 선택 (선택된 칩은 불투명/나머지는 반투명)
3. 시간(h)과 분(m)을 숫자 키보드로 입력
4. 메모 입력 (선택)
5. 우측 하단 FAB(➕) 탭 → DB 저장, 입력 필드 리셋
6. 기록된 항목은 카드 형태로 리스트 표시, 삭제 버튼(🗑)으로 제거
7. 기록이 없으면 "이 날짜에 기록된 항목이 없습니다." 안내문 표시

### 2. 통계 화면 (StatsScreen)

```
┌──────────────────────────────┐
│  월간 통계                    │
│    ◀ 2026년 6월 ▶           │  ← 월별 이동 화살표
├──────────────────────────────┤
│  ┌──────────────────────────┐│
│  │   총 420분 (7시간 0분)   ││
│  │                          ││
│  │    ╭──────────╮          ││
│  │   ╱  🍩 도넛   ╲         ││  ← 카테고리별 비율
│  │  │    차트     │         ││
│  │   ╲           ╱          ││
│  │    ╰──────────╯          ││
│  │                          ││
│  │ ● Health        120분  28.6%│
│  │ ● Mind           90분  21.4%│
│  │ ● Family         60분  14.3%│
│  └──────────────────────────┘│
│                              │
│  ┌─ 카테고리별 상세 ────────┐│
│  │ Health  ████████░░ 120분 ││  ← 색상 진행바
│  │ Mind    ██████░░░░  90분 ││
│  │ Family  ████░░░░░░  60분 ││
│  └──────────────────────────┘│
└──────────────────────────────┘
```

**상호작용 흐름:**
1. ◀ / ▶ 화살표로 월 이동 (년도도 자동 변경)
2. 데이터가 없는 월은 안내 카드 표시
3. 도넛 차트는 6개 카테고리 색상으로 자동 분할
4. 하단 진행바로 카테고리별 시간 비교 가능
5. 통계는 해당 월의 모든 날짜 데이터를 집계

### 3. 목표 화면 (GoalsScreen)

```
┌──────────────────────────────┐
│  2026년 6월 목표              │
│  각 카테고리별로 월 목표      │
│  시간을 설정하고 진행 상황을  │
│  확인하세요.                  │
├──────────────────────────────┤
│  ● Health    [수정] [삭제]   │
│  ████████████████░░░░  120/200분│
│  현재: 120분   목표: 200분   │
│                              │
│  ● Mind      [설정]          │
│  목표가 설정되지 않았습니다.  │
│                              │
│  ● Family    [수정] [삭제]   │
│  ████████████████████ 60/60분│
│  ✅ 목표 달성!               │
└──────────────────────────────┘
```

**상호작용 흐름:**
1. 각 카테고리별로 [설정] 버튼 탭 → 다이얼로그에서 목표 시간(시간 단위) 입력
2. 설정 후 [수정]/[삭제] 버튼으로 관리
3. 진행률 바가 실시간으로当月 기록 시간 반영
4. 목표 달성 시 진행률 100% + ✅ 표시
5. 통계 탭과 연동되어 같은 월 데이터 사용

### 색상 시스템 (다크 테마)

| 카테고리 | 색상 | 용도 |
|---------|------|------|
| Health | `#4CAF50` Green | 카테고리 칩, 차트, 진행바 |
| Mind | `#2196F3` Blue | 카테고리 칩, 차트, 진행바 |
| Family | `#FF9800` Orange | 카테고리 칩, 차트, 진행바 |
| Language | `#9C27B0` Purple | 카테고리 칩, 차트, 진행바 |
| Finance | `#F44336` Red | 카테고리 칩, 차트, 진행바 |
| Technology | `#00BCD4` Cyan | 카테고리 칩, 차트, 진행바 |

- 배경: `#1A1A2E` (Dark Navy)
- 카드: `#1E2A4A` (Card Background)
- 강조(Accent): `#E94560` (Red-Pink)
- 폰트: 기본 흰색 계열, 보조 텍스트는 회색

### 데이터 흐름

```
User Input → Composable → ViewModel → Repository → Room DB
                                   ↕
                          StateFlow 기반 자동 갱신
                                   ↕
                        통계/목표 화면에 실시간 반영
```

- 날짜 변경, 항목 추가/삭제, 목표 설정 모두 즉시 DB 저장 후 UI 자동 갱신
- 월 변경 시 해당 월 범위(start ~ end)로 쿼리하여 통계 재계산
- 목표 진행률은 goals + monthlyStats 두 Flow를 combine하여 계산

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
