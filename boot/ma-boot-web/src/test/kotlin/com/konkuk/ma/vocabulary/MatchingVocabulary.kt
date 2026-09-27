package com.konkuk.ma.vocabulary

import com.konkuk.ma.extension.BOOLEAN
import com.konkuk.ma.extension.NUMBER
import com.konkuk.ma.extension.STRING
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseType

// --- 매칭 관련 필드 ---

fun targetInfoId(fieldName: String = "targetInfoId") =
    fieldName responseType STRING means "찾는 사람 정보 ID (인코딩)" example "abc123"

fun registerEmail(fieldName: String = "registerEmail") =
    fieldName responseType STRING means "등록자 email" example "test@example.com"

fun targetName(fieldName: String = "name") =
    fieldName responseType STRING means "찾는 사람의 이름" example "김만남"

fun middleNumber(fieldName: String = "middleNumber") =
    fieldName responseType STRING means "전화번호 중간자리" example "1234"

fun lastNumber(fieldName: String = "lastNumber") =
    fieldName responseType STRING means "전화번호 뒷자리" example "5678"

fun year(fieldName: String = "year") =
    fieldName responseType NUMBER means "생년" example "1995"

fun month(fieldName: String = "month") =
    fieldName responseType NUMBER means "생월" example "5"

fun day(fieldName: String = "day") =
    fieldName responseType NUMBER means "생일" example "15"

fun targetGender(fieldName: String = "targetGender") =
    fieldName responseType STRING means "찾는 사람 성별" example "FEMALE"

fun targetRegion(fieldName: String = "region") =
    fieldName responseType STRING means "지역" example "SEOUL"

// --- 매칭 결과 고유 필드 ---
// 회원 프로필 관련 필드(이름, 닉네임, 프로필 이미지, 탈퇴 여부, 회원 ID)는 MemberVocabulary의 generic 함수를 path와 함께 호출하여 재사용한다

fun matchingResultId(fieldName: String = "matchingResults[].matchingResultId") =
    fieldName responseType STRING means "매칭 결과 ID (인코딩)" example "abc123"

fun remainingDays(fieldName: String = "matchingResults[].remainingDays") =
    fieldName responseType NUMBER means "매칭 결과 노출 잔여일" example "25"

fun matchRate(fieldName: String = "matchingResults[].matchRate") =
    fieldName responseType NUMBER means "매칭률 (%)" example "75"

fun claimed(fieldName: String = "matchingResults[].claimed") =
    fieldName responseType BOOLEAN means "claim 진행 여부 (거절된 경우 포함, NONE이 아니면 true)" example "false"

// --- 매칭 결과 상세 고유 필드 ---

fun detailMatchingResultId(fieldName: String = "matchingResultId") =
    fieldName responseType STRING means "매칭 결과 ID (인코딩)" example "abc123"

fun detailRemainingDays(fieldName: String = "remainingDays") =
    fieldName responseType NUMBER means "매칭 결과 노출 잔여일" example "25"

fun detailMatchRate(fieldName: String = "matchRate") =
    fieldName responseType NUMBER means "매칭률 (%)" example "75"

fun middleNumberMatched(fieldName: String = "middleNumberMatched") =
    fieldName responseType BOOLEAN means "전화번호 중간자리 일치 여부" example "true"

fun lastNumberMatched(fieldName: String = "lastNumberMatched") =
    fieldName responseType BOOLEAN means "전화번호 뒷자리 일치 여부" example "true"

fun yearMatched(fieldName: String = "yearMatched") =
    fieldName responseType BOOLEAN means "생년 일치 여부" example "true"

fun monthMatched(fieldName: String = "monthMatched") =
    fieldName responseType BOOLEAN means "생월 일치 여부" example "false"

fun dayMatched(fieldName: String = "dayMatched") =
    fieldName responseType BOOLEAN means "생일 일치 여부" example "false"

fun regionMatched(fieldName: String = "regionMatched") =
    fieldName responseType BOOLEAN means "지역 일치 여부" example "true"

fun resultExcludedParam(fieldName: String = "excluded") =
    fieldName requestParam "제외된 매칭 결과 조회 여부 (기본값: false)" isOptional true

fun matchingResultIdPath(fieldName: String = "matchingResultId") =
    fieldName requestParam "매칭 결과 ID (인코딩)"

// --- Claimer(나를 X로 신청한 요청자) 고유 필드 ---

fun hasXroom(fieldName: String = "claimers[].hasXroom") =
    fieldName responseType BOOLEAN means "해당 TargetInfo로 X룸을 만들었는지 여부" example "true"
