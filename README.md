# MOEIM — 소모임 커뮤니티 웹 서비스
>**2025-02 오픈소스 개발프로젝트 3조**<br/>
**개발기간: 2025.11.06 ~ 2025.12.02**


## 팀원 소개


| 학번       | 이름   |
|-----------|--------|
| 2022041045  | 서민석 |
| 2022041062  | 최상영 |
| 2022041069  | 이인수 |
| 2024042025  | Ali Zarq |



## 프로젝트 소개

MOEIM은 사용자들이 자신의 관심사와 취향을 중심으로 자연스럽게 모임을 만들고 참여하도록 돕는 
**소모임 커뮤니티 웹 서비스**입니다.  
운동, 공부, 취미, 식사 등 다양한 카테고리의 모임을 만들고 관리할 수 있으며,  
모임 생성부터 일정 확인까지 하나의 플랫폼에서 제공합니다.



## 개발 환경

- **프레임워크**: SpringBoot
- **데이터베이스**: h2
- **개발언어**: java
- **IDE**: IntelliJ
- **UI**: html, css, js
- **버전 관리**: Git + GitHub (평가를 위한 내부적인 Private 레포지토리)


## 주요 기능

1. **로그인/로그아웃** : 메일, 비밀번호, 닉네임, 소개, 관심사 등을 입력하여 계정을 만들 수 있습니다. 비밀번호는 Bcrypt를 통해 암호화 후 관리됩니다.

<br>

2. **메인 페이지** : 메인 페이지는 모든 기능들을 빠르게 접근할 수 있도록 구성되었습니다. 달려을 통해 소모임의 일정들을 한 눈에 볼 수 있으며, 설정된 관심사를 통해 아직 가입하지 않은 소모임들을 추천합니다. 

<br>


3. **유저 찾기** : 계정을 잃어버렸을 경우 닉네임으로 메일을 찾을 수 있으며, 닉네임, 메일을 이용하여 비밀번호 변경도 가능합니다.
   
<br>


4. **소모임** : 카테고리를 통해 원하는 관심를 선택한 후 만들고자 하는 모임을 만들 수 있습니다. 만들어진 소모임은 검색을 통해 찾을 수 있으며 소모임 관련 설정을 통해 정보 수정, 인원 관리, 소모임 삭제 등의 기능읗 사용할 수 있습니다.
   
<br>


5. **마이페이지**: 내 정보를 확인 및 수정 할 수 있습니다. 가입된 소모임, 일정, 내 활동, 평점 등을 확인 가능합니다.
  
<br>


6. **일정 관리** : 소모임의 일정을 만들거나 수정할 수 있습니다. 각 소모임 별로 일정을 볼 수 있으며, 전체 일정도 확인 가능합니다. 일정 내보내기 기능을 통해 다른 달력 앱으로 가져올 수 있습니가.

<br>


7. **채팅** : 각 소모임에게는 그룹 채팅 기능이 주어지고, 유저 별로 개인 채팅 기능도 사용할 수 있습니다. 읽음 기능을 통해 상대방의 채팅 확인 여부도 알 수 있습니다.

<br>


8. **게시판** : 카테고리별 게시글을 올릴 수 있는 게시판 기능을 이용할 수 있습니다. 댓글, 추천 및 비추천 등의 기능을 사용할 수 있습니다.

<br>

9. **신뢰도** : 소모임, 또는 개인을 상대로 평가를 할 수 있습니다. 리뷰 글 작성도 가능합니다.

<br>


10. **오류처리** : 화이트라벨 대신 자체 오류페이지 화면을 출력합니다

<br>


11. **네비게이션 바** : 로그인, FaQ, 마이페이지, 커뮤니티(게시판), 소모임 등의 기능을 빠르게 이동하여 사용할 수 있습니다.


<br>


## 디렉토리 구조

```
Moeim
├─.gradle
│  ├─8.14
│  │  ├─checksums
│  │  ├─executionHistory
│  │  ├─expanded
│  │  ├─fileChanges
│  │  ├─fileHashes
│  │  └─vcsMetadata
│  ├─8.14.3
│  │  ├─checksums
│  │  ├─executionHistory
│  │  ├─expanded
│  │  ├─fileChanges
│  │  ├─fileHashes
│  │  └─vcsMetadata
│  ├─buildOutputCleanup
│  └─vcs-1
├─.idea
│  └─modules
├─build
│  ├─classes
│  │  └─java
│  │      └─main
│  │          └─com
│  │              └─moeim
│  │                  ├─category
│  │                  ├─chat
│  │                  ├─global
│  │                  │  └─enums
│  │                  ├─group
│  │                  ├─post
│  │                  ├─review
│  │                  │  ├─groupreview
│  │                  │  └─userreview
│  │                  ├─schedule
│  │                  │  └─dto
│  │                  └─user
│  ├─generated
│  │  └─sources
│  │      ├─annotationProcessor
│  │      │  └─java
│  │      │      └─main
│  │      └─headers
│  │          └─java
│  │              └─main
│  ├─reports
│  │  └─problems
│  ├─resources
│  │  └─main
│  │      ├─static
│  │      │  ├─css
│  │      │  │  ├─Category
│  │      │  │  ├─chat
│  │      │  │  ├─faq
│  │      │  │  ├─group
│  │      │  │  ├─review
│  │      │  │  ├─schedule
│  │      │  │  └─user
│  │      │  ├─images
│  │      │  └─js
│  │      └─templates
│  │          ├─Category
│  │          ├─chat
│  │          ├─error
│  │          ├─faq
│  │          ├─group
│  │          ├─review
│  │          ├─schedule
│  │          └─user
│  └─tmp
│      └─compileJava
│          └─compileTransaction
│              ├─backup-dir
│              └─stash-dir
├─data
├─gradle
│  └─wrapper
└─src
    ├─main
    │  ├─java
    │  │  └─com
    │  │      └─moeim
    │  │          ├─category
    │  │          ├─chat
    │  │          ├─global
    │  │          │  └─enums
    │  │          ├─group
    │  │          ├─post
    │  │          ├─review
    │  │          │  ├─groupreview
    │  │          │  └─userreview
    │  │          ├─schedule
    │  │          └─user
    │  └─resources
    │      ├─static
    │      │  ├─css
    │      │  │  ├─Category
    │      │  │  ├─chat
    │      │  │  ├─faq
    │      │  │  ├─group
    │      │  │  ├─review
    │      │  │  ├─schedule
    │      │  │  └─user
    │      │  ├─images
    │      │  └─js
    │      └─templates
    │          ├─Category
    │          ├─chat
    │          ├─error
    │          ├─faq
    │          ├─group
    │          ├─review
    │          ├─schedule
    │          └─user
    └─test
        └─java
            └─com
                └─moeim
```

<br>




