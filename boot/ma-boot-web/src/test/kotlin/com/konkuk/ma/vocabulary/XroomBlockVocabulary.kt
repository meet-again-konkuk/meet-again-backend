package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.ARRAY
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.requestPart
import com.konkuk.ma.extension.responseType

// --- X룸 블록 공통 필드 ---

fun blockType(fieldName: String = "type") =
    fieldName responseType STRING means "블록 타입 (PHOTO, SHORT_TEXT, LONG_TEXT, MUSIC, DDAY, VIDEO)" example "PHOTO"

fun blockItem(fieldName: String = "item") =
    fieldName responseType STRING means "블록 표현 아이템 (POLAROID_FRAME, COLLAGE_3, GALLERY_WALL, PLAIN_CARD, LETTER_PAPER, LONG_LETTER, MUSIC_CARD, DDAY_BADGE, VIDEO_FRAME)" example "POLAROID_FRAME"

fun blockPositionX(fieldName: String = "positionX") =
    fieldName responseType NUMBER means "X 좌표 (1~255)" example "120"

fun blockPositionY(fieldName: String = "positionY") =
    fieldName responseType NUMBER means "Y 좌표 (1~255)" example "80"

fun blockRotation(fieldName: String = "rotation") =
    fieldName responseType NUMBER means "회전 각도 (degree)" example "0"

// --- 타입별 필드 ---

fun blockPhotoDate(fieldName: String = "photoDate") =
    fieldName responseType STRING means "사진 날짜" example "2024-03-21"

fun blockContent(fieldName: String = "content") =
    fieldName responseType STRING means "텍스트 내용" example "안녕하세요"

fun blockMusicUrl(fieldName: String = "musicUrl") =
    fieldName responseType STRING means "음원 URL" example "https://example.com/song.mp3"

fun blockMusicTitle(fieldName: String = "title") =
    fieldName responseType STRING means "곡 제목" example "Spring Day"

fun blockMusicArtist(fieldName: String = "artist") =
    fieldName responseType STRING means "아티스트" example "BTS"

fun blockAnniversaryDate(fieldName: String = "anniversaryDate") =
    fieldName responseType STRING means "기념일" example "2023-05-01"

fun blockLabel(fieldName: String = "label") =
    fieldName responseType STRING means "기념일 라벨" example "처음 만난 날"

fun blockDescription(fieldName: String = "description") =
    fieldName responseType STRING means "영상 설명" example "여름 휴가 영상"

// --- 응답 필드 ---

fun blockId(fieldName: String = "blockId") =
    fieldName responseType STRING means "X룸 블록 ID (인코딩)" example "abc123"

fun photoIds(fieldName: String = "photoIds") =
    fieldName responseType ARRAY means "X룸 블록 사진 ID 목록 (인코딩)" example "[\"abc123\", \"def456\"]"

fun videoIds(fieldName: String = "videoIds") =
    fieldName responseType ARRAY means "X룸 블록 영상 ID 목록 (인코딩)" example "[\"abc123\"]"

fun photoId(fieldName: String = "photoId") =
    fieldName responseType STRING means "X룸 블록 사진 ID (인코딩)" example "abc123"

fun videoId(fieldName: String = "videoId") =
    fieldName responseType STRING means "X룸 블록 영상 ID (인코딩)" example "abc123"

// --- Path Variable ---

fun blockIdPath(fieldName: String = "blockId") =
    fieldName requestParam "X룸 블록 ID (인코딩)"

fun photoIdPath(fieldName: String = "photoId") =
    fieldName requestParam "X룸 블록 사진 ID (인코딩)"

fun videoIdPath(fieldName: String = "videoId") =
    fieldName requestParam "X룸 블록 영상 ID (인코딩)"

// --- Multipart Part ---

fun photosPart(fieldName: String = "photos") =
    fieldName requestPart "업로드할 사진 파일 목록 (item이 정의한 maxPhotoCount와 정확히 일치해야 함)"

fun videosPart(fieldName: String = "videos") =
    fieldName requestPart "업로드할 영상 파일 목록 (item이 정의한 maxVideoCount와 정확히 일치해야 함)"

fun photoPart(fieldName: String = "photo") =
    fieldName requestPart "교체할 사진 파일 (1장)"

fun videoPart(fieldName: String = "video") =
    fieldName requestPart "교체할 영상 파일 (1개)"
